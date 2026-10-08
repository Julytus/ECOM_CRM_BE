package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BoughtService {
    private Integer id;
    private Integer serviceId;
    private String serviceNumber;
    private Integer treatmentNum;
    private Integer qty;
    private String note;
    private Double price;
    private Double priceDiscount;
    private Double discount;
    private Integer discountUnit;
    private String variation;
    private Double fee;
    private Integer saleId;
    private LocalDateTime updatedTime;
    private Integer status;
    private Integer customerId;
    private Integer processData;
    private Integer invoiceId;

    private Integer branchId;

    // JOIN thêm nếu cần
    private String serviceName;
    private String customerName;
}
