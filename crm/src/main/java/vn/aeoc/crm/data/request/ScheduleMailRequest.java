package vn.aeoc.crm.data.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleMailRequest {

    private String customerEmail;

    private Integer scheduleId;
    private String title;
    private String content;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // JOIN data
    private String customerName;
    private String creatorName;
    private String branchName;
}