package vn.aeoc.entity.data.mysql.product;

import jakarta.validation.constraints.NotEmpty;
//import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Product {

    private Integer id;
    @NotEmpty(message = "Tên sản phẩm không để trống")
    private String name;
    private String description;
//    @NotNull(message = "Danh mục sản phẩm không để trống")
    private Integer categoryId;
    private String categoryName;
    private Float avgRating = 0f;
    private String image;
    private String thumbnail;
    private Integer status;
    private Double minPrice = 0.0;
    private Double maxPrice = 0.0;
    private Double originalMinPrice = 0.0;
    private Double originalMaxPrice = 0.0;
    private Integer branchId;

    public Product(Product product) {
        this.id = product.id;
        this.name = product.name;
        this.description = product.description;
        this.categoryId = product.categoryId;
        this.categoryName = product.categoryName;
        this.avgRating = product.avgRating;
        this.image = product.image;
        this.thumbnail = product.thumbnail;
        this.status = product.status;
        this.minPrice = product.minPrice;
        this.maxPrice = product.maxPrice;
        this.originalMinPrice = product.originalMinPrice;
        this.originalMaxPrice = product.originalMaxPrice;
    }

    public Product() {}
}
