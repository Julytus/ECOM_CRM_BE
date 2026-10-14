package vn.aeoc.crm.data.request;
import lombok.Data;

@Data
public class OrderItemRequest {

    private Integer productVariantId;
    private Integer quantity;
}