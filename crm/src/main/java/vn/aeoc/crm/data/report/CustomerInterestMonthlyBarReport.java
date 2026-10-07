package vn.aeoc.crm.data.report;

import lombok.Data;

@Data
public class CustomerInterestMonthlyBarReport {

    private Integer month;               // 1 -> 12
    private Long totalCustomer;           // tổng KH có call trong tháng
    private Long highInterestCustomer;    // KH có avgInterest > N
}