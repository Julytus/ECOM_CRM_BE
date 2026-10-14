package vn.aeoc.crm.data.report;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderStatusStatistic {
    private String status;
    private Integer totalOrders;
    private Double percentage;
}