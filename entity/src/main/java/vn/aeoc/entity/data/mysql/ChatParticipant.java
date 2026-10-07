package vn.aeoc.entity.data.mysql;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatParticipant {
    private Integer id;
    private Integer chatId;
    private Integer userId;
    private String displayName;
    private Integer chatRole;
    private LocalDateTime joinedAt;
    private LocalDateTime lastMessageAt;
    private Integer addedBy;
    private String addedByUserName;
    private Boolean isArchived;
    private LocalDateTime archivedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private Boolean isMuted;
    private LocalDateTime mutedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer branchId;
}