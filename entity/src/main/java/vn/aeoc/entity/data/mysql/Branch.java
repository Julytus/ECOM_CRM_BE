package vn.aeoc.entity.data.mysql;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDate;

@Data
public class Branch {
    private Integer id;
    private Integer parentId;
    private String avatar;
    private String name;
    private String address;
    private String website;
    private String description;
    private LocalDate foundingDay;
    private String phone;
    private String email;
    private Integer ownerId;
    private Timestamp createdTime;
    private Integer status;
}
