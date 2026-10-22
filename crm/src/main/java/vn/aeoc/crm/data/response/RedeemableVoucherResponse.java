package vn.aeoc.crm.data.response;

import java.time.LocalDateTime;

public record RedeemableVoucherResponse(Integer id, String code, String name, Long pointsCost,
                                        Integer redeemStock, Integer redeemedCount, Integer minTierId,
                                        boolean affordable, boolean tierOk, LocalDateTime endDate) { }
