package vn.aeoc.entity.data.mysql;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Message {
    private Integer id;
    private String content;
    private Integer senderId;
    private Integer chatId;
    private Integer messageType;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private Integer replyToMessageId;
    private String replyToMessageContent;
    private Integer replyToMessageType;
    private String replyToMessageFilePath;
    private Integer replyToMessageSenderId;
    private Boolean isReplyMessageEdited;
    private Boolean isReplyMessageDeleted;
    private Boolean isEdited;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer branchId;
}