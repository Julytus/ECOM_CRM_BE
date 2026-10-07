package vn.aeoc.crm.data.socket;

import lombok.Data;

@Data
public class MessageReadEvent {
    private Integer chatId;
    private Integer messageId;          // null = all
    private Integer userId;
}