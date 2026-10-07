package vn.aeoc.entity.data.mysql;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class Notification {

    private Integer id;

    private Integer userId;

    private Integer branchId;

    private Integer type;

    private String title;

    private String image;

    private String content;

    private String refType;

    private Integer refId;

    private Integer isRead;

    private Timestamp createdTime;
}