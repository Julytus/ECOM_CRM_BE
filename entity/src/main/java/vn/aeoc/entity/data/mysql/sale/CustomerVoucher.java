package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerVoucher {

    private Integer id;
    private Integer customerId;
    private Integer voucherId;
    private String status;
    private Long pointsSpent;
    private Integer invoiceId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;
}
