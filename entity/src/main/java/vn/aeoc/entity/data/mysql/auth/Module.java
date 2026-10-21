package vn.aeoc.entity.data.mysql.auth;

import lombok.Data;

@Data
public class Module {
    private Integer id;
    private String name;
    private String code;
    private String description;
    private Integer active;
}
