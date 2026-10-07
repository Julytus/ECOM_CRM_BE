package vn.aeoc.entity.data.mysql;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Opportunity {

    private Integer id;

    private String code;

    private Integer customerId;
    private Integer userId;
    private Integer branchId;

    private String name;
    private String description;

    private Double expectedValue;
    private Integer probability;

    private Integer stage;

    private Integer priority;

    private String nextActionType;
    private LocalDateTime nextActionDate;

    private LocalDateTime lastActivityDate;

    private LocalDateTime actualCloseDate;

    private String lostReason;

    private LocalDateTime expectedCloseDate;

    private Integer status;

    private LocalDateTime createdTime = LocalDateTime.now();
    private LocalDateTime updatedTime;

    private Integer creatorId;

    // JOIN thêm

    private String customerName;
    private String userName;
    private String branchName;
}