package vn.aeoc.crm.data.constant;

public final class PaymentStatus {

    private PaymentStatus() {}

    /** Chưa thanh toán */
    public static final String UNPAID = "UNPAID";

    /** Đã thanh toán */
    public static final String PAID = "PAID";

    /** Thanh toán một phần */
    public static final String PARTIAL = "PARTIAL";

    /** Thanh toán thất bại */
    public static final String FAILED = "FAILED";

    /** Đã hoàn tiền */
    public static final String REFUNDED = "REFUNDED";

    public static boolean isValid(String status) {
        return UNPAID.equals(status)
                || PAID.equals(status)
                || PARTIAL.equals(status)
                || FAILED.equals(status)
                || REFUNDED.equals(status);
    }
}