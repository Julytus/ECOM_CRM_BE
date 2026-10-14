package vn.aeoc.crm.data.request;

import lombok.Data;
import java.util.List;

@Data
public class AiIntentResult {

    private String intent; // CHAT | SEARCH_PRODUCT

    private List<String> keywords;

    private Integer categoryId;
    private Double priceFrom;
    private Double priceTo;

    private String sortBy;  // minPrice | maxPrice | avgRating
    private String sortDir; // asc | desc

    private Integer limit;
}
