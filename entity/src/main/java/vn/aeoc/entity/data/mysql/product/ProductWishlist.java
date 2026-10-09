package vn.aeoc.entity.data.mysql.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductWishlist implements Serializable {

    private Integer userId;
    private Integer productId;
    private String productName;
    private LocalDateTime createdAt;
}
