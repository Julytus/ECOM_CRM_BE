package vn.aeoc.crm.data.pojo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Purchase {
    private String name;
    private BigDecimal value;
}
