package vn.aeoc.crm.data.segment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerSegmentSnapshotRow {
    private Integer id;
    private Integer branchId;
    private Integer customerId;
    private String segmentCode;
    private Integer recencyDays;
    private Integer frequency90d;
    private Double monetary90d;
    private Double totalScore;
    private String ruleVersion;
    private Integer active;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
