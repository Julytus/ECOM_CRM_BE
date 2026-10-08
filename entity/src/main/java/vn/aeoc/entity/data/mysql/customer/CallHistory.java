package vn.aeoc.entity.data.mysql.customer;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class CallHistory {

    private Integer id;
    private Integer userId;
    private Integer customerId;
    private String customerName;
    private String userName;
    private Integer callType;       // 1: Audio, 2: Video
    private Integer outcome;        // trạng thái kết quả cuộc gọi
    private Integer interestLevel;  // mức độ quan tâm
    private Integer duration;       // thời lượng (giây)
    private String note;
    private Integer status;          // 1: active, 0: inactive
    private Timestamp createdTime;

    private Integer branchId;
}
