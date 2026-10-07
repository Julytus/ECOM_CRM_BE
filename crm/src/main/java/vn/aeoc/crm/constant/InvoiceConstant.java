package vn.aeoc.crm.constant;

/**
 * Định nghĩa hằng số cho người dùng
 */
public class InvoiceConstant {
    public static final int STATUS_DRAFT = 0;   // Hóa đơn nháp
    public static final int STATUS_DONE  = 1;   // Hoàn tất
    public static final int STATUS_CANCEL  = 2;   // Hủy
    public static final int STATUS_UNPAID  = 3;   // Chưa thanh toán

    public static final String SALE_INVOICE = "SALE";  // Hóa đơn bán hàng

    public static final int PAYMENT_CASH = 1;        // Tiền mặt
    public static final int PAYMENT_TRANSFER = 2;    // Chuyển khoản
}
