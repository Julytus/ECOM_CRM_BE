package vn.aeoc.crm.event;

public record OrderCompletedEvent(Integer orderId, Integer customerId, double baseAmount) { }
