package vn.aeoc.crm.data.report;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RevenueBucket {
    private String bucket;
    private Double revenue;
    private Long invoiceCount;
}
