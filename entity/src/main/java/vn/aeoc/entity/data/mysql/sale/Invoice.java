package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Invoice {
    private Integer id;
    private String invoiceCode;
    private String invoiceType;
    private Double amount = 0D;
    private Double discount = 0D;
    private Double vatAmount = 0D;
    private Double fee = 0D;
    private Double amountCard;
    private Double paid;
    private Double debt;
    private Integer paymentType;
    private Integer status;
    private Integer statusTemp;
    private String receiptImage;
    private LocalDateTime receiptDate;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Integer userId;
    private Integer customerId;
    private Integer branchId;
    private Integer orderId;
    private Double shippingFee;
    private String paymentMethod;

    private String voucherCode;
    private Integer voucherId;

    // JOIN thêm
    private String customerName;
    private String userName;
}
