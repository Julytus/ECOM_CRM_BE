package vn.aeoc.crm.data.segment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SegmentJobRunRow {
    private Integer id;
    private String runId;
    private Integer branchId;
    private String mode;
    private String status;
    private Integer totalCustomers;
    private Integer processedCustomers;
    private Integer errorCount;
    private Integer checkpointCustomerId;
    private Integer checkpointChunkIndex;
    private String triggerSource;
    private LocalDateTime startedTime;
    private LocalDateTime finishedTime;
    private String message;
}
