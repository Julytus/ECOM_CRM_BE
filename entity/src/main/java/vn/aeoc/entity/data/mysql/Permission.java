package vn.aeoc.entity.data.mysql;

import lombok.Data;

@Data
public class Permission {
    private Integer id;
    private Integer resourceId;
    private Integer roleId;
    private String actions;
}
