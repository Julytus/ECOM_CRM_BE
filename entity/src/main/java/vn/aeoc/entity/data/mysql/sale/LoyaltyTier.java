package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoyaltyTier {

    private Integer id;
    private String name;
    private Long minRankPoints;
    private BigDecimal earnMultiplier;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
