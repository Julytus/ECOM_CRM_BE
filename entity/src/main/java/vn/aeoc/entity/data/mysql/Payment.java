package vn.aeoc.entity.data.mysql;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Payment {
    private Integer id;
    private Integer invoiceId;
    private Double amount;
    private String paymentType;
    private String status;
    private String description;
    private String paymentProvider; // tên nhà cung cấp dịch vụ thanh toán (ví dụ: Payos, VNPay, Momo, v.v.)
    private Long orderCode; // mã đơn hàng do các bên thanh toán cung cấp
    private String paymentLinkId;
    private String checkoutUrl;
    private String qrCode; // dạng text
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}