package vn.aeoc.crm.data.response;

import java.time.LocalDateTime;

public record MyVoucherResponse(Integer id, Integer voucherId, String code, String name, String status,
                                Long pointsSpent, LocalDateTime expiresAt, LocalDateTime createdAt) { }
