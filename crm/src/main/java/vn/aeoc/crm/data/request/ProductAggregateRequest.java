package vn.aeoc.crm.data.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ProductAggregateRequest {
    private Integer id;
    @NotBlank private String name;
    private String description;
    private Integer categoryId;
    /** Existing UI sends active as 0/1; backend schema column is STATUS. */
    @NotNull private Integer status;
    private String thumbnail;
    private String image;

    @Valid private List<AttributeInput> attributes = new ArrayList<>();
    @Valid private List<VariantInput> variants = new ArrayList<>();

    @Data @NoArgsConstructor
    public static class AttributeInput {
        private Integer id;
        @NotBlank private String name;
        @NotBlank private String fieldName;
        @Valid private List<OptionInput> options = new ArrayList<>();
    }

    @Data @NoArgsConstructor
    public static class OptionInput {
        private Integer id;
        @NotBlank private String value;
    }

    @Data @NoArgsConstructor
    public static class VariantInput {
        private Integer id;
        @NotBlank private String sku;
        @NotNull private Double price;
        @NotNull private Double originalPrice;
        @NotNull private Integer stock;
        private String image;
        /** fieldName -> option value, e.g. {"color":"Red","size":"M"}. */
        private Map<String, String> options = new LinkedHashMap<>();
    }
}
