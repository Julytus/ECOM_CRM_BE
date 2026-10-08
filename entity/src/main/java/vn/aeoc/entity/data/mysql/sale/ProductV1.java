package vn.aeoc.entity.data.mysql.sale;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductV1 {

    private Integer id;

    private String name;

    @NotNull(message = "Danh mục sản phẩm không được để trống")
    private Integer categoryId;
    private String categoryName;  // Lấy từ CategoryItem.name

    private String content;

    private String code;

    private String avatar;

    private Double price;

    private Double discount;

    private Integer discountUnit;

    private Integer position;

    private Integer status;

    @NotNull(message = "Đơn vị không được để trống")
    private Integer unitId;
    private String unitName;

    private Integer type;

    private Integer expiredPeriod;

    private Integer branchId;
}
