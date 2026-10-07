package vn.aeoc.crm.data.request;

import lombok.Data;

@Data
public class SendMessageRequest {
    private Integer chatId;
    private String  content;
    private Integer messageType;      // 1=TEXT, 2=IMAGE, 3=VIDEO, 4=AUDIO, 5=GIF, 6=DOCUMENT, 7=SYSTEM
    private Integer replyToMessageId;
}