package vn.aeoc.crm.repository.chat;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import org.jooq.Record;
import vn.aeoc.crm.data.response.ChatView;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.Chat;
import vn.entity.backend.tables.ChatParticipant;
import vn.entity.backend.tables.User;
import vn.entity.backend.tables.records.ChatRecord;

import java.time.LocalDateTime;
import java.util.List;

import static org.jooq.impl.DSL.*;
import static vn.entity.backend.tables.Chat.CHAT;
import static vn.entity.backend.tables.ChatParticipant.CHAT_PARTICIPANT;
import static vn.entity.backend.tables.Message.MESSAGE;
import static vn.entity.backend.tables.MessageReadInfo.MESSAGE_READ_INFO;
import static vn.entity.backend.tables.User.USER;

@Repository
public class ChatRepository extends AbsMysqlRepository<ChatRecord, Chat, Integer> {

    public ChatRepository() {
        this.pojoClass = Chat.class;
        this.fieldID   = CHAT.ID;
    }

    @Override
    protected TableImpl<ChatRecord> getTable() {
        return CHAT;
    }

    private static final ChatParticipant CP_OTHER = CHAT_PARTICIPANT.as("cp_other");
    private static final User U_OTHER  = USER.as("u_other");

    // chats có id < beforeChatId, ORDER BY id DESC
    private ChatView mapChatView(Record r) {
        ChatView view = new ChatView();
        view.setId(r.get(CHAT.ID));
        view.setIsGroupChat(r.get(CHAT.IS_GROUP_CHAT, Boolean.class     ));
        view.setChatName(r.get("chatName", String.class));
        view.setChatAvatar(r.get("chatAvatar", String.class));
        view.setOtherUserId(r.get("otherUserId", Integer.class));
        view.setLastMessageId(r.get(CHAT.LAST_MESSAGE_ID));
        view.setLastMessageContent(r.get(CHAT.LAST_MESSAGE_CONTENT));
        view.setLastMessageSenderId(r.get(CHAT.LAST_MESSAGE_SENDER_ID));
        view.setLastMessageSenderName(r.get(CHAT.LAST_MESSAGE_SENDER_NAME));
        view.setLastMessageType(r.get(CHAT.LAST_MESSAGE_TYPE));
        view.setLastMessageAt(r.get(CHAT.LAST_MESSAGE_AT));
        view.setCreatedAt(r.get(CHAT.CREATED_AT));
        view.setUpdatedAt(r.get(CHAT.UPDATED_AT));
        view.setUnreadCount(r.get("unreadCount", Integer.class));
        return view;
    }

    public List<ChatView> getChatsByUserCursor(
            Integer userId,
            Integer beforeChatId,  // null = first page
            Integer     limit,
            Integer     filterBranchId
    ) {
        var CP_ME = CHAT_PARTICIPANT.as("cp_me");

        Field<Integer> lastReadMessageId = dslContext
                .select(max(MESSAGE_READ_INFO.MESSAGE_ID.cast(Integer.class)))
                .from(MESSAGE_READ_INFO)
                .where(MESSAGE_READ_INFO.USER_ID.cast(Integer.class).eq(userId))
                .and(MESSAGE_READ_INFO.READ_AT.isNotNull())
                .and(MESSAGE_READ_INFO.CHAT_ID.cast(Integer.class).eq(MESSAGE.CHAT_ID))
                .asField();

        var unread = dslContext
                .select(
                        MESSAGE.CHAT_ID,
                        count().as("cnt")
                )
                .from(MESSAGE)
                .where(MESSAGE.ID.gt(coalesce(lastReadMessageId, 0)))
                .and(MESSAGE.SENDER_ID.ne(userId))
                .and(MESSAGE.IS_DELETED.isFalse())
                .groupBy(MESSAGE.CHAT_ID)
                .asTable("unread");

        Condition pageCond = BranchFilter.branchEq(CHAT.BRANCH_ID, CommonUtils.NVL(filterBranchId));
        if (beforeChatId != null) {
            pageCond = pageCond.and(CHAT.ID.lt(beforeChatId));
        }

        return dslContext
                .select(
                        CHAT.ID,
                        CHAT.IS_GROUP_CHAT,
                        when(CHAT.IS_GROUP_CHAT.isTrue(), CHAT.GROUP_CHAT_NAME)
                                .otherwise(field(name("u_other", "name"), String.class))
                                .as("chatName"),
                        when(CHAT.IS_GROUP_CHAT.isTrue(), CHAT.GROUP_CHAT_IMAGE)
                                .otherwise(field(name("u_other", "avatar"), String.class))
                                .as("chatAvatar"),
                        when(CHAT.IS_GROUP_CHAT.isFalse(), field(name("u_other", "id"), Integer.class))
                                .otherwise(inline(null, Integer.class))
                                .as("otherUserId"),
                        CHAT.LAST_MESSAGE_ID,
                        CHAT.LAST_MESSAGE_CONTENT,
                        CHAT.LAST_MESSAGE_SENDER_ID,
                        CHAT.LAST_MESSAGE_SENDER_NAME,
                        CHAT.LAST_MESSAGE_TYPE,
                        CHAT.LAST_MESSAGE_AT,
                        CHAT.CREATED_AT,
                        CHAT.UPDATED_AT,
                        coalesce(field(name("unread", "cnt"), Integer.class), 0).as("unreadCount")
                )
                .from(CHAT)
                .join(CP_ME)
                .on(CP_ME.CHAT_ID.eq(CHAT.ID)
                        .and(CP_ME.USER_ID.eq(userId))
                        .and(CP_ME.IS_DELETED.isFalse()))
                .leftJoin(CP_OTHER)
                .on(CP_OTHER.CHAT_ID.eq(CHAT.ID)
                        .and(CP_OTHER.USER_ID.ne(userId))
                        .and(CHAT.IS_GROUP_CHAT.isFalse()))
                .leftJoin(U_OTHER)
                .on(U_OTHER.ID.eq(CP_OTHER.USER_ID))
                .leftJoin(unread)
                .on(field(name("unread", "chat_id"), Integer.class).eq(CHAT.ID))
                .where(pageCond)
                .orderBy(CHAT.ID.desc())
                .limit(limit)
                .fetch(this::mapChatView);
    }

    public List<ChatView> searchChats(
            Integer userId,
            String  keyword,
            Integer beforeChatId,  // null = first page
            int     limit,
            int     filterBranchId
    ) {
        var CP_ME = CHAT_PARTICIPANT.as("cp_me");

        String likeKeyword = "%" + keyword.trim() + "%";

        Condition searchCond =
                // group chat: tìm theo group name
                CHAT.IS_GROUP_CHAT.isTrue()
                        .and(CHAT.GROUP_CHAT_NAME.likeIgnoreCase(likeKeyword))
                        // private chat: tìm theo tên user kia
                        .or(CHAT.IS_GROUP_CHAT.isFalse()
                                .and(field(name("u_other", "name"), String.class)
                                        .likeIgnoreCase(likeKeyword)));

        var unread = dslContext
                .select(
                        MESSAGE.CHAT_ID.as("chat_id"),
                        count().as("cnt")
                )
                .from(MESSAGE)
                .where(MESSAGE.ID.gt(coalesce(
                        dslContext.select(max(MESSAGE_READ_INFO.MESSAGE_ID.cast(Integer.class)))
                                .from(MESSAGE_READ_INFO)
                                .where(MESSAGE_READ_INFO.USER_ID.cast(Integer.class).eq(userId))
                                .and(MESSAGE_READ_INFO.READ_AT.isNotNull())
                                .and(MESSAGE_READ_INFO.CHAT_ID.cast(Integer.class).eq(MESSAGE.CHAT_ID))
                                .asField(),
                        0
                )))
                .and(MESSAGE.SENDER_ID.ne(userId))
                .and(MESSAGE.IS_DELETED.isFalse())
                .groupBy(MESSAGE.CHAT_ID)
                .asTable("unread");

        Condition pageCond = BranchFilter.branchEq(CHAT.BRANCH_ID, CommonUtils.NVL(filterBranchId));
        if (beforeChatId != null) {
            pageCond = pageCond.and(CHAT.ID.lt(beforeChatId));
        }

        return dslContext
                .select(
                        CHAT.ID,
                        CHAT.IS_GROUP_CHAT,
                        when(CHAT.IS_GROUP_CHAT.isTrue(), CHAT.GROUP_CHAT_NAME)
                                .otherwise(field(name("u_other", "name"), String.class))
                                .as("chatName"),
                        when(CHAT.IS_GROUP_CHAT.isTrue(), CHAT.GROUP_CHAT_IMAGE)
                                .otherwise(field(name("u_other", "avatar"), String.class))
                                .as("chatAvatar"),
                        when(CHAT.IS_GROUP_CHAT.isFalse(), field(name("u_other", "id"), Integer.class))
                                .otherwise(inline(null, Integer.class))
                                .as("otherUserId"),
                        CHAT.LAST_MESSAGE_ID,
                        CHAT.LAST_MESSAGE_CONTENT,
                        CHAT.LAST_MESSAGE_SENDER_ID,
                        CHAT.LAST_MESSAGE_SENDER_NAME,
                        CHAT.LAST_MESSAGE_TYPE,
                        CHAT.LAST_MESSAGE_AT,
                        CHAT.CREATED_AT,
                        CHAT.UPDATED_AT,
                        coalesce(field(name("unread", "cnt"), Integer.class), 0).as("unreadCount")
                )
                .from(CHAT)
                .join(CP_ME)
                .on(CP_ME.CHAT_ID.eq(CHAT.ID)
                        .and(CP_ME.USER_ID.eq(userId))
                        .and(CP_ME.IS_DELETED.isFalse()))
                .leftJoin(CP_OTHER)
                .on(CP_OTHER.CHAT_ID.eq(CHAT.ID)
                        .and(CP_OTHER.USER_ID.ne(userId))
                        .and(CHAT.IS_GROUP_CHAT.isFalse()))
                .leftJoin(U_OTHER)
                .on(U_OTHER.ID.eq(CP_OTHER.USER_ID))
                .leftJoin(unread)
                .on(field(name("unread", "chat_id"), Integer.class).eq(CHAT.ID))
                .where(searchCond.and(pageCond))
                .orderBy(CHAT.ID.desc())
                .limit(limit)
                .fetch(this::mapChatView);
    }

    /**
     * Returns a paginated list of ChatView for a given user.
     * - group chat  → name / image come from CHAT table
     * - private chat → name / image come from the OTHER participant's USER row
     * Includes unread count per chat.
     */
    public List<ChatView> getChatsByUser(Integer userId, Pageable pageable, int filterBranchId) {

        // my participant row (join anchor)
        var CP_ME = CHAT_PARTICIPANT.as("cp_me");

        Field<Integer> lastReadMessageId = dslContext
                .select(max(MESSAGE_READ_INFO.MESSAGE_ID.cast(Integer.class)))
                .from(MESSAGE_READ_INFO)
                .where(MESSAGE_READ_INFO.USER_ID.cast(Integer.class).eq(userId))
                .and(MESSAGE_READ_INFO.READ_AT.isNotNull())
                .and(MESSAGE_READ_INFO.CHAT_ID.cast(Integer.class).eq(MESSAGE.CHAT_ID))
                .asField();

        var unread = dslContext
                .select(
                        MESSAGE.CHAT_ID,
                        count().as("cnt")
                )
                .from(MESSAGE)
                .where(MESSAGE.ID.gt(coalesce(lastReadMessageId, 0)))
                .and(MESSAGE.SENDER_ID.ne(userId))
                .and(MESSAGE.IS_DELETED.isFalse())
                .groupBy(MESSAGE.CHAT_ID)
                .asTable("unread");

        return dslContext
                .select(
                        CHAT.ID,
                        CHAT.IS_GROUP_CHAT,
                        // chat name: group → group_chat_name, private → other user name
                        when(CHAT.IS_GROUP_CHAT.isTrue(), CHAT.GROUP_CHAT_NAME)
                                .otherwise(field(name("u_other", "name"), String.class))
                                .as("chatName"),
                        // avatar
                        when(CHAT.IS_GROUP_CHAT.isTrue(), CHAT.GROUP_CHAT_IMAGE)
                                .otherwise(field(name("u_other", "avatar"), String.class))
                                .as("chatAvatar"),
                        CHAT.LAST_MESSAGE_ID,
                        CHAT.LAST_MESSAGE_CONTENT,
                        CHAT.LAST_MESSAGE_SENDER_ID,
                        CHAT.LAST_MESSAGE_SENDER_NAME,
                        CHAT.LAST_MESSAGE_TYPE,
                        CHAT.LAST_MESSAGE_AT,
                        CHAT.CREATED_AT,
                        CHAT.UPDATED_AT,
                        coalesce(field(name("unread", "cnt"), Integer.class), 0).as("unreadCount")
                )
                .from(CHAT)
                // my participant row
                .join(CP_ME)
                .on(CP_ME.CHAT_ID.eq(CHAT.ID)
                        .and(CP_ME.USER_ID.eq(userId))
                        .and(CP_ME.IS_DELETED.isFalse()))
                // other participant row (left join — absent for group chats is fine)
                .leftJoin(CP_OTHER)
                .on(CP_OTHER.CHAT_ID.eq(CHAT.ID)
                        .and(CP_OTHER.USER_ID.ne(userId))
                        .and(CHAT.IS_GROUP_CHAT.isFalse()))
                .leftJoin(U_OTHER)
                .on(U_OTHER.ID.eq(CP_OTHER.USER_ID))
                // unread counts
                .leftJoin(unread)
                .on(field(name("unread", "chat_id"), Integer.class).eq(CHAT.ID))
                .where(CP_ME.IS_DELETED.isFalse()
                        .and(BranchFilter.branchEq(CHAT.BRANCH_ID, CommonUtils.NVL(filterBranchId))))
                .orderBy(CHAT.LAST_MESSAGE_AT.desc().nullsLast())
                .limit(pageable.getLimit())
                .offset(pageable.getOffset())
                .fetch(this::mapChatView);
    }

    public Long countChatsByUser(Integer userId, int filterBranchId) {
        var CP_ME = CHAT_PARTICIPANT.as("cp_me");
        return dslContext
                .selectCount()
                .from(CHAT)
                .join(CP_ME).on(CP_ME.CHAT_ID.eq(CHAT.ID)
                        .and(CP_ME.USER_ID.eq(userId))
                        .and(CP_ME.IS_DELETED.isFalse()))
                .where(BranchFilter.branchEq(CHAT.BRANCH_ID, CommonUtils.NVL(filterBranchId)))
                .fetchOneInto(Long.class);
    }
    public Long countUnreadTotalByUser(Integer userId, int filterBranchId) {
        var CP_ME = CHAT_PARTICIPANT.as("cp_me");

        Field<Integer> lastReadMessageId = dslContext
                .select(max(MESSAGE_READ_INFO.MESSAGE_ID.cast(Integer.class)))
                .from(MESSAGE_READ_INFO)
                .where(MESSAGE_READ_INFO.USER_ID.cast(Integer.class).eq(userId))
                .and(MESSAGE_READ_INFO.READ_AT.isNotNull())
                .and(MESSAGE_READ_INFO.CHAT_ID.cast(Integer.class).eq(MESSAGE.CHAT_ID))
                .asField();

        var unread = dslContext
                .select(
                        MESSAGE.CHAT_ID.as("chat_id"),
                        count().as("cnt")
                )
                .from(MESSAGE)
                .where(MESSAGE.ID.gt(coalesce(lastReadMessageId, 0)))
                .and(MESSAGE.SENDER_ID.ne(userId))
                .and(MESSAGE.IS_DELETED.isFalse())
                .groupBy(MESSAGE.CHAT_ID)
                .asTable("unread");

        return dslContext
                .select(coalesce(sum(field(name("unread", "cnt"), Integer.class)), 0).cast(Long.class))
                .from(CHAT)
                .join(CP_ME).on(CP_ME.CHAT_ID.eq(CHAT.ID)
                        .and(CP_ME.USER_ID.eq(userId))
                        .and(CP_ME.IS_DELETED.isFalse()))
                .leftJoin(unread)
                .on(field(name("unread", "chat_id"), Integer.class).eq(CHAT.ID))
                .where(BranchFilter.branchEq(CHAT.BRANCH_ID, CommonUtils.NVL(filterBranchId)))
                .fetchOneInto(Long.class);
    }

    /**
     * Find an existing private chat between two users (returns null if none).
     */
    public Chat findPrivateChat(Integer userA, Integer userB, int filterBranchId) {
        var CP_A = CHAT_PARTICIPANT.as("cp_a");
        var CP_B = CHAT_PARTICIPANT.as("cp_b");

        Condition cond = CHAT.IS_GROUP_CHAT.isFalse()
                .and(BranchFilter.branchEq(CHAT.BRANCH_ID, CommonUtils.NVL(filterBranchId)));

        return dslContext
                .select(CHAT.fields())
                .from(CHAT)
                .join(CP_A).on(CP_A.CHAT_ID.eq(CHAT.ID).and(CP_A.USER_ID.eq(userA)))
                .join(CP_B).on(CP_B.CHAT_ID.eq(CHAT.ID).and(CP_B.USER_ID.eq(userB)))
                .where(cond)
                .limit(1)
                .fetchOneInto(Chat.class);
    }

    public Chat getById(Integer chatId) {
        return dslContext
                .select(CHAT.fields())
                .from(CHAT)
                .where(CHAT.ID.eq(chatId))
                .fetchOneInto(Chat.class);
    }

    /**
     * Update the last-message snapshot on a chat row (called after every message insert).
     */
    public void updateLastMessage(
            Integer chatId,
            Integer messageId,
            String  content,
            Integer senderId,
            String  senderName,
            Integer messageType
    ) {
        dslContext.update(CHAT)
                .set(CHAT.LAST_MESSAGE_ID,          messageId)
                .set(CHAT.LAST_MESSAGE_CONTENT,     content)
                .set(CHAT.LAST_MESSAGE_SENDER_ID,   senderId)
                .set(CHAT.LAST_MESSAGE_SENDER_NAME, senderName)
                .set(CHAT.LAST_MESSAGE_TYPE,        messageType)
                .set(CHAT.LAST_MESSAGE_AT, LocalDateTime.now())
                .where(CHAT.ID.eq(chatId))
                .execute();
    }
}