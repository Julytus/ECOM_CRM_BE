package vn.aeoc.crm.service.loyalty;

import org.jooq.DSLContext;
import org.jooq.Select;
import org.junit.jupiter.api.Test;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.data.request.LoyaltyTierRequest;
import vn.aeoc.crm.config.security.principal.impl.SimplePrincipal;
import vn.aeoc.crm.controller.loyalty.LoyaltyAdminController;
import vn.aeoc.crm.repository.loyalty.LoyaltyAccountRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyConfigRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyTierRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyTransactionRepository;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.entity.data.mysql.auth.User;
import vn.aeoc.entity.data.mysql.sale.LoyaltyAccount;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTransaction;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoyaltyAdminServiceTest {
    @Test
    void rejectsDuplicateTierThreshold() {
        DSLContext dsl = mock(DSLContext.class, RETURNS_DEEP_STUBS);
        when(dsl.fetchExists(any(Select.class))).thenReturn(true);
        LoyaltyAdminService service = new LoyaltyAdminService(dsl, mock(LoyaltyLedgerService.class));

        assertThrows(AppException.class, () -> service.saveTier(
                new LoyaltyTierRequest(null, "Silver", 100L, BigDecimal.ONE, 1)));
    }

    @Test
    void rejectsEarnRateAtOrBelowZero() {
        LoyaltyAdminService service = new LoyaltyAdminService(mock(DSLContext.class), mock(LoyaltyLedgerService.class));
        assertThrows(AppException.class, () -> service.updateConfig(0));
        assertThrows(AppException.class, () -> service.updateConfig(-1));
    }

    @Test
    void rejectsDeletingTierInUse() {
        DSLContext dsl = mock(DSLContext.class, RETURNS_DEEP_STUBS);
        when(dsl.fetchExists(any(Select.class))).thenReturn(true);
        LoyaltyAdminService service = new LoyaltyAdminService(dsl, mock(LoyaltyLedgerService.class));

        assertThrows(AppException.class, () -> service.deleteTier(3));
    }

    @Test
    void rejectsLinkingAccountAlreadyAttachedToAnotherCustomer() {
        DSLContext dsl = mock(DSLContext.class, RETURNS_DEEP_STUBS);
        when(dsl.fetchExists(any(Select.class))).thenReturn(true, true);
        LoyaltyAdminService service = new LoyaltyAdminService(dsl, mock(LoyaltyLedgerService.class));

        assertThrows(AppException.class, () -> service.linkAccount(12, 91));
    }

    @Test
    void adminAdjustDelegatesCreatorIdToLedger() {
        LoyaltyLedgerService ledger = mock(LoyaltyLedgerService.class);
        LoyaltyAdminService service = new LoyaltyAdminService(mock(DSLContext.class), ledger);

        service.adjust(12, -20, -5, "manual correction", 91);

        verify(ledger).adjust(12, -20, -5, "manual correction", 91);
    }

    @Test
    void allowsAdjustmentToCreateNegativeBalance() {
        LoyaltyAccountRepository accounts = mock(LoyaltyAccountRepository.class);
        LoyaltyTransactionRepository transactions = mock(LoyaltyTransactionRepository.class);
        LoyaltyTierRepository tiers = mock(LoyaltyTierRepository.class);
        LoyaltyAccount account = new LoyaltyAccount();
        account.setId(3);
        account.setCustomerId(12);
        account.setBalance(0L);
        account.setRankPoints(0L);
        when(accounts.getOrCreate(12)).thenReturn(account);
        when(accounts.lockByCustomerId(12)).thenReturn(account);
        when(transactions.insert(any(LoyaltyTransaction.class))).thenReturn(true);
        when(tiers.findHighestTier(-5)).thenReturn(null);
        LoyaltyLedgerService ledger = new LoyaltyLedgerService(accounts, transactions, tiers,
                mock(LoyaltyConfigRepository.class));

        assertTrue(ledger.adjust(12, -10, -5, "correction", 91));

        verify(accounts).applyDelta(3, -10, -5, null);
    }

    @Test
    void rejectsAdminRequestWithoutLoyaltyPermission() {
        UserService users = mock(UserService.class);
        when(users.checkPermission(20, "/loyalty/", "VIEW")).thenReturn(new User());
        LoyaltyAdminController controller = new LoyaltyAdminController(mock(LoyaltyAdminService.class), users);

        assertThrows(AppException.class,
                () -> controller.tiers(SimplePrincipal.builder().id(20).build()));
    }

    @Test
    void ledgerRejectsMissingAdjustmentReason() {
        LoyaltyLedgerService ledger = new LoyaltyLedgerService(mock(vn.aeoc.crm.repository.loyalty.LoyaltyAccountRepository.class),
                mock(vn.aeoc.crm.repository.loyalty.LoyaltyTransactionRepository.class),
                mock(vn.aeoc.crm.repository.loyalty.LoyaltyTierRepository.class),
                mock(vn.aeoc.crm.repository.loyalty.LoyaltyConfigRepository.class));

        assertThrows(AppException.class, () -> ledger.adjust(12, 20, 0, " ", 91));
    }
}
