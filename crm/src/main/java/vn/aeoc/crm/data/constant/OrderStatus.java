package vn.aeoc.crm.data.constant;

public final class OrderStatus {

    private OrderStatus() {}

    /** Đơn hàng mới tạo */
    public static final String NEW = "NEW";

    /** Đã xác nhận đơn */
    public static final String CONFIRMED = "CONFIRMED";

    /** Đang giao hàng */
    public static final String SHIPPING = "SHIPPING";

    /** Đã giao thành công / đã nhận hàng */
    public static final String COMPLETED = "COMPLETED";

    /** Đơn hàng bị hủy */
    public static final String CANCELLED = "CANCELLED";

    /** Đơn đã hoàn tiền */
    public static final String REFUNDED = "REFUNDED";

    /** Kiểm tra trạng thái hợp lệ */
    public static boolean isValid(String status) {
        return NEW.equals(status)
                || CONFIRMED.equals(status)
                || SHIPPING.equals(status)
                || COMPLETED.equals(status)
                || CANCELLED.equals(status)
                || REFUNDED.equals(status);
    }

    /** Kiểm tra có phải trạng thái kết thúc */
    public static boolean isFinalStatus(String status) {
        return COMPLETED.equals(status)
                || CANCELLED.equals(status)
                || REFUNDED.equals(status);
    }
}
