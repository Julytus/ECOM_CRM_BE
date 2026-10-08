package vn.aeoc.crm.service.chat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aeoc.base.config.extention.paging.Cursor;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.data.request.CreateChatRequest;
import vn.aeoc.crm.data.response.ChatSidebarSummary;
import vn.aeoc.crm.data.response.ChatView;
import vn.aeoc.crm.config.socket.ChatSocketHandler;
import vn.aeoc.crm.repository.auth.UserRepository;
import vn.aeoc.crm.repository.chat.ChatParticipantRepository;
import vn.aeoc.crm.repository.chat.ChatRepository;
import vn.aeoc.crm.repository.chat.MessageReadInfoRepository;
import vn.aeoc.entity.data.mysql.chat.Chat;
import vn.aeoc.entity.data.mysql.chat.ChatParticipant;
import vn.aeoc.entity.data.mysql.auth.User;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService extends BaseService<ChatRepository, Chat, Integer> {

    private final ChatParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final MessageReadInfoRepository readInfoRepository;
    private final ChatSocketHandler chatSocketHandler;

    public ChatService(ChatRepository repository,
                       ChatParticipantRepository participantRepository,
                       UserRepository userRepository,
                       MessageReadInfoRepository readInfoRepository,
                       ChatSocketHandler chatSocketHandler) {
        super(repository);
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.readInfoRepository = readInfoRepository;
        this.chatSocketHandler = chatSocketHandler;
    }

    public Cursor<ChatView> getChatsByUserCursor(
            Integer userId,
            Integer beforeChatId,
            Integer limit,
            Integer filterBranchId
    ) {
        List<ChatView> rows = enrichPresence(repository.getChatsByUserCursor(userId, beforeChatId, limit + 1, filterBranchId));
        return new Cursor<>(rows, limit, ChatView::getId);
    }

    public Cursor<ChatView> searchChats(
            Integer userId,
            String  keyword,
            Integer beforeChatId,
            Integer limit,
            Integer filterBranchId
    ) {
        List<ChatView> rows = enrichPresence(repository.searchChats(userId, keyword, beforeChatId, limit + 1, filterBranchId));
        return new Cursor<>(rows, limit, ChatView::getId);
    }

    public Page<ChatView> getChatsByUser(Integer userId, Pageable pageable, int filterBranchId) {
        return pageQueryView(
                pageable,
                () -> enrichPresence(repository.getChatsByUser(userId, pageable, filterBranchId)),
                () -> repository.countChatsByUser(userId, filterBranchId)
        );
    }

    public ChatSidebarSummary getSidebarSummary(Integer userId, int filterBranchId) {
        ChatSidebarSummary summary = new ChatSidebarSummary();
        summary.setUnreadTotal(repository.countUnreadTotalByUser(userId, filterBranchId).intValue());
        return summary;
    }

    @Transactional
    public ChatView createChat(Integer creatorId, String creatorName, CreateChatRequest req, Integer branchIdFromLogin) {

        boolean isGroup = Boolean.TRUE.equals(req.getIsGroupChat());
        int privateChatBranchScope = CommonUtils.NVL(branchIdFromLogin) > 0 ? branchIdFromLogin : 0;

        // For private chat, reuse existing conversation if present
        if (!isGroup && req.getMemberIds().size() == 1) {
            Chat existing = repository.findPrivateChat(
                    creatorId, req.getMemberIds().getFirst(), privateChatBranchScope);
            if (existing != null) return toChatView(existing, creatorId);
        }

        // Build chat entity
        Chat chat = new Chat();
        chat.setIsGroupChat(isGroup);
        chat.setGroupChatName(isGroup ? req.getGroupChatName() : null);
        chat.setGroupChatImage(isGroup ? req.getGroupChatImage() : null);
        chat.setCreatedBy(creatorId);
        chat.setCreatedByUserName(creatorName);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());
        if (CommonUtils.NVL(branchIdFromLogin) > 0) {
            chat.setBranchId(branchIdFromLogin);
        }
        Chat saved = repository.insertReturning(chat);
        Integer chatBranchId = saved.getBranchId();

        // Add creator
        addParticipant(saved.getId(), creatorId, creatorName, 2 /* ADMIN */, creatorId, creatorName, chatBranchId);

        // Add other members
        for (Integer memberId : req.getMemberIds()) {
            addParticipant(saved.getId(), memberId, null, 1 /* MEMBER */, creatorId, creatorName, chatBranchId);
        }

        return toChatView(saved, creatorId);
    }

    public List<ChatParticipant> getParticipants(Integer chatId) {
        return participantRepository.getByChatId(chatId);
    }

    @Transactional
    public ChatParticipant addMember(Integer chatId, Integer userId,
                                     String displayName, Integer addedBy, String addedByName) {
        if (participantRepository.isMember(userId, chatId)) {
            return participantRepository.getByUserAndChat(userId, chatId);
        }
        Chat chat = repository.getById(chatId);
        Integer chatBranchId = chat != null ? chat.getBranchId() : null;
        return addParticipant(chatId, userId, displayName, 0, addedBy, addedByName, chatBranchId);
    }

    public Integer leaveChat(Integer chatId, Integer userId) {
        return participantRepository.softDelete(userId, chatId);
    }

    private ChatParticipant addParticipant(Integer chatId, Integer userId,
                                           String displayName, Integer role,
                                           Integer addedBy, String addedByName,
                                           Integer chatBranchId) {
        ChatParticipant cp = new ChatParticipant();
        cp.setChatId(chatId);
        cp.setUserId(userId);
        cp.setDisplayName(displayName);
        cp.setChatRole(role);
        cp.setJoinedAt(LocalDateTime.now());
        cp.setAddedBy(addedBy);
        cp.setAddedByUserName(addedByName);
        cp.setIsArchived(false);
        cp.setIsDeleted(false);
        cp.setIsMuted(false);
        cp.setCreatedAt(LocalDateTime.now());
        cp.setUpdatedAt(LocalDateTime.now());
        cp.setBranchId(chatBranchId);
        return participantRepository.insertReturning(cp);
    }

    /**
     * Helper: parallel page query for ChatView (not the generic POJO type).
     */
    private Page<ChatView> pageQueryView(Pageable pageable,
                                         java.util.function.Supplier<List<ChatView>> list,
                                         java.util.function.Supplier<Long> count) {
        var result = queryExecutor.parallel2(list, count);
        pageable.setTotal(result.v2());
        return new Page<>(pageable, result.v1());
    }

    public ChatView toChatView(Chat chat, Integer userId) {
        if (chat == null) return null;

        ChatView view = new ChatView();
        view.setId(chat.getId());
        view.setIsGroupChat(chat.getIsGroupChat());
        view.setLastMessageId(chat.getLastMessageId());
        view.setLastMessageContent(chat.getLastMessageContent());
        view.setLastMessageSenderId(chat.getLastMessageSenderId());
        view.setLastMessageSenderName(chat.getLastMessageSenderName());
        view.setLastMessageType(chat.getLastMessageType());
        view.setLastMessageAt(chat.getLastMessageAt());
        view.setCreatedAt(chat.getCreatedAt());
        view.setUpdatedAt(chat.getUpdatedAt());

        if (Boolean.TRUE.equals(chat.getIsGroupChat())) {
            view.setChatName(chat.getGroupChatName());
            view.setChatAvatar(chat.getGroupChatImage());
        } else {
            List<ChatParticipant> participants = participantRepository.getByChatId(chat.getId());
            ChatParticipant other = participants.stream()
                    .filter(p -> !p.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);
            if (other != null) {
                User otherUser = userRepository.getUserById(other.getUserId());
                if (otherUser != null) {
                    view.setChatName(otherUser.getName());
                    view.setChatAvatar(otherUser.getAvatar());
                    view.setOtherUserId(otherUser.getId());
                    view.setIsOnline(chatSocketHandler.isUserOnline(otherUser.getId()));
                }
            }
        }

        return view;
    }

    private List<ChatView> enrichPresence(List<ChatView> rows) {
        if (rows == null) return null;
        for (ChatView view : rows) {
            if (view == null || Boolean.TRUE.equals(view.getIsGroupChat())) continue;
            if (view.getOtherUserId() == null) {
                List<ChatParticipant> participants = participantRepository.getByChatId(view.getId());
                ChatParticipant other = participants.stream()
                        .filter(p -> !p.getUserId().equals(view.getLastMessageSenderId()))
                        .findFirst()
                        .orElse(null);
                if (other != null) {
                    User otherUser = userRepository.getUserById(other.getUserId());
                    if (otherUser != null) {
                        view.setOtherUserId(otherUser.getId());
                        if (view.getChatName() == null) view.setChatName(otherUser.getName());
                        if (view.getChatAvatar() == null) view.setChatAvatar(otherUser.getAvatar());
                    }
                }
            }
            if (view.getOtherUserId() != null) {
                view.setIsOnline(chatSocketHandler.isUserOnline(view.getOtherUserId()));
            }
        }
        return rows;
    }
}