package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Order {

    private Integer id;
    private String code;
    private Integer userId;

    private Double totalAmount;
    private Double discountAmount;
    private Double finalAmount;

    private String status;
    private String paymentMethod;
    private String paymentStatus;

    private String note;
    private String address;
    private String buyerName;
    private String buyerPhone;
    private String buyerEmail;
    private String receiverName;
    private String receiverPhone;
    private Double shippingFee;
    private Integer voucherId;
    private String voucherCode;
    private Integer branchId;
    private Integer customerId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
