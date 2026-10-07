package vn.aeoc.crm.data.segment;

import lombok.Data;

@Data
public class SegmentTrendItem {
    private String period;
    private String segmentCode;
    private Integer totalCustomer;
}
