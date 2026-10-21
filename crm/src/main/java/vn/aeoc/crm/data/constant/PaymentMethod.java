package vn.aeoc.crm.data.constant;

import java.util.Arrays;

public final class PaymentMethod {

    private PaymentMethod() {}

    public static final String COD = "COD";
    public static final String VNPAY = "VNPAY";
    public static final String PAYPAL = "PAYPAL";
    public static final String PAYOS = "PAYOS";
    public static final String BANK_TRANSFER = "BANK_TRANSFER";
    public static final String[] CHECKOUT_METHODS = {COD, PAYOS};

    public static boolean isValid(String method) {
        return COD.equals(method)
                || PAYPAL.equals(method)
                || PAYOS.equals(method)
                || BANK_TRANSFER.equals(method);
    }

    public static boolean isCheckoutMethod(String method) {
        return Arrays.asList(CHECKOUT_METHODS).contains(method);
    }
}
