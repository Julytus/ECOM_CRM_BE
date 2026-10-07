package vn.aeoc.entity.data.mysql;

import lombok.Data;

@Data
public class Role {
    private Integer id;
    private String name;

    //Role default Khi tạo tài khoản mới
    private Integer isDefault;

    //Có cấp all Chức năng cho role không
    private Integer isOperator;
}
