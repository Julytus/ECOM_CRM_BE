package vn.aeoc.crm.data.segment;

import lombok.Data;

@Data
public class SegmentRecomputeProgress {
    private String runId;
    private Integer branchId;
    private String status;
    private Integer totalCustomers;
    private Integer chunkSize;
    private Integer totalChunks;
    private Integer processedChunks;
    private Integer processedCustomers;
    private Integer errorCount;
    private Integer checkpointCustomerId;
    private Integer nextChunkIndex;
    private Integer resumeFromChunkIndex;
    private Boolean resumable;
    private String message;
    private Long startedAtEpochMs;
    private Long lastUpdatedAtEpochMs;
}
