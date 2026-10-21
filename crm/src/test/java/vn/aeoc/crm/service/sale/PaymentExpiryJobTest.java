package vn.aeoc.crm.service.sale;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import vn.aeoc.crm.config.checkout.CheckoutConfig;
import vn.aeoc.crm.config.payos.PayOSConfig;
import vn.aeoc.crm.config.socket.ChatSocketHandler;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Payment;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentExpiryJobTest {
    private PaymentRepository paymentRepository;
    private InvoiceRepository invoiceRepository;
    private InvoiceService invoiceService;
    private RestTemplate restTemplate;
    private PaymentExpiryJob job;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        invoiceService = mock(InvoiceService.class);
        restTemplate = mock(RestTemplate.class);
        PayOSConfig payOSConfig = new PayOSConfig();
        payOSConfig.apiUrl = "https://api.example/v2/payment-requests";
        payOSConfig.clientId = "client";
        payOSConfig.apiKey = "api-key";
        PaymentService paymentService = new PaymentService(payOSConfig, paymentRepository, invoiceRepository,
                mock(ChatSocketHandler.class), new ObjectMapper(), invoiceService, mock(OrderRepository.class));
        paymentService.setRestTemplateForTest(restTemplate);
        CheckoutConfig checkoutConfig = new CheckoutConfig();
        checkoutConfig.payosExpireMinutes = 30;
        job = new PaymentExpiryJob(paymentRepository, paymentService, checkoutConfig);
    }

    @Test
    void paidAtExpiryIsSettledInsteadOfCancelled() {
        Invoice invoice = unpaidInvoice();
        when(invoiceRepository.getById(10)).thenReturn(invoice);
        when(paymentRepository.findExpiredPendingPayos(any())).thenReturn(List.of(pendingPayment(LocalDateTime.now().minusMinutes(40))));
        mockPayosStatus("PAID");

        job.expireUnpaidPayosInvoices();

        verify(invoiceService).markPaid(10, "PAYOS");
        verify(invoiceService, never()).cancelOrderInvoice(anyInt());
    }

    @Test
    void cancelledAtPayosIsCancelledLocally() {
        when(invoiceRepository.getById(10)).thenReturn(unpaidInvoice());
        when(paymentRepository.findExpiredPendingPayos(any())).thenReturn(List.of(pendingPayment(LocalDateTime.now().minusMinutes(40))));
        mockPayosStatus("CANCELLED");

        job.expireUnpaidPayosInvoices();

        verify(invoiceService).cancelOrderInvoice(10);
        verify(invoiceService, never()).markPaid(anyInt(), anyString());
    }

    @Test
    void stillPendingAtPayosIsNotCancelledLocally() {
        when(invoiceRepository.getById(10)).thenReturn(unpaidInvoice());
        when(paymentRepository.findExpiredPendingPayos(any())).thenReturn(List.of(pendingPayment(LocalDateTime.now().minusMinutes(40))));
        mockPayosStatus("PENDING");

        job.expireUnpaidPayosInvoices();

        verify(invoiceService, never()).cancelOrderInvoice(anyInt());
        verify(invoiceService, never()).markPaid(anyInt(), anyString());
    }

    @Test
    void payosLookupFailureLeavesInvoiceUnchanged() {
        when(invoiceRepository.getById(10)).thenReturn(unpaidInvoice());
        when(paymentRepository.findExpiredPendingPayos(any())).thenReturn(List.of(pendingPayment(LocalDateTime.now().minusMinutes(40))));
        when(restTemplate.exchange(eq("https://api.example/v2/payment-requests/12345"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(String.class))).thenThrow(new ResourceAccessException("timeout"));

        job.expireUnpaidPayosInvoices();

        verify(invoiceService, never()).cancelOrderInvoice(anyInt());
        verify(invoiceService, never()).markPaid(anyInt(), anyString());
    }

    @Test
    void unknownPayosStatusLeavesInvoiceUnchanged() {
        when(invoiceRepository.getById(10)).thenReturn(unpaidInvoice());
        when(paymentRepository.findExpiredPendingPayos(any())).thenReturn(List.of(pendingPayment(LocalDateTime.now().minusMinutes(40))));
        mockPayosStatus("REFUNDED");

        job.expireUnpaidPayosInvoices();

        verify(invoiceService, never()).cancelOrderInvoice(anyInt());
        verify(invoiceService, never()).markPaid(anyInt(), anyString());
    }

    @Test
    void recentRetryPaymentDoesNotExpireWithOldInvoice() {
        Payment retriedPayment = pendingPayment(LocalDateTime.now().minusMinutes(5));
        when(paymentRepository.findExpiredPendingPayos(any())).thenReturn(List.of(retriedPayment));

        job.expireUnpaidPayosInvoices();

        verifyNoInteractions(restTemplate, invoiceRepository, invoiceService);
    }

    private void mockPayosStatus(String status) {
        when(restTemplate.exchange(eq("https://api.example/v2/payment-requests/12345"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"code\":\"00\",\"data\":{\"status\":\"" + status + "\"}}"));
    }

    private Invoice unpaidInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId(10);
        invoice.setStatus(InvoiceConstant.STATUS_UNPAID);
        invoice.setPaymentMethod("PAYOS");
        return invoice;
    }

    private Payment pendingPayment(LocalDateTime createdTime) {
        Payment payment = new Payment();
        payment.setId(20);
        payment.setInvoiceId(10);
        payment.setOrderCode(12345L);
        payment.setStatus("PENDING");
        payment.setCreatedTime(createdTime);
        return payment;
    }
}
