package vn.aeoc.crm.data.response;

import lombok.Data;
import vn.aeoc.entity.data.mysql.product.ProductVariant;

import java.util.Map;

@Data
public class ProductVariantResponse extends ProductVariant {

    private Map<String, String> options;

    public ProductVariantResponse() {
    }

    public ProductVariantResponse(
            ProductVariant variant,
            Map<String, String> options) {

        this.setId(variant.getId());
        this.setProductId(variant.getProductId());
        this.setProductName(variant.getProductName());
        this.setSku(variant.getSku());
        this.setImage(variant.getImage());
        this.setPrice(variant.getPrice());
        this.setOriginalPrice(variant.getOriginalPrice());
        this.setStock(variant.getStock());
        this.setCreatedAt(variant.getCreatedAt());

        this.options = options;
    }
}
