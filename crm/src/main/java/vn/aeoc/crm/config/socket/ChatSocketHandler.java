package vn.aeoc.crm.config.socket;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.data.request.SendMessageRequest;
import vn.aeoc.crm.data.socket.*;
import vn.aeoc.crm.repository.chat.ChatParticipantRepository;
import vn.aeoc.crm.repository.chat.ChatRepository;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.chat.MessageService;
import vn.aeoc.entity.data.mysql.Chat;
import vn.aeoc.entity.data.mysql.ChatParticipant;
import vn.aeoc.entity.data.mysql.Message;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatSocketHandler {
    private final SocketIOServer            server;
    private final MessageService            messageService;
    private final UserService               userService;
    private final ChatRepository            chatRepository;
    private final ChatParticipantRepository participantRepo;

    // userId (String) → SocketIOClient: 1 connection per user
    private final Map<String, SocketIOClient> userClientMap = new ConcurrentHashMap<>();

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public ChatSocketHandler(
            SocketIOServer server,
            MessageService messageService,
            UserService userService,
            ChatRepository chatRepository,
            ChatParticipantRepository participantRepo
    ) {
        this.server          = server;
        this.messageService  = messageService;
        this.userService     = userService;
        this.chatRepository = chatRepository;
        this.participantRepo = participantRepo;

        registerListeners();
    }

    @PostConstruct
    public void start() {
        server.start();
        log.info("✅ Socket.IO server started on port {}", server.getConfiguration().getPort());
    }

    @PreDestroy
    public void stop() {
        server.stop();
        log.info("Socket.IO server stopped");
    }

    private void registerListeners() {
        server.addConnectListener(this::onConnect);
        server.addDisconnectListener(this::onDisconnect);

        server.addEventListener("join_room",    JoinRoomPayload.class,    this::onJoinRoom);
        server.addEventListener("leave_room",   LeaveRoomPayload.class,   this::onLeaveRoom);
        server.addEventListener("send_message", SendMessagePayload.class, this::onSendMessage);
        server.addEventListener("typing",       TypingPayload.class,      this::onTyping);
        server.addEventListener("mark_read",    MarkReadPayload.class,    this::onMarkRead);

        log.info("Socket.IO listeners registered");
    }

    /* ── Connection ─────────────────────────────────────────────────────────── */

    private void onConnect(SocketIOClient client) {
        String userId = userId(client);
        if (userId == null || userId.isBlank()) {
            log.warn("Rejected: no userId in query params");
            client.disconnect();
            return;
        }

        SocketIOClient old = userClientMap.put(userId, client);
        if (old != null && old.isChannelOpen()) {
            log.info("[presence] replacing old socket userId={} oldSession={} newSession={}", userId, old.getSessionId(), client.getSessionId());
            old.disconnect();
        }

        Integer uid = Integer.parseInt(userId);
        log.info("[presence] connect userId={} session={} mapSizeBeforeBroadcast={}", userId, client.getSessionId(), userClientMap.size());
        broadcastUserStatus(uid, "online");
        log.info("🔌 Connected userId={} session={}", userId, client.getSessionId());
    }

    private void onDisconnect(SocketIOClient client) {
        String userId = userId(client);
        if (userId == null) return;
        userClientMap.remove(userId, client);
        log.info("[presence] disconnect userId={} session={} mapSizeAfterRemove={}", userId, client.getSessionId(), userClientMap.size());
        broadcastUserStatus(Integer.parseInt(userId), "offline");
        log.info("❌ Disconnected userId={}", userId);
    }

    /* ── Room (typing indicator only) ──────────────────────────────────────── */

    private void onJoinRoom(SocketIOClient client, JoinRoomPayload data, AckRequest ack) {
        String r = room(data.getChatId());
        client.joinRoom(r);
        log.debug("userId={} joined {}", userId(client), r);
    }

    private void onLeaveRoom(SocketIOClient client, LeaveRoomPayload data, AckRequest ack) {
        client.leaveRoom(room(data.getChatId()));
    }

    /* ── Send message ───────────────────────────────────────────────────────── */

    private void onSendMessage(SocketIOClient client, SendMessagePayload data, AckRequest ack) {
        try {
            Integer senderId   = Integer.parseInt(userId(client));
            String  senderName = name(client);

            SendMessageRequest req = new SendMessageRequest();
            req.setChatId(data.getChatId());
            req.setContent(data.getContent());
            req.setMessageType(data.getMessageType() != null ? data.getMessageType() : 1);
            req.setReplyToMessageId(data.getReplyToMessageId());

            var login = userService.getUserById(senderId);
            Integer branchId = login != null && CommonUtils.NVL(login.getBranchId()) > 0
                    ? login.getBranchId()
                    : null;
            Message saved = messageService.send(senderId, senderName, req, null, branchId);
            pushToAllMembers(data.getChatId(), "message_received", toEvent(saved, senderName));

            ack(ack, Map.of("status", "ok", "messageId", saved.getId()));
        } catch (Exception e) {
            log.error("send_message error", e);
            ack(ack, Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /* ── Typing ─────────────────────────────────────────────────────────────── */

    private void onTyping(SocketIOClient client, TypingPayload data, AckRequest ack) {
        TypingEvent ev = new TypingEvent();
        ev.setChatId(data.getChatId());
        ev.setUserId(Integer.parseInt(userId(client)));
        ev.setUserName(name(client));
        ev.setTyping(data.isTyping());

        server.getRoomOperations(room(data.getChatId()))
                .getClients().stream()
                .filter(c -> !c.getSessionId().equals(client.getSessionId()))
                .forEach(c -> c.sendEvent("typing", ev));
    }

    /* ── Mark read ──────────────────────────────────────────────────────────── */

    private void onMarkRead(SocketIOClient client, MarkReadPayload data, AckRequest ack) {
        Integer uid = Integer.parseInt(userId(client));

        Chat chat = chatRepository.getById(data.getChatId());
        if (chat == null) return;

        Integer lastMessageId = chat.getLastMessageId();
        if (lastMessageId != null) {
            messageService.markRead(lastMessageId, data.getChatId(), uid);
        }

        MessageReadEvent ev = new MessageReadEvent();
        ev.setChatId(data.getChatId());
        ev.setMessageId(lastMessageId);
        ev.setUserId(uid);
        pushToAllMembers(data.getChatId(), "message_read", ev);
    }

    /* ── Public: gọi từ MessageService sau REST file upload ─────────────────── */

    public void emitNewMessage(Integer chatId, Message msg, String senderName) {
        pushToAllMembers(chatId, "message_received", toEvent(msg, senderName));
    }

    public void emitPaymentDone(Integer userId, Integer invoiceId) {
        SocketIOClient client = userClientMap.get(String.valueOf(userId));
        if (client != null && client.isChannelOpen()) {
            client.sendEvent("payment_done", Map.of("invoiceId", invoiceId));
            log.info("[Socket] payment_done → userId={} invoiceId={}", userId, invoiceId);
        } else {
            log.warn("[Socket] payment_done: userId={} not connected", userId);
        }
    }

    public void emitNotification(Integer userId, Object payload) {
        SocketIOClient client = userClientMap.get(String.valueOf(userId));
        if (client != null && client.isChannelOpen()) {
            client.sendEvent("notification_received", payload);
            log.info("[Socket] notification_received → userId={}", userId);
        } else {
            log.warn("[Socket] notification_received: userId={} not connected", userId);
        }
    }

    /* ── Internals ──────────────────────────────────────────────────────────── */

    private void pushToAllMembers(Integer chatId, String event, Object data) {
        List<ChatParticipant> members = participantRepo.getByChatId(chatId);
        members.forEach(m -> {
            SocketIOClient target = userClientMap.get(String.valueOf(m.getUserId()));
            if (target != null && target.isChannelOpen())
                target.sendEvent(event, data);
        });
    }

    private String room(Integer chatId) { return "chat_" + chatId; }

    public boolean isUserOnline(Integer userId) {
        return userId != null && userClientMap.containsKey(String.valueOf(userId));
    }

    private void broadcastUserStatus(Integer userId, String status) {
        UserStatusEvent event = new UserStatusEvent(userId, status);
        log.info("[presence] broadcast user_status userId={} status={} targets={}", userId, status, userClientMap.size());
        server.getBroadcastOperations().sendEvent("user_status", event);
    }

    private String userId(SocketIOClient c) {
        return c.getHandshakeData().getSingleUrlParam("userId");
    }

    private String name(SocketIOClient c) {
        String n = c.getHandshakeData().getSingleUrlParam("userName");
        return (n != null && !n.isBlank()) ? n : "Unknown";
    }

    private void ack(AckRequest ack, Object data) {
        if (ack.isAckRequested()) ack.sendAckData(data);
    }

    private MessageReceivedEvent toEvent(Message m, String senderName) {
        MessageReceivedEvent e = new MessageReceivedEvent();
        e.setId(m.getId());
        e.setChatId(m.getChatId());
        Chat chat = chatRepository.getById(m.getChatId());
        e.setChatName(chat != null ? chat.getChatName() : null);
        e.setSenderId(m.getSenderId());
        e.setSenderName(senderName);
        e.setContent(m.getContent());
        e.setMessageType(m.getMessageType());
        e.setFilePath(m.getFilePath());
        e.setFileName(m.getFileName());
        e.setFileSize(m.getFileSize());
        e.setReplyToMessageId(m.getReplyToMessageId());
        e.setReplyToMessageContent(m.getReplyToMessageContent());
        e.setReplyToMessageSenderId(m.getReplyToMessageSenderId());
        e.setReplyToMessageType(m.getReplyToMessageType());
        e.setReplyToMessageFilePath(m.getReplyToMessageFilePath());
        e.setIsEdited(m.getIsEdited());
        e.setIsDeleted(m.getIsDeleted());
        e.setCreatedAt(m.getCreatedAt() != null ? m.getCreatedAt().format(FMT) : null);
        return e;
    }
}