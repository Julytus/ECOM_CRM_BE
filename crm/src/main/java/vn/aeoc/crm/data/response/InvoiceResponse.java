package vn.aeoc.crm.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import vn.aeoc.entity.data.mysql.sale.BoughtProduct;
import vn.aeoc.entity.data.mysql.sale.BoughtService;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Order;
import vn.aeoc.entity.data.mysql.sale.OrderItem;
import vn.aeoc.entity.data.mysql.sale.Payment;

import java.util.List;

@Data
@NoArgsConstructor
public class InvoiceResponse {
    private Invoice invoice;
    private List<BoughtProduct> products;
    private List<BoughtService> services;
    private Order order;
    private List<OrderItem> orderItems;
    private List<Payment> payments;
    private VoucherSummary voucher;

    public InvoiceResponse(Invoice invoice, List<BoughtProduct> products, List<BoughtService> services,
                           Order order, List<OrderItem> orderItems, List<Payment> payments,
                           VoucherSummary voucher) {
        this.invoice = invoice;
        this.products = products;
        this.services = services;
        this.order = order;
        this.orderItems = orderItems;
        this.payments = payments;
        this.voucher = voucher;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoucherSummary {
        private String code;
        private Double discountAmount;
    }

}
