package vn.aeoc.crm.data.segment;

import lombok.Data;

@Data
public class CustomerInvoiceAggregate {
    private Integer customerId;
    private Integer branchId;
    private java.time.LocalDateTime lastReceiptTime;
    private Integer recencyDays;
    private Integer frequency90d;
    private Double monetary90d;
}
