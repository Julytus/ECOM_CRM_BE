package vn.aeoc.entity.data.mysql;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageReadInfo {
    private Integer id;
    private String messageId;
    private String chatId;
    private String userId;
    private LocalDateTime readAt;
}