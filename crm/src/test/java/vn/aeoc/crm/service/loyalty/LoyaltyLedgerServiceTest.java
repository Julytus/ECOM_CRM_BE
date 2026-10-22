package vn.aeoc.crm.service.loyalty;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.repository.loyalty.LoyaltyAccountRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyConfigRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyTierRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyTransactionRepository;
import vn.aeoc.entity.data.mysql.sale.LoyaltyAccount;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTier;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTransaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.inOrder;

class LoyaltyLedgerServiceTest {
    private LoyaltyAccountRepository accounts;
    private LoyaltyTransactionRepository transactions;
    private LoyaltyTierRepository tiers;
    private LoyaltyConfigRepository config;
    private LoyaltyLedgerService service;

    @BeforeEach
    void setUp() {
        accounts = mock(LoyaltyAccountRepository.class);
        transactions = mock(LoyaltyTransactionRepository.class);
        tiers = mock(LoyaltyTierRepository.class);
        config = mock(LoyaltyConfigRepository.class);
        service = new LoyaltyLedgerService(accounts, transactions, tiers, config);
        when(config.getEarnRate()).thenReturn(10);
        when(accounts.getOrCreate(1)).thenReturn(account(12, 0, 0, 1));
        when(accounts.lockByCustomerId(1)).thenAnswer(invocation -> account(12, 0, 0, 1));
        when(transactions.insert(any())).thenReturn(true);
        when(transactions.findByRef(anyString(), anyString(), anyString())).thenReturn(null);
        when(tiers.findHighestTier(anyLong())).thenAnswer(invocation -> tier(1, 0, "1.00"));
    }

    @Test
    void earnUsesCurrentTierMultiplierAndRoundsDown() {
        when(accounts.lockByCustomerId(1)).thenReturn(account(12, 0, 0, 2));
        when(tiers.listActiveOrderedByThreshold()).thenReturn(List.of(tier(2, 0, "1.75")));

        var result = service.earnForOrder(1, 101, 100.01);

        assertEquals(17L, result.points());
        assertEquals(10L, result.rankDelta());
        verify(transactions).insert(argThat(tx -> tx.getPoints() == 17L && tx.getRankPointsDelta() == 10L));
        verify(accounts).applyDelta(12, 17L, 10L, 1);
    }

    @Test
    void earnUsesRoundedAmountForRateBoundariesAndIsIdempotent() {
        when(config.getEarnRate()).thenReturn(10_000);
        service.earnForOrder(1, 101, 9.999);
        service.earnForOrder(1, 103, 10.000);
        verify(transactions, never()).insert(any());

        reset(transactions);
        when(transactions.findByRef("EARN", "ORDER", "102")).thenReturn(null);
        when(transactions.insert(any())).thenReturn(true);
        service.earnForOrder(1, 102, 10_001);
        verify(transactions).insert(argThat(tx -> tx.getPoints() == 1L));

        reset(transactions);
        when(transactions.findByRef("EARN", "ORDER", "102")).thenReturn(transaction("EARN", 1, 1));
        service.earnForOrder(1, 102, 10_001);
        verify(transactions, never()).insert(any());
        verify(accounts, times(1)).applyDelta(12, 1L, 1L, 1);
    }

    @Test
    void spendLocksAccountBeforeInsertingLedgerEntry() {
        when(accounts.lockByCustomerId(1)).thenReturn(account(12, 20, 0, 1));

        assertTrue(service.spend(1, 5, "CUSTOMER_VOUCHER", "77"));

        var order = inOrder(accounts, transactions);
        order.verify(accounts).lockByCustomerId(1);
        order.verify(transactions).findByRef("REDEEM", "CUSTOMER_VOUCHER", "77");
        order.verify(transactions).insert(argThat(tx -> tx.getPoints() == -5L));
        order.verify(accounts).applyDelta(12, -5L, 0L, 1);
    }

    @Test
    void reverseCopiesEarnDeltasAllowsNegativeBalanceAndRecalculatesTier() {
        when(transactions.findByRef("EARN", "ORDER", "101")).thenReturn(transaction("EARN", 15, 8));
        when(accounts.lockByCustomerId(1)).thenReturn(account(12, 3, 2, 2));
        when(transactions.findByRef("REVERSE_EARN", "ORDER", "101")).thenReturn(null);
        when(tiers.findHighestTier(-6)).thenReturn(tier(1, 0, "1.00"));

        assertEquals(LoyaltyLedgerService.ReverseResult.REVERSED, service.reverseOrder(101));

        verify(transactions).insert(argThat(tx -> tx.getPoints() == -15L
                && tx.getRankPointsDelta() == -8L
                && tx.getNote().contains("#101")));
        verify(accounts).applyDelta(12, -15L, -8L, 1);
    }

    @Test
    void reverseWithoutEarnReturnsNothingToReverse() {
        when(transactions.findByRef("EARN", "ORDER", "999")).thenReturn(null);
        assertEquals(LoyaltyLedgerService.ReverseResult.NOTHING_TO_REVERSE, service.reverseOrder(999));
        verify(accounts, never()).applyDelta(anyInt(), anyLong(), anyLong(), any());
    }

    @Test
    void spendRejectsInsufficientAndNegativeBalancesAndLocksBeforeWriting() {
        when(accounts.lockByCustomerId(1)).thenReturn(account(12, 4, 0, 1));
        assertThrows(AppException.class, () -> service.spend(1, 5, "CUSTOMER_VOUCHER", "5"));
        when(accounts.lockByCustomerId(1)).thenReturn(account(12, -1, 0, 1));
        assertThrows(AppException.class, () -> service.spend(1, 1, "CUSTOMER_VOUCHER", "6"));
        verify(transactions, never()).insert(any());
    }

    @Test
    void adjustRequiresReason() {
        assertThrows(AppException.class, () -> service.adjust(1, 5, 2, "  ", 8));
        verify(accounts, never()).lockByCustomerId(anyInt());
    }

    @Test
    void earnSkipsInvalidCustomerAndRejectsBadEarnRate() {
        assertEquals(0L, service.earnForOrder(null, 1, 100).points());
        verify(accounts, never()).getOrCreate(anyInt());
        when(config.getEarnRate()).thenReturn(0);
        assertThrows(AppException.class, () -> service.earnForOrder(1, 2, 100));
    }

    @Test
    void accountLockQueryContainsForUpdate() {
        AtomicReference<String> capturedSql = new AtomicReference<>();
        DSLContext rowDsl = org.jooq.impl.DSL.using(SQLDialect.MYSQL);
        var rows = rowDsl.newResult(vn.entity.backend.tables.LoyaltyAccount.LOYALTY_ACCOUNT.fields());
        var row = rowDsl.newRecord(vn.entity.backend.tables.LoyaltyAccount.LOYALTY_ACCOUNT);
        row.setId(12);
        row.setCustomerId(1);
        row.setBalance(0L);
        row.setRankPoints(0L);
        row.setVersion(0);
        rows.add(row);
        DSLContext dsl = org.jooq.impl.DSL.using(new MockConnection(context -> {
            capturedSql.set(context.sql());
            return new MockResult[]{new MockResult(1, rows)};
        }), SQLDialect.MYSQL);

        new LoyaltyAccountRepository(dsl).lockByCustomerId(1);

        assertTrue(capturedSql.get().toLowerCase().contains("for update"));
    }

    @Test
    void accountBalancesEqualLedgerSumsAfterMixedOperations() {
        LoyaltyAccountRepository accountRepo = mock(LoyaltyAccountRepository.class);
        LoyaltyTransactionRepository transactionRepo = mock(LoyaltyTransactionRepository.class);
        LoyaltyTierRepository tierRepo = mock(LoyaltyTierRepository.class);
        LoyaltyConfigRepository configRepo = mock(LoyaltyConfigRepository.class);
        LoyaltyLedgerService ledger = new LoyaltyLedgerService(accountRepo, transactionRepo, tierRepo, configRepo);
        List<LoyaltyTransaction> rows = new ArrayList<>();
        LoyaltyAccount state = account(12, 0, 0, 1);

        when(configRepo.getEarnRate()).thenReturn(10);
        when(accountRepo.getOrCreate(1)).thenReturn(state);
        when(accountRepo.lockByCustomerId(1)).thenAnswer(call -> state);
        when(tierRepo.listActiveOrderedByThreshold()).thenReturn(List.of(tier(1, 0, "1.00")));
        when(tierRepo.findHighestTier(anyLong())).thenReturn(tier(1, 0, "1.00"));
        when(transactionRepo.findByRef(anyString(), anyString(), anyString())).thenAnswer(call -> rows.stream()
                .filter(row -> call.getArgument(0).equals(row.getType()))
                .filter(row -> call.getArgument(1).equals(row.getRefType()))
                .filter(row -> call.getArgument(2).equals(row.getRefId()))
                .findFirst().orElse(null));
        when(transactionRepo.insert(any())).thenAnswer(call -> { rows.add(call.getArgument(0)); return true; });
        when(accountRepo.applyDelta(anyInt(), anyLong(), anyLong(), any())).thenAnswer(call -> {
            state.setBalance(state.getBalance() + (Long) call.getArgument(1));
            state.setRankPoints(state.getRankPoints() + (Long) call.getArgument(2));
            return 1;
        });

        ledger.earnForOrder(1, 101, 100);
        ledger.spend(1, 3, "CUSTOMER_VOUCHER", "77");
        ledger.adjust(1, 2, -4, "manual correction", 8);
        ledger.reverseOrder(101);

        long pointsSum = rows.stream().mapToLong(LoyaltyTransaction::getPoints).sum();
        long rankSum = rows.stream().mapToLong(LoyaltyTransaction::getRankPointsDelta).sum();
        assertEquals(pointsSum, state.getBalance());
        assertEquals(rankSum, state.getRankPoints());
    }

    private LoyaltyAccount account(int id, long balance, long rank, int tierId) {
        LoyaltyAccount a = new LoyaltyAccount();
        a.setId(id); a.setCustomerId(1); a.setBalance(balance); a.setRankPoints(rank); a.setTierId(tierId);
        return a;
    }

    private LoyaltyTier tier(int id, long minPoints, String multiplier) {
        LoyaltyTier tier = new LoyaltyTier();
        tier.setId(id); tier.setMinRankPoints(minPoints); tier.setEarnMultiplier(new BigDecimal(multiplier));
        return tier;
    }

    private LoyaltyTransaction transaction(String type, long points, long rankDelta) {
        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setCustomerId(1); tx.setType(type); tx.setPoints(points); tx.setRankPointsDelta(rankDelta);
        return tx;
    }
}
