package vn.aeoc.crm.data.report;

import lombok.Data;

@Data
public class CustomerInterestAvgReport {

    private Integer customerId;
    private String customerName;
    private Long totalCall;
    private Double avgInterestLevel;
}