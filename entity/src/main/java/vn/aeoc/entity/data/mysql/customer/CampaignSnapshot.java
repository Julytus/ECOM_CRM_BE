package vn.aeoc.entity.data.mysql.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CampaignSnapshot {
    Integer id;
    String campaign;
    LocalDateTime createdTime;
    Integer customerId;
    Integer status;
}
