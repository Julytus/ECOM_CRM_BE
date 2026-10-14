package vn.aeoc.crm.data.response;

import lombok.Data;
import vn.aeoc.entity.data.mysql.product.ProductVariant;

@Data
public class ProductVariantRow extends ProductVariant {
    private String fieldName;
    private String value;
}