package vn.aeoc.crm.data.segment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerSegmentHistoryRow {
    private Integer id;
    private String runId;
    private Integer branchId;
    private Integer customerId;
    private String fromSegment;
    private String toSegment;
    private Integer recencyDays;
    private Integer frequency90d;
    private Double monetary90d;
    private Double totalScore;
    private String ruleVersion;
    private LocalDateTime createdTime;
}
