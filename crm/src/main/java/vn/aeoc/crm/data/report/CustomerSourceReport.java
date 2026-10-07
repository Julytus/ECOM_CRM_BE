package vn.aeoc.crm.data.report;

import lombok.Data;

@Data
public class CustomerSourceReport {
    private Integer sourceId;
    private String sourceName;
    private Long totalCustomer;
}