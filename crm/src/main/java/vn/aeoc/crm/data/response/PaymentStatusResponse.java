package vn.aeoc.crm.data.response;

import java.time.LocalDateTime;

public record PaymentStatusResponse(Integer orderId, String orderCode, Integer invoiceId,
                                    Integer invoiceStatus, String orderStatus,
                                    String paymentStatus, LocalDateTime paidAt) {}
