package vn.aeoc.crm.data.report;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RevenueStatistic {
    private String period;
    private Integer totalOrders;
    private BigDecimal revenue;

    public RevenueStatistic(
            String period,
            Integer totalOrders,
            BigDecimal revenue
    ) {
        this.period = period;
        this.totalOrders = totalOrders;
        this.revenue = revenue;
    }
}