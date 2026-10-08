package vn.aeoc.entity.data.mysql.customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerExtraInfo {
    private Integer id;

    private Integer attributeId;
    private String attributeName;

    private Integer customerId;
    private String attributeValue;
    private String datatype;
    private String fieldName;
}