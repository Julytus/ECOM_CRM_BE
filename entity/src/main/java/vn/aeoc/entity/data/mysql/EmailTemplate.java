package vn.aeoc.entity.data.mysql;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmailTemplate {

    private Long id;

    private String name;

    private String designJson;

    private Integer branchId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}