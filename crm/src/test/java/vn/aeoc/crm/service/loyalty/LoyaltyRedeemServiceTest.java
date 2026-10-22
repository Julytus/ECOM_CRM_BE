package vn.aeoc.crm.service.loyalty;

import org.jooq.DSLContext;
import org.jooq.Condition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.repository.loyalty.LoyaltyAccountRepository;
import vn.aeoc.crm.repository.sale.VoucherRepository;
import vn.aeoc.entity.data.mysql.sale.LoyaltyAccount;
import vn.aeoc.entity.data.mysql.sale.Voucher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static vn.entity.backend.tables.Voucher.VOUCHER;

class LoyaltyRedeemServiceTest {
    @Test
    void stockIncrementSqlChecksRedeemedCountAgainstStock() {
        DSLContext dsl = mock(DSLContext.class, RETURNS_DEEP_STUBS);
        when(dsl.update(VOUCHER)
                .set(VOUCHER.REDEEMED_COUNT, VOUCHER.REDEEMED_COUNT.plus(1))
                .where(any(Condition.class)).execute()).thenReturn(1);
        VoucherRepository repository = new VoucherRepository(dsl);

        assertEquals(1, repository.increaseRedeemedIfInStock(12));

        ArgumentCaptor<Condition> condition = ArgumentCaptor.forClass(Condition.class);
        verify(dsl.update(VOUCHER).set(VOUCHER.REDEEMED_COUNT, VOUCHER.REDEEMED_COUNT.plus(1)), times(2))
                .where(condition.capture());
        String rendered = condition.getAllValues().getLast().toString().toLowerCase();
        assertTrue(rendered.matches("(?s).*redeemed_count.*<.*redeem_stock.*"), rendered);
    }

    @Test
    void rejectsWhenStockUpdateAffectsNoRowsWithoutSpendingPoints() {
        DSLContext dsl = mock(DSLContext.class, RETURNS_DEEP_STUBS);
        LoyaltyAccountRepository accounts = mock(LoyaltyAccountRepository.class);
        VoucherRepository vouchers = mock(VoucherRepository.class);
        LoyaltyLedgerService ledger = mock(LoyaltyLedgerService.class);
        LoyaltyAccount account = new LoyaltyAccount();
        account.setCustomerId(7);
        account.setBalance(500L);
        account.setRankPoints(0L);
        Voucher voucher = new Voucher();
        voucher.setId(12);
        voucher.setStatus(1);
        voucher.setPointsCost(100L);
        voucher.setRedeemStock(1);
        voucher.setRedeemedCount(1);
        voucher.setStartDate(java.time.LocalDateTime.now().minusDays(1));
        voucher.setEndDate(java.time.LocalDateTime.now().plusDays(1));
        when(accounts.getOrCreate(7)).thenReturn(account);
        when(accounts.lockByCustomerId(7)).thenReturn(account);
        when(dsl.selectFrom(VOUCHER).where(any(org.jooq.Condition.class)).forUpdate().fetchOneInto(Voucher.class))
                .thenReturn(voucher);
        when(vouchers.increaseRedeemedIfInStock(12)).thenReturn(0);

        LoyaltyRedeemService service = new LoyaltyRedeemService(dsl, accounts, vouchers, ledger);
        assertThrows(AppException.class, () -> service.redeem(7, 12));
        verify(vouchers).increaseRedeemedIfInStock(12);
        verifyNoInteractions(ledger);
    }

    @Test
    void rejectsInvalidCustomerBeforeAccessingPersistence() {
        DSLContext dsl = mock(DSLContext.class);
        LoyaltyAccountRepository accounts = mock(LoyaltyAccountRepository.class);
        VoucherRepository vouchers = mock(VoucherRepository.class);
        LoyaltyLedgerService ledger = mock(LoyaltyLedgerService.class);
        LoyaltyRedeemService service = new LoyaltyRedeemService(dsl, accounts, vouchers, ledger);
        assertThrows(AppException.class, () -> service.redeem(null, 12));
        verifyNoInteractions(accounts, vouchers, ledger);
    }
}
