package vn.aeoc.entity.data.mysql;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Chat {
    private Integer id;
    private String groupChatName;
    private String chatName;
    private String groupChatImage;
    private Boolean isGroupChat;
    private Integer lastMessageId;
    private String lastMessageContent;
    private Integer lastMessageSenderId;
    private String lastMessageSenderName;
    private Integer lastMessageType;
    private LocalDateTime lastMessageAt;
    private Integer createdBy;
    private String createdByUserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer branchId;
}