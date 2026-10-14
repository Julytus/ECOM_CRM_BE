package vn.aeoc.crm.data.response;

import lombok.Getter;
import lombok.Setter;
import vn.aeoc.entity.data.mysql.product.Product;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProductDetailResponse extends Product {
    private List<ProductAttributeResponse> attributes = new ArrayList<>();
    private List<ProductVariantResponse> variants= new ArrayList<>();

    public ProductDetailResponse(Product product) {
        super(product);
    }

    public ProductDetailResponse() {}
}
