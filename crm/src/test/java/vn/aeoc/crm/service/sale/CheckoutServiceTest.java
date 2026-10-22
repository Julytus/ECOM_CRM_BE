package vn.aeoc.crm.service.sale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.config.checkout.CheckoutConfig;
import vn.aeoc.crm.config.payos.PayOSConfig;
import vn.aeoc.crm.data.constant.PaymentMethod;
import vn.aeoc.crm.data.request.CheckoutRequest;
import vn.aeoc.crm.repository.sale.CustomerVoucherUsageRepository;
import vn.aeoc.crm.repository.sale.CustomerVoucherRepository;
import vn.aeoc.crm.repository.customer.CustomerRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.InvoiceVoucherRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Order;
import vn.aeoc.entity.data.mysql.sale.Payment;
import vn.aeoc.entity.data.mysql.sale.CustomerVoucher;
import vn.aeoc.entity.data.mysql.sale.Voucher;

import java.util.List;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.data.request.OrderItemRequest;

class CheckoutServiceTest {
    private OrderRepository orderRepository;
    private InvoiceRepository invoiceRepository;
    private PaymentRepository paymentRepository;
    private InvoiceVoucherRepository invoiceVoucherRepository;
    private CustomerVoucherUsageRepository customerVoucherUsageRepository;
    private vn.aeoc.crm.repository.sale.CustomerVoucherRepository customerVoucherRepository;
    private vn.aeoc.crm.repository.customer.CustomerRepository customerRepository;
    private OrderItemService orderItemService;
    private VoucherService voucherService;
    private PaymentService paymentService;
    private PayOSConfig payOSConfig;
    private CheckoutService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        invoiceVoucherRepository = mock(InvoiceVoucherRepository.class);
        customerVoucherUsageRepository = mock(CustomerVoucherUsageRepository.class);
        customerVoucherRepository = mock(vn.aeoc.crm.repository.sale.CustomerVoucherRepository.class);
        customerRepository = mock(vn.aeoc.crm.repository.customer.CustomerRepository.class);
        orderItemService = mock(OrderItemService.class);
        voucherService = mock(VoucherService.class);
        paymentService = mock(PaymentService.class);
        CheckoutConfig config = new CheckoutConfig();
        config.shippingFee = 30_000;
        config.defaultBranchId = 7;
        payOSConfig = new PayOSConfig();
        payOSConfig.baseUrl = "https://example.test";
        service = new CheckoutService(config, payOSConfig, orderRepository, invoiceRepository,
                paymentRepository, invoiceVoucherRepository, customerVoucherUsageRepository,
                customerVoucherRepository, customerRepository,
                orderItemService, voucherService, paymentService, new QueryExecutor(Runnable::run));
    }

    @Test
    void previewCalculatesSubtotalAndShippingWithoutVoucher() {
        CheckoutRequest request = request(PaymentMethod.COD);
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));

        var result = service.preview(request);

        assertEquals(100_000, result.subtotal());
        assertEquals(0, result.discount());
        assertEquals(130_000, result.finalAmount());
        assertNull(result.voucherCode());
        verifyNoInteractions(orderRepository, invoiceRepository, paymentRepository);
    }

    @Test
    void previewDefaultsMissingPaymentMethodToCod() {
        CheckoutRequest request = request(null);
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));

        var result = service.preview(request);

        assertEquals(130_000, result.finalAmount());
    }

    @Test
    void previewUsesVoucherEvaluationDiscount() {
        CheckoutRequest request = request(PaymentMethod.COD);
        request.setVoucherCode("SAVE");
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
        when(voucherService.evaluate("SAVE", 100_000, 7, null))
                .thenReturn(new VoucherService.VoucherEvaluation(null, 20_000));

        var result = service.preview(request);

        assertEquals(20_000, result.discount());
        assertEquals(110_000, result.finalAmount());
        assertEquals("SAVE", result.voucherCode());
    }

    @Test
    void codSubmitInsertsPendingPaymentWithoutPayosOrPerUserUsage() {
        CheckoutRequest request = request(PaymentMethod.COD);
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
        when(customerRepository.findCustomerIdByAccountUserId(9)).thenReturn(null);
        Order order = new Order();
        order.setId(5);
        order.setCode("ORD-5");
        when(orderRepository.insertCheckoutOrder(any(Order.class), anyString())).thenReturn(order);
        Invoice invoice = new Invoice();
        invoice.setId(6);
        invoice.setInvoiceCode("INV-6");
        when(invoiceRepository.insertFromOrder(5, 100_000, 0, 30_000, PaymentMethod.COD, null, 7, 9))
                .thenReturn(invoice);
        when(paymentRepository.insert(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submit(request, 9);

        assertEquals(130_000, response.finalAmount());
        assertNull(response.checkoutUrl());
        assertNull(response.qrCode());
        verify(paymentService, never()).requestPayosLink(any(Long.class), any(Double.class), anyString(), anyString(), anyString());
        verify(customerVoucherUsageRepository, never()).increaseUsage(anyInt(), anyInt());
        verify(paymentRepository).insert(argThat(payment -> payment.getOrderId().equals(5)
                && payment.getInvoiceId().equals(6) && "COD".equals(payment.getPaymentProvider())
                && "PENDING".equals(payment.getStatus())));
    }

    @Test
    void submitDefaultsUnknownPaymentMethodToCodAcrossCheckoutRecords() {
        CheckoutRequest request = request("unsupported");
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
        when(customerRepository.findCustomerIdByAccountUserId(9)).thenReturn(null);
        Order order = new Order();
        order.setId(5);
        order.setCode("ORD-5");
        when(orderRepository.insertCheckoutOrder(any(Order.class), anyString())).thenAnswer(invocation -> {
            assertEquals(PaymentMethod.COD, ((Order) invocation.getArgument(0)).getPaymentMethod());
            return order;
        });
        Invoice invoice = new Invoice();
        invoice.setId(6);
        invoice.setInvoiceCode("INV-6");
        when(invoiceRepository.insertFromOrder(5, 100_000, 0, 30_000, PaymentMethod.COD, null, 7, 9)).thenReturn(invoice);
        when(paymentRepository.insert(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submit(request, 9);

        assertEquals(PaymentMethod.COD, response.paymentMethod());
        verify(paymentRepository).insert(argThat(payment -> PaymentMethod.COD.equals(payment.getPaymentProvider())));
        verify(paymentService, never()).requestPayosLink(any(Long.class), any(Double.class), anyString(), anyString(), anyString());
    }

    @Test
    void invalidVoucherStopsBeforeAnyCheckoutInsert() {
        CheckoutRequest request = request(PaymentMethod.COD);
        request.setVoucherCode("EXPIRED");
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
        when(customerRepository.findCustomerIdByAccountUserId(9)).thenReturn(null);
        when(voucherService.lockByCode("EXPIRED")).thenReturn(null);
        when(voucherService.evaluateLocked(null, 100_000, 7, null)).thenThrow(new AppException(vn.aeoc.base.constant.ErrorCode.INPUT_WRONG));

        assertThrows(AppException.class, () -> service.submit(request, 9));

        verifyNoInteractions(orderRepository, invoiceRepository, paymentRepository, invoiceVoucherRepository,
                customerVoucherUsageRepository);
    }

    @Test
    void previewCombinesRegularAndRedeemedVoucherDiscountsOnRemainingSubtotal() {
        CheckoutRequest request = request(PaymentMethod.COD);
        request.setVoucherCode("SAVE");
        request.setCustomerVoucherId(77L);
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
        when(customerRepository.findCustomerIdByAccountUserId(9)).thenReturn(33);
        when(customerVoucherRepository.findAvailable(77, 33)).thenReturn(customerVoucher(22));
        when(voucherService.evaluate("SAVE", 100_000, 7, 33))
                .thenReturn(new VoucherService.VoucherEvaluation(voucher(11), 20_000));
        when(voucherService.evaluateRedeemed(22, 80_000, 7))
                .thenReturn(new VoucherService.VoucherEvaluation(voucher(22), 10_000));

        var result = service.preview(request, 9);

        assertEquals(30_000, result.discount());
        assertEquals(10_000, result.loyaltyDiscount());
        assertEquals(100_000, result.subtotal());
        assertEquals(100_000, result.finalAmount());
        assertEquals(77L, result.customerVoucherId());
    }

    @Test
    void submitWithBothVouchersStoresSeparateDiscountsAndResolvedCustomer() {
        CheckoutRequest request = request(PaymentMethod.PAYOS);
        request.setVoucherCode("SAVE");
        request.setCustomerVoucherId(77L);
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
        when(customerRepository.findCustomerIdByAccountUserId(9)).thenReturn(33);
        when(voucherService.lockByCode("SAVE")).thenReturn(voucher(11));
        when(voucherService.evaluateLocked(any(Voucher.class), eq(100_000D), eq(7), eq(33)))
                .thenReturn(new VoucherService.VoucherEvaluation(voucher(11), 20_000));
        when(customerVoucherRepository.findAvailable(77, 33)).thenReturn(customerVoucher(22));
        when(voucherService.evaluateRedeemed(22, 80_000, 7))
                .thenReturn(new VoucherService.VoucherEvaluation(voucher(22), 10_000));
        when(customerVoucherRepository.markUsedIfAvailable(77, 33, null)).thenReturn(1);
        when(customerVoucherRepository.attachInvoice(77, 33, 6)).thenReturn(1);
        when(paymentService.generateUniqueOrderCode()).thenReturn(123L);
        when(paymentService.requestPayosLink(123L, 100_000D, "ORD-5",
                "https://example.test/payment/123/return", "https://example.test/payment/123/cancel"))
                .thenReturn(new PaymentService.PayosLink(123L, "link-5", "https://pay.test/5", "qr-5"));
        Order order = new Order();
        order.setId(5);
        order.setCode("ORD-5");
        when(orderRepository.insertCheckoutOrder(any(Order.class), anyString())).thenAnswer(invocation -> {
            Order inserted = invocation.getArgument(0);
            assertEquals(33, inserted.getCustomerId());
            assertEquals(30_000, inserted.getDiscountAmount());
            assertEquals(10_000, inserted.getLoyaltyVoucherDiscount());
            assertEquals(77, inserted.getCustomerVoucherId());
            return order;
        });
        Invoice invoice = new Invoice();
        invoice.setId(6);
        invoice.setInvoiceCode("INV-6");
        when(invoiceRepository.insertFromOrder(5, 100_000, 30_000, 30_000, PaymentMethod.PAYOS, 33, 7, 9))
                .thenReturn(invoice);
        when(paymentRepository.insert(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submit(request, 9);

        assertEquals(100_000, response.finalAmount());
        verify(paymentService).requestPayosLink(123L, response.finalAmount(), "ORD-5",
                "https://example.test/payment/123/return", "https://example.test/payment/123/cancel");
        verify(invoiceVoucherRepository).insert(6, 11, 20_000D);
        verify(invoiceVoucherRepository).insert(6, 22, 10_000D);
        verify(customerVoucherRepository).markUsedIfAvailable(77, 33, null);
        verify(customerVoucherRepository).attachInvoice(77, 33, 6);
        verify(customerVoucherUsageRepository).increaseUsage(11, 33);
    }

    @Test
    void submitWithOnlyRedeemedVoucherCreatesItsInvoiceVoucherRow() {
        CheckoutRequest request = request(PaymentMethod.COD);
        request.setCustomerVoucherId(77L);
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
        when(customerRepository.findCustomerIdByAccountUserId(9)).thenReturn(33);
        when(customerVoucherRepository.findAvailable(77, 33)).thenReturn(customerVoucher(22));
        when(voucherService.evaluateRedeemed(22, 100_000, 7))
                .thenReturn(new VoucherService.VoucherEvaluation(voucher(22), 10_000));
        when(customerVoucherRepository.markUsedIfAvailable(77, 33, null)).thenReturn(1);
        when(customerVoucherRepository.attachInvoice(77, 33, 6)).thenReturn(1);
        Order order = new Order();
        order.setId(5);
        order.setCode("ORD-5");
        when(orderRepository.insertCheckoutOrder(any(Order.class), anyString())).thenReturn(order);
        Invoice invoice = new Invoice();
        invoice.setId(6);
        invoice.setInvoiceCode("INV-6");
        when(invoiceRepository.insertFromOrder(5, 100_000, 10_000, 30_000, PaymentMethod.COD, 33, 7, 9))
                .thenReturn(invoice);
        when(paymentRepository.insert(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submit(request, 9);

        assertEquals(120_000, response.finalAmount());
        verify(invoiceVoucherRepository).insert(6, 22, 10_000D);
        verifyNoMoreInteractions(invoiceVoucherRepository);
        verify(customerVoucherUsageRepository, never()).increaseUsage(anyInt(), anyInt());
    }

    @Test
    void anotherCustomersVoucherIsRejectedBeforeOrderInsert() {
        CheckoutRequest request = request(PaymentMethod.COD);
        request.setCustomerVoucherId(77L);
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
        when(customerRepository.findCustomerIdByAccountUserId(9)).thenReturn(33);
        when(customerVoucherRepository.findAvailable(77, 33)).thenReturn(null);

        assertThrows(AppException.class, () -> service.submit(request, 9));

        verifyNoInteractions(orderRepository, invoiceRepository, paymentRepository);
    }

    @Test
    void unavailableVoucherClaimFailsBeforeOrderInsert() {
        CheckoutRequest request = request(PaymentMethod.COD);
        request.setCustomerVoucherId(77L);
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
        when(customerRepository.findCustomerIdByAccountUserId(9)).thenReturn(33);
        when(customerVoucherRepository.findAvailable(77, 33)).thenReturn(customerVoucher(22));
        when(voucherService.evaluateRedeemed(22, 100_000, 7))
                .thenReturn(new VoucherService.VoucherEvaluation(voucher(22), 10_000));
        when(customerVoucherRepository.markUsedIfAvailable(77, 33, null)).thenReturn(0);

        assertThrows(AppException.class, () -> service.submit(request, 9));

        verifyNoInteractions(orderRepository, invoiceRepository, paymentRepository);
    }

    @Test
    void redeemedVoucherWithoutCustomerMappingIsRejected() {
        CheckoutRequest request = request(PaymentMethod.COD);
        request.setCustomerVoucherId(77L);
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));

        assertThrows(AppException.class, () -> service.submit(request, 9));

        verifyNoInteractions(orderRepository, invoiceRepository, paymentRepository);
    }

    @Test
    void redeemedVoucherClaimSqlRequiresAvailableStatusAndMatchingOwner() throws Exception {
        AtomicReference<String> sql = new AtomicReference<>();
        AtomicReference<Object[]> bindings = new AtomicReference<>();
        MockDataProvider provider = context -> {
            sql.set(context.sql());
            bindings.set(context.bindings());
            return new MockResult[]{new MockResult(1)};
        };
        DSLContext dsl = org.jooq.impl.DSL.using(new MockConnection(provider), SQLDialect.MYSQL);
        var repository = new vn.aeoc.crm.repository.sale.CustomerVoucherRepository(dsl);

        assertEquals(1, repository.markUsedIfAvailable(7, 33, null));

        String normalizedSql = sql.get().toLowerCase(java.util.Locale.ROOT);
        int whereIndex = normalizedSql.indexOf("where");
        assertTrue(whereIndex >= 0);
        assertTrue(normalizedSql.substring(whereIndex).contains("status"));
        assertTrue(Arrays.asList(bindings.get()).contains("AVAILABLE"));
        assertTrue(Arrays.asList(bindings.get()).contains(7));
        assertTrue(Arrays.asList(bindings.get()).contains(33));
    }

    @Test
    void payosFailureDoesNotInsertInvoiceOrPayment() {
        CheckoutRequest request = request(PaymentMethod.PAYOS);
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
        when(customerRepository.findCustomerIdByAccountUserId(9)).thenReturn(null);
        Order order = new Order();
        order.setId(5);
        order.setCode("ORD-5");
        when(orderRepository.insertCheckoutOrder(any(Order.class), anyString())).thenReturn(order);
        when(paymentService.generateUniqueOrderCode()).thenReturn(123L);
        when(paymentService.requestPayosLink(123L, 130_000, "ORD-5",
                "https://example.test/payment/123/return", "https://example.test/payment/123/cancel"))
                .thenThrow(new IllegalStateException("PayOS down"));

        assertThrows(RuntimeException.class, () -> service.submit(request, 9));

        verify(invoiceRepository).insertFromOrder(5, 100_000, 0, 30_000, PaymentMethod.PAYOS, null, 7, 9);
        verify(paymentRepository, never()).insert(any(Payment.class));
    }

    private CheckoutRequest request(String method) {
        CheckoutRequest request = new CheckoutRequest();
        request.setItems(List.of(new OrderItemRequest()));
        request.setPaymentMethod(method);
        request.setBuyerName("Buyer");
        request.setBuyerPhone("0900000000");
        request.setReceiverName("Receiver");
        request.setReceiverPhone("0911111111");
        request.setAddressLine("Address");
        return request;
    }

    private CustomerVoucher customerVoucher(Integer voucherId) {
        CustomerVoucher customerVoucher = new CustomerVoucher();
        customerVoucher.setId(77);
        customerVoucher.setCustomerId(33);
        customerVoucher.setVoucherId(voucherId);
        customerVoucher.setStatus("AVAILABLE");
        return customerVoucher;
    }

    private Voucher voucher(Integer id) {
        Voucher voucher = new Voucher();
        voucher.setId(id);
        voucher.setCode("SAVE");
        return voucher;
    }
}
