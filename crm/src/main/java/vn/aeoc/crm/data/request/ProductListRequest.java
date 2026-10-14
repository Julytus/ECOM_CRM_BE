package vn.aeoc.crm.data.request;

import lombok.Data;

import java.util.List;

@Data
public class ProductListRequest {

    private String keyword;
    private Integer categoryId;
    private Integer active;

    private List<Integer> ids;

    private Double priceFrom;
    private Double priceTo;

    private Double avgRatingFrom;
    private Double avgRatingTo;

    private String sortBy;
    private String sortDir;
}
