package vn.aeoc.crm.data.report;

import lombok.Data;

@Data
public class RevenueToday {
    private Double totalAmount;
    private Double totalInvoice;
    private Double cashAmount;
    private Double transferAmount;
//    private Double cardAmount;
}