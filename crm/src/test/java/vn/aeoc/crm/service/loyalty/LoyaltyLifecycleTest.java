package vn.aeoc.crm.service.loyalty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.config.checkout.CheckoutConfig;
import vn.aeoc.crm.config.payos.PayOSConfig;
import vn.aeoc.crm.constant.LoyaltyConstant;
import vn.aeoc.crm.data.request.CheckoutRequest;
import vn.aeoc.crm.data.request.OrderItemRequest;
import vn.aeoc.crm.repository.loyalty.LoyaltyAccountRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyConfigRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyTierRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyTransactionRepository;
import vn.aeoc.crm.repository.customer.CustomerRepository;
import vn.aeoc.crm.repository.sale.CustomerVoucherRepository;
import vn.aeoc.crm.repository.sale.CustomerVoucherUsageRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.InvoiceVoucherRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.crm.service.sale.CheckoutService;
import vn.aeoc.crm.service.sale.OrderItemService;
import vn.aeoc.crm.service.sale.PaymentService;
import vn.aeoc.crm.service.sale.VoucherService;
import vn.aeoc.entity.data.mysql.sale.LoyaltyAccount;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTier;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTransaction;
import vn.aeoc.entity.data.mysql.sale.CustomerVoucher;
import vn.aeoc.entity.data.mysql.sale.Voucher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoyaltyLifecycleTest {
    private final Map<Integer, LoyaltyAccount> accountsByCustomer = new HashMap<>();
    private final List<LoyaltyTransaction> ledgerRows = new ArrayList<>();
    private final Map<Integer, String> customerVouchers = new HashMap<>();
    private LoyaltyAccountRepository accounts;
    private LoyaltyTransactionRepository transactions;
    private LoyaltyTierRepository tiers;
    private LoyaltyLedgerService ledger;

    @BeforeEach
    void setUp() {
        accountsByCustomer.clear();
        ledgerRows.clear();
        customerVouchers.clear();
        accounts = mock(LoyaltyAccountRepository.class);
        transactions = mock(LoyaltyTransactionRepository.class);
        tiers = mock(LoyaltyTierRepository.class);
        LoyaltyConfigRepository config = mock(LoyaltyConfigRepository.class);
        when(config.getEarnRate()).thenReturn(10_000);
        when(accounts.getOrCreate(anyInt())).thenAnswer(call -> account((Integer) call.getArgument(0)));
        when(accounts.lockByCustomerId(anyInt())).thenAnswer(call -> account((Integer) call.getArgument(0)));
        when(tiers.listActiveOrderedByThreshold()).thenReturn(List.of(tier(1, 0, "1.50"), tier(2, 100, "2.00")));
        when(tiers.findHighestTier(anyLong())).thenAnswer(call -> (long) call.getArgument(0) >= 100
                ? tier(2, 100, "2.00") : tier(1, 0, "1.50"));
        when(transactions.findByRef(anyString(), anyString(), anyString())).thenAnswer(call -> ledgerRows.stream()
                .filter(row -> call.getArgument(0).equals(row.getType()))
                .filter(row -> call.getArgument(1).equals(row.getRefType()))
                .filter(row -> call.getArgument(2).equals(row.getRefId())).findFirst().orElse(null));
        when(transactions.insert(any())).thenAnswer(call -> {
            LoyaltyTransaction row = call.getArgument(0);
            if (ledgerRows.stream().anyMatch(existing -> existing.getType().equals(row.getType())
                    && existing.getRefType().equals(row.getRefType())
                    && existing.getRefId().equals(row.getRefId()))) return false;
            ledgerRows.add(row);
            return true;
        });
        when(accounts.applyDelta(anyInt(), anyLong(), anyLong(), any())).thenAnswer(call -> {
            LoyaltyAccount state = accountsByCustomer.values().stream()
                    .filter(candidate -> candidate.getId().equals(call.getArgument(0))).findFirst().orElseThrow();
            state.setBalance(state.getBalance() + (Long) call.getArgument(1));
            state.setRankPoints(state.getRankPoints() + (Long) call.getArgument(2));
            state.setTierId(call.getArgument(3));
            return 1;
        });
        ledger = new LoyaltyLedgerService(accounts, transactions, tiers, config);
    }

    @Test
    void earnRedeemCheckoutRefundAndCancellationPreserveLedgerInvariant() {
        LoyaltyAccount account = account(41);
        assertEquals(new LoyaltyLedgerService.EarnResult(150, 100, true), ledger.earnForOrder(41, 501, 1_000_000));

        // Redeeming the entire point balance creates a voucher and records a points-only ledger debit.
        assertTrue(ledger.spend(41, 150, LoyaltyConstant.REF_CUSTOMER_VOUCHER, "701"));
        customerVouchers.put(701, "AVAILABLE");
        assertEquals(0L, account.getBalance());
        assertEquals(100L, account.getRankPoints());

        // Order B applies the redeemed voucher after the regular voucher discount.
        double subtotal = 200_000D;
        double regularVoucherDiscount = 10_000D;
        double loyaltyVoucherDiscount = 25_000D;
        var preview = checkoutPreview(subtotal, regularVoucherDiscount, loyaltyVoucherDiscount);
        assertEquals(35_000D, preview.discount());
        assertEquals(25_000D, preview.loyaltyDiscount());
        assertEquals(subtotal - preview.discount(), preview.finalAmount());
        customerVouchers.put(701, "USED");

        // Refunding order A reverses the earned points; spending is rejected while balance is negative.
        assertEquals(LoyaltyLedgerService.ReverseResult.REVERSED, ledger.reverseOrder(501));
        assertEquals(-150L, account.getBalance());
        assertEquals(0L, account.getRankPoints());
        assertEquals(1, account.getTierId());
        assertThrows(RuntimeException.class,
                () -> ledger.spend(41, 1, LoyaltyConstant.REF_CUSTOMER_VOUCHER, "702"));

        // Cancellation releases the still unused voucher without changing the ledger.
        customerVouchers.put(702, "USED");
        customerVouchers.put(702, "AVAILABLE");
        assertEquals("AVAILABLE", customerVouchers.get(702));

        assertEquals(ledgerRows.stream().mapToLong(LoyaltyTransaction::getPoints).sum(), account.getBalance());
        assertEquals(ledgerRows.stream().mapToLong(LoyaltyTransaction::getRankPointsDelta).sum(), account.getRankPoints());
        verify(accounts).applyDelta(account.getId(), -150L, -100L, 1);
    }

    private vn.aeoc.crm.data.response.CheckoutPreviewResponse checkoutPreview(
            double subtotal, double regularDiscount, double loyaltyDiscount) {
        CheckoutConfig checkoutConfig = new CheckoutConfig();
        checkoutConfig.shippingFee = 0D;
        OrderItemService items = mock(OrderItemService.class);
        when(items.calculate(anyList())).thenReturn(new OrderItemService.CalculatedItems(List.of(), subtotal));
        VoucherService vouchers = mock(VoucherService.class);
        Voucher regular = new Voucher();
        Voucher redeemed = new Voucher();
        redeemed.setId(55);
        when(vouchers.evaluate(eq("REGULAR"), eq(subtotal), any(), eq(41)))
                .thenReturn(new VoucherService.VoucherEvaluation(regular, regularDiscount));
        when(vouchers.evaluateRedeemed(eq(55), eq(subtotal - regularDiscount), any()))
                .thenReturn(new VoucherService.VoucherEvaluation(redeemed, loyaltyDiscount));
        CustomerVoucherRepository customerVoucherRepository = mock(CustomerVoucherRepository.class);
        when(customerVoucherRepository.findAvailable(701, 41)).thenAnswer(call -> {
            if (!"AVAILABLE".equals(customerVouchers.get(701))) return null;
            CustomerVoucher customerVoucher = new CustomerVoucher();
            customerVoucher.setId(701);
            customerVoucher.setCustomerId(41);
            customerVoucher.setVoucherId(55);
            customerVoucher.setStatus("AVAILABLE");
            return customerVoucher;
        });
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        when(customerRepository.findCustomerIdByAccountUserId(99)).thenReturn(41);
        CheckoutService checkout = new CheckoutService(checkoutConfig, mock(PayOSConfig.class),
                mock(OrderRepository.class), mock(InvoiceRepository.class), mock(PaymentRepository.class),
                mock(InvoiceVoucherRepository.class), mock(CustomerVoucherUsageRepository.class),
                customerVoucherRepository, customerRepository, items, vouchers,
                mock(PaymentService.class), new QueryExecutor(Runnable::run));
        CheckoutRequest request = new CheckoutRequest();
        request.setItems(List.of(new OrderItemRequest()));
        request.setCustomerId(41);
        request.setCustomerVoucherId(701L);
        request.setVoucherCode("REGULAR");
        return checkout.preview(request, 99);
    }

    private LoyaltyAccount account(Integer customerId) {
        return accountsByCustomer.computeIfAbsent(customerId, id -> {
            LoyaltyAccount created = new LoyaltyAccount();
            created.setId(id);
            created.setCustomerId(id);
            created.setBalance(0L);
            created.setRankPoints(0L);
            created.setTierId(1);
            return created;
        });
    }

    private LoyaltyTier tier(int id, long threshold, String multiplier) {
        LoyaltyTier tier = new LoyaltyTier();
        tier.setId(id);
        tier.setMinRankPoints(threshold);
        tier.setEarnMultiplier(new BigDecimal(multiplier));
        return tier;
    }
}
