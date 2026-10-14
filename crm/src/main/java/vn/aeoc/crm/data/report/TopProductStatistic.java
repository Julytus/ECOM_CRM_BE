package vn.aeoc.crm.data.report;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopProductStatistic {
    private Integer productId;
    private String productName;
    private Integer totalQuantity;
    private Double revenue;
}