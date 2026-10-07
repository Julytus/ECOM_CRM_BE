package vn.aeoc.entity.data.mysql;

import lombok.Data;

@Data
public class CustomerVoucherUsage {

    private Integer id;
    private Integer voucherId;
    private Integer customerId;
    private Integer usedCount;
}
