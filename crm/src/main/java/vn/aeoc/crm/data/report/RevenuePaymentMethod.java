package vn.aeoc.crm.data.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenuePaymentMethod {
    private String method;
    private Double revenue;
    private Long invoiceCount;
}
