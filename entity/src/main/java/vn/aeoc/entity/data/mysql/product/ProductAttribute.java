package vn.aeoc.entity.data.mysql.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttribute {

    private Integer id;
    private String fieldName;
    private String name;
    private Integer productId;
}
