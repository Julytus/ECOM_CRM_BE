package vn.aeoc.crm.service.sale;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.config.payos.PayOSConfig;
import vn.aeoc.crm.config.socket.ChatSocketHandler;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private static final String KEY = "test-checksum-key";
    private PaymentRepository paymentRepository;
    private InvoiceRepository invoiceRepository;
    private ChatSocketHandler socketHandler;
    private InvoiceService invoiceService;
    private OrderRepository orderRepository;
    private RestTemplate restTemplate;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        PayOSConfig config = new PayOSConfig();
        config.checksumKey = KEY;
        config.apiUrl = "https://api.example/v2/payment-requests";
        paymentRepository = mock(PaymentRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        socketHandler = mock(ChatSocketHandler.class);
        invoiceService = mock(InvoiceService.class);
        orderRepository = mock(OrderRepository.class);
        restTemplate = mock(RestTemplate.class);
        service = new PaymentService(config, paymentRepository, invoiceRepository, socketHandler,
                new ObjectMapper(), invoiceService, orderRepository);
        service.setRestTemplateForTest(restTemplate);
    }

    @Test
    void invalidSignatureDoesNotChangePaymentOrInvoice() {
        String body = "{\"code\":\"00\",\"success\":true,\"data\":{\"orderCode\":12345},\"signature\":\"bad\"}";

        assertEquals("INVALID_SIGNATURE", service.handleWebhook(body));

        verifyNoInteractions(paymentRepository, invoiceRepository, invoiceService, socketHandler);
    }

    @Test
    void validSignatureMarksInvoicePaidAndEmitsSocketEvent() throws Exception {
        String data = "{\"orderCode\":12345,\"amount\":1200}";
        String signature = hmac("amount=1200&orderCode=12345");
        String body = "{\"code\":\"00\",\"success\":true,\"data\":" + data
                + ",\"signature\":\"" + signature + "\"}";
        Payment payment = new Payment();
        payment.setId(1);
        payment.setInvoiceId(9);
        when(paymentRepository.findByPayosOrderCode(12345L)).thenReturn(payment);
        when(invoiceService.markPaid(9, "PAYOS")).thenReturn(1);
        Invoice invoice = new Invoice();
        invoice.setUserId(7);
        when(invoiceRepository.getById(9)).thenReturn(invoice);

        assertEquals("SUCCESS", service.handleWebhook(body));

        verify(invoiceService).markPaid(9, "PAYOS");
        verify(paymentRepository, never()).updateStatus(anyInt(), anyString());
        verify(socketHandler).emitPaymentDone(7, 9);
    }

    @Test
    void lateWebhookForCancelledInvoiceIsIgnored() throws Exception {
        Payment payment = new Payment();
        payment.setInvoiceId(9);
        payment.setOrderCode(12345L);
        when(paymentRepository.findByPayosOrderCode(12345L)).thenReturn(payment);
        Invoice invoice = new Invoice();
        invoice.setId(9);
        invoice.setStatus(vn.aeoc.crm.constant.InvoiceConstant.STATUS_CANCEL);
        when(invoiceRepository.getById(9)).thenReturn(invoice);
        when(invoiceService.markPaid(9, "PAYOS")).thenThrow(new AppException(ErrorCode.BAD_REQUEST));

        assertEquals("IGNORED", service.handleWebhook(signedWebhook()));

        verify(invoiceService).markPaid(9, "PAYOS");
        verify(socketHandler, never()).emitPaymentDone(anyInt(), anyInt());
    }

    @Test
    void lateWebhookForRefundedInvoiceIsIgnored() throws Exception {
        Payment payment = new Payment();
        payment.setInvoiceId(9);
        payment.setOrderCode(12345L);
        when(paymentRepository.findByPayosOrderCode(12345L)).thenReturn(payment);
        Invoice invoice = new Invoice();
        invoice.setId(9);
        invoice.setStatus(vn.aeoc.crm.constant.InvoiceConstant.STATUS_REFUNDED);
        when(invoiceRepository.getById(9)).thenReturn(invoice);
        when(invoiceService.markPaid(9, "PAYOS")).thenThrow(new AppException(ErrorCode.BAD_REQUEST));

        assertEquals("IGNORED", service.handleWebhook(signedWebhook()));

        verify(invoiceService).markPaid(9, "PAYOS");
        verify(socketHandler, never()).emitPaymentDone(anyInt(), anyInt());
    }

    @Test
    void webhookRethrowsSystemFailureSoPayosCanRetry() throws Exception {
        Payment payment = new Payment();
        payment.setInvoiceId(9);
        when(paymentRepository.findByPayosOrderCode(12345L)).thenReturn(payment);
        when(invoiceService.markPaid(9, "PAYOS")).thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> service.handleWebhook(signedWebhook()));
    }

    @Test
    void emptyChecksumKeyFailsClosed() {
        PayOSConfig config = new PayOSConfig();
        config.checksumKey = " ";
        PaymentService noKeyService = new PaymentService(config, paymentRepository, invoiceRepository,
                socketHandler, new ObjectMapper(), invoiceService);

        assertEquals("INVALID_SIGNATURE", noKeyService.handleWebhook(
                "{\"code\":\"00\",\"success\":true,\"data\":{\"orderCode\":12345},\"signature\":\"abc\"}"));
        verifyNoInteractions(paymentRepository, invoiceService, socketHandler);
    }

    @Test
    void syncFromPayosSettlesPaidOrderForItsOwner() {
        Payment payment = new Payment();
        payment.setInvoiceId(9);
        payment.setOrderCode(12345L);
        when(paymentRepository.findByPayosOrderCode(12345L)).thenReturn(payment);
        Invoice invoice = new Invoice();
        invoice.setId(9);
        invoice.setOrderId(4);
        invoice.setUserId(7);
        invoice.setStatus(vn.aeoc.crm.constant.InvoiceConstant.STATUS_UNPAID);
        when(invoiceRepository.getById(9)).thenReturn(invoice);
        vn.aeoc.entity.data.mysql.sale.Order order = new vn.aeoc.entity.data.mysql.sale.Order();
        order.setId(4);
        order.setCode("ORD-4");
        order.setStatus("CONFIRMED");
        order.setPaymentStatus("PAID");
        when(orderRepository.getById(4)).thenReturn(order);
        when(restTemplate.exchange(eq("https://api.example/v2/payment-requests/12345"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":\"00\",\"data\":{\"status\":\"PAID\"}}"));
        when(invoiceService.markPaid(9, "PAYOS")).thenReturn(1);

        var result = service.syncFromPayos("12345", 7);

        assertEquals(4, result.orderId());
        assertEquals("ORD-4", result.orderCode());
        verify(invoiceService).markPaid(9, "PAYOS");
    }

    @Test
    void syncFromPayosRejectsAnotherUsersInvoiceBeforePayosRequest() {
        Payment payment = new Payment();
        payment.setInvoiceId(9);
        when(paymentRepository.findByPayosOrderCode(12345L)).thenReturn(payment);
        Invoice invoice = new Invoice();
        invoice.setId(9);
        invoice.setUserId(8);
        when(invoiceRepository.getById(9)).thenReturn(invoice);

        assertThrows(RuntimeException.class, () -> service.syncFromPayos("12345", 7));
        verifyNoInteractions(restTemplate);
    }

    private String hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(String.format("%02x", b));
        return result.toString();
    }

    private String signedWebhook() throws Exception {
        String signature = hmac("amount=1200&orderCode=12345");
        return "{\"code\":\"00\",\"success\":true,\"data\":{\"orderCode\":12345,\"amount\":1200},"
                + "\"signature\":\"" + signature + "\"}";
    }
}
