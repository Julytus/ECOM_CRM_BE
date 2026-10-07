package vn.aeoc.crm.service.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.aeoc.base.config.extention.paging.Cursor;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.config.socket.ChatSocketHandler;
import vn.aeoc.crm.data.request.SendMessageRequest;
import vn.aeoc.crm.repository.chat.ChatParticipantRepository;
import vn.aeoc.crm.repository.chat.ChatRepository;
import vn.aeoc.crm.repository.chat.MessageReadInfoRepository;
import vn.aeoc.crm.repository.chat.MessageRepository;
import vn.aeoc.crm.service.other.FileStorageService;
import vn.aeoc.entity.data.mysql.Message;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService extends BaseService<MessageRepository, Message, Integer> {

    private final ChatRepository chatRepo;
    private final ChatParticipantRepository participantRepo;
    private final MessageReadInfoRepository readInfoRepo;
    private final FileStorageService fileStorageService;

    @Lazy
    @Autowired(required = false)
    private ChatSocketHandler socketHandler;

    private static final String UPLOAD_DIR = "uploads/chat/";

    public MessageService(MessageRepository repository,
                          ChatRepository chatRepo,
                          ChatParticipantRepository participantRepo,
                          MessageReadInfoRepository readInfoRepo,
                          FileStorageService fileStorageService) {
        super(repository);
        this.chatRepo = chatRepo;
        this.participantRepo = participantRepo;
        this.readInfoRepo = readInfoRepo;
        this.fileStorageService = fileStorageService;
    }

    // Cursor list
    public Cursor<Message> getMessagesCursor(
            Integer chatId,
            Integer userId,
            Integer beforeMessageId,
            int     limit
    ) {
        assertMember(userId, chatId);
        List<Message> rows = repository.getByChatIdCursor(chatId, beforeMessageId, limit + 1);
        return new Cursor<>(rows, limit, Message::getId);
    }

    // Search
    public Cursor<Message> searchMessages(
            Integer chatId,
            Integer userId,
            String  keyword,
            Integer beforeMessageId,
            int     limit
    ) {
        assertMember(userId, chatId);
        List<Message> rows = repository.searchMessages(chatId, keyword, beforeMessageId, limit + 1);
        return new Cursor<>(rows, limit, Message::getId);
    }

    public Page<Message> getMessages(Integer chatId, Integer userId, Pageable pageable) {
        assertMember(userId, chatId);
        return pageQuery(
                pageable,
                () -> repository.getByChatId(chatId, pageable),
                () -> repository.countByChatId(chatId)
        );
    }

    @Transactional
    public Message send(Integer senderId, String senderName, SendMessageRequest req, MultipartFile file,
                        Integer branchIdFromLogin) {

        assertMember(senderId, req.getChatId());

        Message msg = new Message();
        msg.setChatId(req.getChatId());
        if (CommonUtils.NVL(branchIdFromLogin) > 0) {
            msg.setBranchId(branchIdFromLogin);
        }
        msg.setSenderId(senderId);
        msg.setMessageType(req.getMessageType() != null ? req.getMessageType() : 1);
        msg.setIsEdited(false);
        msg.setIsDeleted(false);
        msg.setCreatedAt(LocalDateTime.now());
        msg.setUpdatedAt(LocalDateTime.now());

        if (file != null && !file.isEmpty()) {
            String fileUrl = fileStorageService.upload(file);
            msg.setFilePath(fileUrl);
            msg.setFileName(file.getOriginalFilename());
            msg.setFileSize(file.getSize());
            // If caller didn't specify type, treat as DOCUMENT
            if (req.getMessageType() == null || req.getMessageType() == 1) {
                msg.setMessageType(6);
            }
        } else {
            msg.setContent(req.getContent());
        }

        if (req.getReplyToMessageId() != null) {
            msg.setReplyToMessageId(req.getReplyToMessageId());
            Message parent = repository.getById(req.getReplyToMessageId());
            if (parent != null) {
                msg.setReplyToMessageContent(parent.getContent());
                msg.setReplyToMessageType(parent.getMessageType());
                msg.setReplyToMessageFilePath(parent.getFilePath());
                msg.setReplyToMessageSenderId(parent.getSenderId());
                msg.setIsReplyMessageEdited(parent.getIsEdited());
                msg.setIsReplyMessageDeleted(parent.getIsDeleted());
            }
        }

        Message saved = repository.insertReturning(msg);

        // ── Update chat's last-message snapshot ──────────────────────────────
        chatRepo.updateLastMessage(
                req.getChatId(),
                saved.getId(),
                saved.getContent() != null ? saved.getContent() : saved.getFileName(),
                senderId,
                senderName,
                saved.getMessageType()
        );

        if (socketHandler != null) {
            socketHandler.emitNewMessage(req.getChatId(), saved, senderName);
        }

        return saved;
    }

    @Transactional
    public Message editMessage(Integer messageId, Integer userId, String newContent) {
        Message msg = repository.getById(messageId);
        assertOwner(msg, userId);
        repository.editContent(messageId, newContent);
        return repository.getById(messageId);
    }

    @Transactional
    public Integer deleteMessage(Integer messageId, Integer userId) {
        Message msg = repository.getById(messageId);
        assertOwner(msg, userId);
        return repository.softDelete(messageId);
    }

    // ── Read receipts ────────────────────────────────────────────────────────

    public void markRead(Integer messageId, Integer chatId, Integer userId) {
        readInfoRepo.markRead(messageId, chatId, userId);
    }

    public void markAllRead(Integer chatId, Integer userId) {
        readInfoRepo.markAllRead(chatId, userId);
    }

    // ── Guards ───────────────────────────────────────────────────────────────

    private void assertMember(Integer userId, Integer chatId) {
        if (!participantRepo.isMember(userId, chatId)) {
            throw new RuntimeException("User " + userId + " is not a member of chat " + chatId);
        }
    }

    private void assertOwner(Message msg, Integer userId) {
        if (msg == null || !msg.getSenderId().equals(userId)) {
            throw new RuntimeException("Permission denied");
        }
    }
}