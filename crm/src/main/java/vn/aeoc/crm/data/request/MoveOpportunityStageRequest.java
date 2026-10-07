package vn.aeoc.crm.data.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MoveOpportunityStageRequest {

    private Integer toStage;

    // payload for required fields before moving stage
    private Integer userId;
    private Double expectedValue;
    private Integer probability;
    private LocalDateTime expectedCloseDate;
    private LocalDateTime lastActivityDate;
    private LocalDateTime actualCloseDate;
    private String lostReason;
    private Integer priority;
    private String nextActionType;
    private LocalDateTime nextActionDate;
}