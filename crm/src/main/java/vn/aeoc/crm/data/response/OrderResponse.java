package vn.aeoc.crm.data.response;

import lombok.Data;
import vn.aeoc.entity.data.mysql.sale.Order;
import vn.aeoc.entity.data.mysql.sale.OrderItem;

import java.util.List;

@Data
public class OrderResponse extends Order {
    private List<OrderItem> items;

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
}
