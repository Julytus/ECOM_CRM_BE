package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoyaltyAccount {

    private Integer id;
    private Integer customerId;
    private Long balance;
    private Long rankPoints;
    private Integer tierId;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
