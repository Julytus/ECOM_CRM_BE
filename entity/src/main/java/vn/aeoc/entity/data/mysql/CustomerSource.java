package vn.aeoc.entity.data.mysql;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class CustomerSource {

    private Integer id;
    private String name;
    private Integer status;
    private Timestamp createdTime;

    private Integer branchId;
}
