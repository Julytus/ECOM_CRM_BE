package vn.aeoc.crm.data.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatView {
    private Integer id;
    private String chatName;          // group name || other user's name
    private String chatAvatar;        // group image || other user's avatar
    private Boolean isGroupChat;

    // last message snapshot
    private Integer lastMessageId;
    private String  lastMessageContent;
    private Integer lastMessageSenderId;
    private String  lastMessageSenderName;
    private Integer lastMessageType;
    private LocalDateTime lastMessageAt;

    private Integer unreadCount;
    private Integer otherUserId;
    private Boolean isOnline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}