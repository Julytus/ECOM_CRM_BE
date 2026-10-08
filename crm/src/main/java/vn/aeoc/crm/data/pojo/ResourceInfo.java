package vn.aeoc.crm.data.pojo;

import lombok.Data;
import vn.aeoc.entity.data.mysql.auth.Resource;


/**
 * Mỗi tài nguyên chứa một danh sách các quyền được phép
 */
@Data
public class ResourceInfo extends Resource {
    //Chứa roleId và các actions
    private PermissionInfo permission;

    public ResourceInfo() {
        super();
    }

    public ResourceInfo(Resource p) {
        super(p);
    }
}
