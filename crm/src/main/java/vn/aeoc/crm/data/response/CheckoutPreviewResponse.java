package vn.aeoc.crm.data.response;

public record CheckoutPreviewResponse(
        double subtotal,
        double discount,
        double shippingFee,
        double finalAmount,
        String voucherCode,
        double loyaltyDiscount,
        Long customerVoucherId
) {}
