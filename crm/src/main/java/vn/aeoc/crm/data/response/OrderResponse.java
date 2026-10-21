package vn.aeoc.crm.data.response;

import lombok.Data;
import vn.aeoc.entity.data.mysql.sale.Order;
import vn.aeoc.entity.data.mysql.sale.OrderItem;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Payment;

import java.util.List;

@Data
public class OrderResponse extends Order {
    private List<OrderItem> items;
    private Integer invoiceId;
    private String invoiceCode;
    private Integer invoiceStatus;
    private Invoice invoice;
    private List<Payment> payments;

    public OrderResponse() {}

    public OrderResponse(Order order, List<OrderItem> items) {
        this.items = items;
        this.setId(order.getId());
        this.setStatus(order.getStatus());
        this.setCreatedAt(order.getCreatedAt());
        this.setUpdatedAt(order.getUpdatedAt());
        this.setCode(order.getCode());
        this.setDiscountAmount(order.getDiscountAmount());
        this.setFinalAmount(order.getFinalAmount());
        this.setTotalAmount(order.getTotalAmount());
        this.setNote(order.getNote());
        this.setPaymentStatus(order.getPaymentStatus());
        this.setPaymentMethod(order.getPaymentMethod());
        this.setUserId(order.getUserId());
        this.setAddress(order.getAddress());
    }

    public OrderResponse(Order order, List<OrderItem> items, Invoice invoice, List<Payment> payments) {
        this(order, items);
        this.invoice = invoice;
        this.payments = payments;
        if (invoice != null) {
            this.invoiceId = invoice.getId();
            this.invoiceCode = invoice.getInvoiceCode();
            this.invoiceStatus = invoice.getStatus();
        }
    }
}
