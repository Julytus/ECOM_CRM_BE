package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoyaltyTransaction {

    private Integer id;
    private Integer customerId;
    private String type;
    private Long points;
    private Long rankPointsDelta;
    private Long balanceAfter;
    private Long rankPointsAfter;
    private String refType;
    private String refId;
    private String note;
    private Integer createdBy;
    private LocalDateTime createdAt;
}
