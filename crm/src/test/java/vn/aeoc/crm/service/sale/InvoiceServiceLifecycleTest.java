package vn.aeoc.crm.service.sale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.data.constant.OrderStatus;
import vn.aeoc.crm.data.constant.PaymentStatus;
import vn.aeoc.crm.event.InvoicePaidEvent;
import vn.aeoc.crm.event.InvoiceRefundedEvent;
import vn.aeoc.crm.repository.sale.CustomerVoucherUsageRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.InvoiceVoucherRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.crm.repository.sale.OrderItemRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InvoiceServiceLifecycleTest {
    private InvoiceRepository invoiceRepository;
    private InvoiceVoucherRepository invoiceVoucherRepository;
    private CustomerVoucherUsageRepository usageRepository;
    private OrderRepository orderRepository;
    private PaymentRepository paymentRepository;
    private ApplicationEventPublisher eventPublisher;
    private InvoiceService service;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        invoiceVoucherRepository = mock(InvoiceVoucherRepository.class);
        usageRepository = mock(CustomerVoucherUsageRepository.class);
        orderRepository = mock(OrderRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new InvoiceService(invoiceRepository, invoiceVoucherRepository, usageRepository,
                new QueryExecutor(Runnable::run), mock(BoughtProductService.class),
                mock(BoughtServiceItemService.class), mock(ThreeLayerCacheTemplate.class),
                orderRepository, mock(OrderItemRepository.class), paymentRepository, eventPublisher);
    }

    @Test
    void markPaidPublishesOnlyOnceAndSynchronizesOrderAndPayment() {
        Invoice invoice = invoice(InvoiceConstant.STATUS_UNPAID);
        when(invoiceRepository.getById(12)).thenAnswer(invocation -> invoice);
        Order order = new Order();
        order.setStatus(OrderStatus.NEW);
        when(orderRepository.getById(44)).thenReturn(order);
        when(invoiceRepository.transitionStatus(eq(12), eq(InvoiceConstant.STATUS_UNPAID),
                eq(InvoiceConstant.STATUS_DONE), anyMap())).thenAnswer(invocation -> {
            invoice.setStatus(InvoiceConstant.STATUS_DONE);
            java.util.Map<?, ?> fields = invocation.getArgument(3);
            invoice.setPaid((Double) fields.get(vn.entity.backend.tables.Invoice.INVOICE.PAID));
            invoice.setDebt((Double) fields.get(vn.entity.backend.tables.Invoice.INVOICE.DEBT));
            return 1;
        });

        assertEquals(1, service.markPaid(12, "COD"));
        assertEquals(0, service.markPaid(12, "PAYOS"));

        assertEquals(InvoiceConstant.STATUS_DONE, invoice.getStatus());
        assertEquals(120D, invoice.getPaid());
        assertEquals(0D, invoice.getDebt());
        verify(orderRepository).updateStatusAndPayment(44, OrderStatus.NEW, PaymentStatus.PAID);
        verify(paymentRepository).markStatusByInvoice(12, "COMPLETED");
        verify(eventPublisher, times(1)).publishEvent(any(InvoicePaidEvent.class));
    }

    @Test
    void markPaidDoesNotPublishOrUpdateRelatedRecordsWhenTransitionLosesRace() {
        Invoice invoice = invoice(InvoiceConstant.STATUS_UNPAID);
        when(invoiceRepository.getById(12)).thenReturn(invoice);
        when(invoiceRepository.transitionStatus(eq(12), eq(InvoiceConstant.STATUS_UNPAID),
                eq(InvoiceConstant.STATUS_DONE), anyMap())).thenReturn(0);

        assertEquals(0, service.markPaid(12, "PAYOS"));

        verifyNoInteractions(orderRepository, paymentRepository, invoiceVoucherRepository,
                usageRepository, eventPublisher);
        verify(invoiceRepository).transitionStatus(eq(12), eq(InvoiceConstant.STATUS_UNPAID),
                eq(InvoiceConstant.STATUS_DONE), anyMap());
    }

    @Test
    void cancellingUnpaidInvoiceReleasesVoucherAndCancelsPendingPayment() {
        Invoice invoice = invoice(InvoiceConstant.STATUS_UNPAID);
        when(invoiceRepository.getById(12)).thenReturn(invoice);
        when(invoiceRepository.transitionStatus(eq(12), eq(InvoiceConstant.STATUS_UNPAID),
                eq(InvoiceConstant.STATUS_CANCEL), anyMap())).thenReturn(1);
        when(invoiceVoucherRepository.findVoucherIdByInvoice(12)).thenReturn(8);
        when(invoiceVoucherRepository.deleteByInvoice(12)).thenReturn(1);

        assertEquals(1, service.cancelOrderInvoice(12));

        verify(orderRepository).updateStatusAndPayment(44, OrderStatus.CANCELLED, PaymentStatus.UNPAID);
        verify(paymentRepository).cancelPendingByInvoice(12);
        verify(usageRepository).decreaseUsage(8, 33);
    }

    @Test
    void doneInvoiceCannotBeCancelledAndRefundSynchronizesOrderAndPayment() {
        Invoice invoice = invoice(InvoiceConstant.STATUS_DONE);
        when(invoiceRepository.getById(12)).thenReturn(invoice);
        when(invoiceRepository.transitionStatus(eq(12), eq(InvoiceConstant.STATUS_DONE),
                eq(InvoiceConstant.STATUS_REFUNDED), anyMap())).thenReturn(1);

        assertThrows(RuntimeException.class, () -> service.cancelOrderInvoice(12));
        assertEquals(1, service.refund(12, "requested"));

        verify(orderRepository).updateStatusAndPayment(44, OrderStatus.REFUNDED, PaymentStatus.REFUNDED);
        verify(paymentRepository).markStatusByInvoice(12, "REFUNDED");
        verify(eventPublisher).publishEvent(new InvoiceRefundedEvent(12, 44, 33));
    }

    private Invoice invoice(int status) {
        Invoice invoice = new Invoice();
        invoice.setId(12);
        invoice.setStatus(status);
        invoice.setFee(120D);
        invoice.setOrderId(44);
        invoice.setCustomerId(33);
        invoice.setBranchId(2);
        invoice.setUserId(7);
        return invoice;
    }
}
