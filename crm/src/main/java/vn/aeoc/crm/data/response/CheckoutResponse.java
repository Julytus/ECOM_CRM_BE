package vn.aeoc.crm.data.response;

public record CheckoutResponse(
        Integer orderId,
        String orderCode,
        Integer invoiceId,
        String invoiceCode,
        String paymentMethod,
        String paymentStatus,
        double finalAmount,
        String checkoutUrl,
        String qrCode
) {}
