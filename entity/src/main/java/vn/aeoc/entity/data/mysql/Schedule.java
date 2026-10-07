package vn.aeoc.entity.data.mysql;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Schedule {

    private Integer id;
    private String title;
    private Integer creatorId;
    private String content;
    private Integer customerId;
    private Integer userId;
    private String userName;
    private String userEmail;
    private String note;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer type;
    private Integer branchId;

    // JOIN thêm
    private String creatorName;   // user.name (creator_id)
    private String customerName;  // customer.name
    private String branchName;    // branch.name
}
