package vn.aeoc.crm.service.loyalty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.aeoc.crm.event.InvoiceRefundedEvent;
import vn.aeoc.crm.event.OrderCompletedEvent;
import vn.aeoc.crm.repository.customer.CustomerRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Order;

@Slf4j
@Component
public class LoyaltyEventListener {
    private final LoyaltyLedgerService ledger;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    public LoyaltyEventListener(LoyaltyLedgerService ledger, OrderRepository orderRepository,
                                InvoiceRepository invoiceRepository, CustomerRepository customerRepository) {
        this.ledger = ledger;
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onOrderCompleted(OrderCompletedEvent event) {
        Order order = orderRepository.getById(event.orderId());
        if (order == null || order.getCustomerId() == null) return;

        Integer accountUserId = customerRepository.findAccountUserIdById(order.getCustomerId());
        if (order.getUserId() == null || !order.getUserId().equals(accountUserId)) {
            log.info("Skip loyalty earn for order {}: customer {} account owner does not match order user",
                    order.getId(), order.getCustomerId());
            return;
        }

        ledger.earnForOrder(order.getCustomerId(), order.getId(), event.baseAmount());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onInvoiceRefunded(InvoiceRefundedEvent event) {
        Invoice invoice = invoiceRepository.getById(event.invoiceId());
        if (invoice == null || invoice.getOrderId() == null) return;
        ledger.reverseOrder(invoice.getOrderId());
    }
}
