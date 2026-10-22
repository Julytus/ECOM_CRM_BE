package vn.aeoc.crm.service.loyalty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.aeoc.crm.event.InvoiceRefundedEvent;
import vn.aeoc.crm.event.OrderCompletedEvent;
import vn.aeoc.crm.repository.customer.CustomerRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Order;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LoyaltyEventListenerTest {
    private LoyaltyLedgerService ledger;
    private OrderRepository orderRepository;
    private InvoiceRepository invoiceRepository;
    private CustomerRepository customerRepository;
    private LoyaltyEventListener listener;

    @BeforeEach
    void setUp() {
        ledger = mock(LoyaltyLedgerService.class);
        orderRepository = mock(OrderRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        customerRepository = mock(CustomerRepository.class);
        listener = new LoyaltyEventListener(ledger, orderRepository, invoiceRepository, customerRepository);
    }

    @Test
    void earnsForPersistedOrderCustomerOnlyWhenAccountBelongsToOrderUser() {
        Order order = order(33, 7);
        when(orderRepository.getById(5)).thenReturn(order);
        when(customerRepository.findAccountUserIdById(33)).thenReturn(7);

        listener.onOrderCompleted(new OrderCompletedEvent(5, 999, 120D));

        verify(ledger).earnForOrder(33, 5, 120D);
    }

    @Test
    void skipsGuestAndMismatchedOrUnownedCustomer() {
        Order guest = order(null, 7);
        Order mismatch = order(33, 7);
        when(orderRepository.getById(1)).thenReturn(guest);
        when(orderRepository.getById(2)).thenReturn(mismatch);
        when(orderRepository.getById(3)).thenReturn(mismatch);
        when(customerRepository.findAccountUserIdById(33)).thenReturn(8, (Integer) null);

        listener.onOrderCompleted(new OrderCompletedEvent(1, null, 10D));
        listener.onOrderCompleted(new OrderCompletedEvent(2, 33, 10D));
        listener.onOrderCompleted(new OrderCompletedEvent(3, 33, 10D));

        verifyNoInteractions(ledger);
    }

    @Test
    void reversesEarnFromPersistedInvoiceOrderAndPropagatesFailures() {
        Invoice invoice = new Invoice();
        invoice.setId(9);
        invoice.setOrderId(5);
        invoice.setCustomerId(33);
        when(invoiceRepository.getById(9)).thenReturn(invoice);

        listener.onInvoiceRefunded(new InvoiceRefundedEvent(9, 99, 999));
        verify(ledger).reverseOrder(5);

        doThrow(new IllegalStateException("ledger unavailable")).when(ledger).reverseOrder(5);
        assertThrows(IllegalStateException.class,
                () -> listener.onInvoiceRefunded(new InvoiceRefundedEvent(9, 5, 33)));
    }

    private Order order(Integer customerId, Integer userId) {
        Order order = new Order();
        order.setId(5);
        order.setCustomerId(customerId);
        order.setUserId(userId);
        return order;
    }
}
