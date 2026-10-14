package vn.aeoc.crm.data.response;

import lombok.Data;

@Data
public class ProductDetailRow {

    private Integer productId;
    private String productName;
    private String productDescription;
    private String productImage;
    private String productThumbnail;
    private Float productAvgRating;
    private Integer categoryId;
    private Integer productActive;

    private Integer attributeId;
    private String attributeCode;
    private String attributeName;

    private Integer valueId;
    private String value;
}
