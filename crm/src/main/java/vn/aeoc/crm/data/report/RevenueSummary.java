package vn.aeoc.crm.data.report;

import lombok.Data;

@Data
public class RevenueSummary {
    private Double totalRevenue;
    private Long invoiceCount;
    private Double avgOrderValue;
    private Double discountTotal;
    private Double shippingTotal;
    private Double unpaidAmount;
    private Long cancelledCount;
    private Double refundedAmount;

    private Double previousTotalRevenue;
    private Long previousInvoiceCount;
    private Double previousAvgOrderValue;
    private Double previousDiscountTotal;
    private Double previousShippingTotal;
    private Double previousUnpaidAmount;
    private Long previousCancelledCount;
    private Double previousRefundedAmount;

    private Double totalRevenueChangePercent;
    private Double invoiceCountChangePercent;
}
