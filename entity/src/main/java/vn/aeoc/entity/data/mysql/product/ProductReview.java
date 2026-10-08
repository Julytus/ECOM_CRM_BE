package vn.aeoc.entity.data.mysql.product;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductReview {

    private Integer id;

    private Integer orderId;
    private Integer orderItemId;

    private Integer productId;
    private Integer productVariantId;

    private Integer userId;

    private Integer rating;     // 1..5
    private String comment;
    private String files;       // JSON string

    private LocalDateTime createdAt;
}
