package vn.aeoc.crm.data.pojo;

import lombok.Data;

/**
 * Thông tin tổng hợp để phân quyền
 */
@Data
public class PermissionInfo {
    private Integer roleId;
    private String actions;
}
