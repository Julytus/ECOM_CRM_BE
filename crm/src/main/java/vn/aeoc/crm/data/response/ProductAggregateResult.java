package vn.aeoc.crm.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductAggregateResult {
    private Integer productId;
    private int attributeCount;
    private int optionCount;
    private int variantCount;
    private int variantOptionLinkCount;
}
