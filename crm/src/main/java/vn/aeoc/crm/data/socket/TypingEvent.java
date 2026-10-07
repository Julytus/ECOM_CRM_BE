package vn.aeoc.crm.data.socket;

import lombok.Data;

@Data
public class TypingEvent {
    private Integer chatId;
    private Integer userId;
    private String  userName;
    private boolean typing;
}