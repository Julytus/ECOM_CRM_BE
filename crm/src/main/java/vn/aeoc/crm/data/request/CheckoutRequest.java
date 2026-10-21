package vn.aeoc.crm.data.request;

import lombok.Data;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.data.constant.PaymentMethod;

import java.util.List;

@Data
public class CheckoutRequest {
    private List<OrderItemRequest> items;
    private String voucherCode;
    private String paymentMethod;
    private String buyerName;
    private String buyerPhone;
    private String buyerEmail;
    private String receiverName;
    private String receiverPhone;
    private String addressLine;
    private String note;
    private Integer branchId;
    private Integer customerId;

    public void validate() {
        if (items == null || items.isEmpty()
                || blank(buyerName) || blank(buyerPhone)
                || blank(receiverName) || blank(receiverPhone)
                || blank(addressLine) || !PaymentMethod.isCheckoutMethod(paymentMethod)) {
            throw new AppException(ErrorCode.INPUT_WRONG);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
