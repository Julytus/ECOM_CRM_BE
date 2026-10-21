package vn.aeoc.crm.service.sale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.data.constant.OrderStatus;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.OrderItemRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Order;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OrderLifecycleTest {
    private OrderRepository orderRepository;
    private InvoiceRepository invoiceRepository;
    private InvoiceService invoiceService;
    private OrderService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        invoiceService = mock(InvoiceService.class);
        service = new OrderService(orderRepository, mock(OrderItemRepository.class), mock(OrderItemService.class),
                invoiceRepository, mock(PaymentRepository.class), invoiceService,
                new QueryExecutor(Runnable::run));
    }

    @Test
    void rejectsSkippingLifecycleState() {
        Order order = order(OrderStatus.NEW);
        when(orderRepository.getById(1)).thenReturn(order);
        when(invoiceRepository.findByOrderId(1)).thenReturn(invoice("COD", InvoiceConstant.STATUS_UNPAID));

        assertThrows(RuntimeException.class, () -> service.advance(1, OrderStatus.SHIPPING));
        verify(orderRepository, never()).updateStatusAndPayment(anyInt(), anyString(), anyString());
    }

    @Test
    void completingCodOrderMarksInvoicePaidAndCompletesOrder() {
        when(orderRepository.getById(1)).thenReturn(order(OrderStatus.SHIPPING));
        when(invoiceRepository.findByOrderId(1)).thenReturn(invoice("COD", InvoiceConstant.STATUS_UNPAID));

        service.advance(1, OrderStatus.COMPLETED);

        verify(invoiceService).markPaid(2, "COD");
        verify(orderRepository).updateStatusAndPayment(1, OrderStatus.COMPLETED, "PAID");
    }

    private Order order(String status) {
        Order order = new Order();
        order.setId(1);
        order.setStatus(status);
        order.setPaymentStatus("UNPAID");
        return order;
    }

    private Invoice invoice(String method, int status) {
        Invoice invoice = new Invoice();
        invoice.setId(2);
        invoice.setStatus(status);
        invoice.setPaymentMethod(method);
        return invoice;
    }
}
