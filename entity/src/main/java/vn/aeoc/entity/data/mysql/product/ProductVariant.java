package vn.aeoc.entity.data.mysql.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    private Integer id;
    private Integer productId;
    private String productName;
    private String sku;
    private String image;
    private Double price;
    private Double originalPrice;
    private Integer stock;
    private LocalDateTime createdAt;
}
