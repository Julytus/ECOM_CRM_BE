package vn.aeoc.entity.data.mysql.sale;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Unit implements Serializable {

    private Integer id;

    private String name;

    /** 1 - đang sử dụng, 0 - ngưng sử dụng */
    private Integer status;

    private Integer position;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;

    private Integer branchId;
}
