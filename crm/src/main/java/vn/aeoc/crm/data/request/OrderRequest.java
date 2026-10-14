package vn.aeoc.crm.data.request;

import lombok.Data;
import vn.aeoc.entity.data.mysql.sale.Order;

import java.util.List;

@Data
public class OrderRequest extends Order {
    private String paymentReturnUrl;
    private List<OrderItemRequest> items;
}