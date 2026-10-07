package vn.aeoc.entity.data.mysql;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerAttribute {
    private Integer id;
    private Integer isTitle;
    private String name;
    private String fieldName;
    private Integer required;
    private Integer readonly;
    private Integer uniqued;
    private String datatype; // number/text/listbox
    private String attributes; // json string
    private Integer position;

    private Integer parentId;
    private String parentName;

    private LocalDateTime createdAt;

    private Integer branchId;
}
