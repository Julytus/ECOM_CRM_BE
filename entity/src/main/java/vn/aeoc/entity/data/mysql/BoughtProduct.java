package vn.aeoc.entity.data.mysql;

import lombok.Data;

@Data
public class BoughtProduct {
    private Integer id;
    private Integer productId;
    private Integer unitId;
    private Double price;
    private Integer qty;
    private Double fee;
    private Integer saleId;
    private String note;
    private Integer status;
    private Integer customerId;
    private Integer invoiceId;

    private Integer branchId;

    // join field
    private String productName;
    private String customerName;
    private String unitName;
}
