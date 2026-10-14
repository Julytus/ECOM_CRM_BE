package vn.aeoc.crm.data.report;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InventoryItemStatistic {
    private Integer variantId;
    private Integer productId;
    private String productName;
    private String sku;
    private Integer stock;
    private Double price;
}