package vn.aeoc.crm.service.sale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.config.checkout.CheckoutConfig;
import vn.aeoc.crm.config.payos.PayOSConfig;
import vn.aeoc.crm.data.constant.PaymentMethod;
import vn.aeoc.crm.data.request.CheckoutRequest;
import vn.aeoc.crm.repository.sale.CustomerVoucherUsageRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.InvoiceVoucherRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Order;
import vn.aeoc.entity.data.mysql.sale.Payment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void invalidVoucherStopsBeforeAnyCheckoutInsert() {
        CheckoutRequest request = request(PaymentMethod.COD);
        request.setVoucherCode("EXPIRED");
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
        when(voucherService.lockByCode("EXPIRED")).thenReturn(null);
        when(voucherService.evaluateLocked(null, 100_000, 7, null)).thenThrow(new AppException(vn.aeoc.base.constant.ErrorCode.INPUT_WRONG));

        assertThrows(AppException.class, () -> service.submit(request, 9));

        verifyNoInteractions(orderRepository, invoiceRepository, paymentRepository, invoiceVoucherRepository,
                customerVoucherUsageRepository);
    }

    @Test
    void payosFailureDoesNotInsertInvoiceOrPayment() {
        CheckoutRequest request = request(PaymentMethod.PAYOS);
        when(orderItemService.calculate(request.getItems()))
                .thenReturn(new OrderItemService.CalculatedItems(List.of(), 100_000));
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
}
