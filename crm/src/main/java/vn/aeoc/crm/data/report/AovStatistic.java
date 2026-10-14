package vn.aeoc.crm.data.report;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AovStatistic {
    private String period;
    private Integer totalOrders;
    private Double averageOrderValue;
}