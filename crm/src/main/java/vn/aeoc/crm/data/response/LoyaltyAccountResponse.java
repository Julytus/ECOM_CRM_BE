package vn.aeoc.crm.data.response;

public record LoyaltyAccountResponse(long balance, long rankPoints, Integer tierId, String tierName,
                                     Integer nextTierId, String nextTierName, Long nextTierMinRankPoints,
                                     long pointsToNextTier) { }
