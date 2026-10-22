package vn.aeoc.crm.data.request;

public record LoyaltyAdjustRequest(Integer customerId, Long points, Long rankDelta, String reason) { }
