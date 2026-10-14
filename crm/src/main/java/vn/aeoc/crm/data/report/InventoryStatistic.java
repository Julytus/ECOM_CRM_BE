package vn.aeoc.crm.data.report;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InventoryStatistic {
    private Integer threshold;
    private Integer totalVariants;
    private Integer lowStockVariants;
    private Integer outOfStockVariants;
    private List<InventoryItemStatistic> items;
}