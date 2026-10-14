package vn.aeoc.crm.data.response;

import lombok.Getter;
import lombok.Setter;
import vn.aeoc.entity.data.mysql.product.AttributeValue;

import java.util.List;

@Getter
@Setter
public class ProductAttributeResponse {
    private Integer id;
    private String fieldName;
    private String name;
    private List<AttributeValue> options;
}
