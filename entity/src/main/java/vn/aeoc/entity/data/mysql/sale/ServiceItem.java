package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.io.Serializable;

@Data
public class ServiceItem implements Serializable {
    private Integer id;

    private Integer categoryId;
    private String categoryName; // join category_item

    private String avatar;

    private String name;

    private String code;

    private String intro;

    private Integer cost;

    private Integer price;

    private Integer discount;

    private String priceVariation; // jsonb -> string

    private Integer totalTime;

    private Integer isCombo;

    private Integer featured;

    private Integer treatmentNum;

    private Integer parentId;

    private Long createdTime;

    private Integer branchId;

    private Integer status;
}
