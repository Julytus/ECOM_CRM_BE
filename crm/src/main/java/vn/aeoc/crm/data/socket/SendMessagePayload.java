package vn.aeoc.crm.data.socket;

import lombok.Data;

@Data
public class SendMessagePayload {
    private Integer chatId;
    private String  content;
    private Integer messageType;        // 1 = TEXT (file dùng REST /message/send)
    private Integer replyToMessageId;
}