package vn.aeoc.crm.data.report;


import lombok.Data;

@Data
public class CustomerMonthlyReport {
    private Integer month;          // 1 -> 12
    private Long totalCustomer;
}