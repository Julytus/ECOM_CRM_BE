package vn.aeoc.crm.data.constant;

public final class PaymentMethod {

    private PaymentMethod() {}

    public static final String COD = "COD";
    public static final String VNPAY = "VNPAY";
    public static final String PAYPAL = "PAYPAL";
    public static final String PAYOS = "PAYOS";
    public static final String BANK_TRANSFER = "BANK_TRANSFER";

    public static boolean isValid(String method) {
        return COD.equals(method)
                || PAYPAL.equals(method)
                || PAYOS.equals(method)
                || BANK_TRANSFER.equals(method);
    }
}