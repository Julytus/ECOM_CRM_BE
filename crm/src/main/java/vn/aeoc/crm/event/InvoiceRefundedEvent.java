package vn.aeoc.crm.event;

public record InvoiceRefundedEvent(Integer invoiceId, Integer orderId, Integer customerId) { }
