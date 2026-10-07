package vn.aeoc.crm.data.socket;

import lombok.Data;

@Data
public class MessageReceivedEvent {
    private Integer id;
    private Integer chatId;
    private String chatName;
    private Integer senderId;
    private String senderName;
    private String content;
    private Integer messageType;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private Integer replyToMessageId;
    private String replyToMessageContent;
    private Integer replyToMessageType;
    private String replyToMessageFilePath;
    private Integer replyToMessageSenderId;
    private Boolean isEdited;
    private Boolean isDeleted;
    private String createdAt;
}
