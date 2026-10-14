package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

@Data
public class OrderItem {
    private Integer id;
    private Integer orderId;


    private Integer productId;
    private String productName;

    private Integer productVariantId;
    private String sku;
    private String productVariantImage;

    private Double price;
    private Integer quantity;
    private Double totalPrice;
}