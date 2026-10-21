package vn.aeoc.crm.service.sale;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.aeoc.crm.config.payos.PayOSConfig;
import vn.aeoc.crm.config.socket.ChatSocketHandler;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PaymentServiceWebhookTest {
    private static final String KEY = "test-checksum-key";
    private PaymentRepository paymentRepository;
    private InvoiceRepository invoiceRepository;
    private ChatSocketHandler socketHandler;
    private InvoiceService invoiceService;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        PayOSConfig config = new PayOSConfig();
        config.checksumKey = KEY;
        paymentRepository = mock(PaymentRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        socketHandler = mock(ChatSocketHandler.class);
        invoiceService = mock(InvoiceService.class);
        service = new PaymentService(config, paymentRepository, invoiceRepository, socketHandler,
                new ObjectMapper(), invoiceService);
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
    void emptyChecksumKeyFailsClosed() {
        PayOSConfig config = new PayOSConfig();
        config.checksumKey = " ";
        PaymentService noKeyService = new PaymentService(config, paymentRepository, invoiceRepository,
                socketHandler, new ObjectMapper(), invoiceService);

        assertEquals("INVALID_SIGNATURE", noKeyService.handleWebhook(
                "{\"code\":\"00\",\"success\":true,\"data\":{\"orderCode\":12345},\"signature\":\"abc\"}"));
        verifyNoInteractions(paymentRepository, invoiceService, socketHandler);
    }

    private String hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(String.format("%02x", b));
        return result.toString();
    }
}
