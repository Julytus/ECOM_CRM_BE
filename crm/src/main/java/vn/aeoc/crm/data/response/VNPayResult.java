package vn.aeoc.crm.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VNPayResult {
    private boolean success;
    private Long invoiceId;
    private String transactionId;
    private String paymentTime;
    private String totalPrice;
}