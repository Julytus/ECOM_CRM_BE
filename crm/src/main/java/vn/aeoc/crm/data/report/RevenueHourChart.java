package vn.aeoc.crm.data.report;

import lombok.Data;

@Data
public class RevenueHourChart {

    private Integer hour;

    private Integer totalInvoice;

    private Double totalAmount;

}