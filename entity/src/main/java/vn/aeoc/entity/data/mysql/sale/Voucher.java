package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Voucher {

    private Integer id;
    private String code;
    private String name;
    private Integer discountType;     // 1: FIXED, 2: PERCENT
    private Double discountValue;
    private Double maxDiscount;
    private Double minInvoiceAmount;
    private Integer totalQuantity;
    private Integer usageQuantity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer perUserLimit;
    private Integer status;
    private Integer branchId;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // JOIN thêm
    private String branchName;
}
