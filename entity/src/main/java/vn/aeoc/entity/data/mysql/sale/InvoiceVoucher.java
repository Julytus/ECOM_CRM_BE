package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InvoiceVoucher {

    private Integer id;
    private Integer invoiceId;
    private Integer voucherId;
    private Double discountAmount;
    private LocalDateTime createdTime;
}
