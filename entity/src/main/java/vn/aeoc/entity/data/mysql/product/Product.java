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
}
