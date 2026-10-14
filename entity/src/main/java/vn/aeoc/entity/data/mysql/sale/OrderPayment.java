package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderPayment {
    private Integer id;
    private Integer orderId;
    private String provider;
    private String paymentCode;
    private Double amount;
    private String paymentStatus;
    private String rawResponse;
    private LocalDateTime createdAt;
}