package vn.aeoc.crm.data.request;

import java.math.BigDecimal;

public record LoyaltyTierRequest(Integer id, String name, Long minRankPoints,
                                 BigDecimal earnMultiplier, Integer status) { }
