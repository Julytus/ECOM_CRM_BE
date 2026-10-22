package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoyaltyConfig {

    private Integer id;
    private Integer earnRate;
    private LocalDateTime updatedAt;
}
