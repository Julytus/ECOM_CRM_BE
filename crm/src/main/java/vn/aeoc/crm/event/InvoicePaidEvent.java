package vn.aeoc.crm.event;

public record InvoicePaidEvent(Integer invoiceId, Integer orderId, Integer userId, Integer customerId, double amount) {}
