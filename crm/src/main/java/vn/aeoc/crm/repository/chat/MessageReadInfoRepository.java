package vn.aeoc.crm.repository.chat;

import org.jooq.DSLContext;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.entity.data.mysql.chat.MessageReadInfo;
import vn.entity.backend.tables.records.MessageReadInfoRecord;

import java.time.LocalDateTime;
import java.util.List;

import static vn.entity.backend.tables.MessageReadInfo.MESSAGE_READ_INFO;

@Repository
public class MessageReadInfoRepository
        extends AbsMysqlRepository<MessageReadInfoRecord, MessageReadInfo, Integer> {
    public MessageReadInfoRepository(DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    protected TableImpl<MessageReadInfoRecord> getTable() {
        return MESSAGE_READ_INFO;
    }

    /**
     * Mark a single message as read for a user.
     * Uses INSERT … ON DUPLICATE KEY UPDATE to be idempotent.
     */
    public void markRead(Integer messageId, Integer chatId, Integer userId) {
        dslContext.insertInto(MESSAGE_READ_INFO)
                .set(MESSAGE_READ_INFO.MESSAGE_ID, messageId)
                .set(MESSAGE_READ_INFO.CHAT_ID, chatId)
                .set(MESSAGE_READ_INFO.USER_ID, userId)
                .set(MESSAGE_READ_INFO.READ_AT, LocalDateTime.now())
                .onDuplicateKeyIgnore()
                .execute();
    }

    /**
     * Mark ALL unread messages in a chat as read for a user.
     */
    public void markAllRead(Integer chatId, Integer userId) {
        dslContext.update(MESSAGE_READ_INFO)
                .set(MESSAGE_READ_INFO.READ_AT, LocalDateTime.now())
                .where(MESSAGE_READ_INFO.CHAT_ID.eq(chatId)
                        .and(MESSAGE_READ_INFO.USER_ID.eq(userId))
                        .and(MESSAGE_READ_INFO.READ_AT.isNull()))
                .execute();
    }

    public List<MessageReadInfo> getReadersOfMessage(Integer messageId) {
        return dslContext
                .select(getFiled())
                .from(MESSAGE_READ_INFO)
                .where(MESSAGE_READ_INFO.MESSAGE_ID.eq(messageId)
                        .and(MESSAGE_READ_INFO.READ_AT.isNotNull()))
                .fetchInto(MessageReadInfo.class);
    }

    public long countUnread(Integer chatId, Integer userId) {
        return dslContext
                .selectCount()
                .from(MESSAGE_READ_INFO)
                .where(MESSAGE_READ_INFO.CHAT_ID.eq(chatId)
                        .and(MESSAGE_READ_INFO.USER_ID.eq(userId))
                        .and(MESSAGE_READ_INFO.READ_AT.isNull()))
                .fetchOneInto(Long.class);
    }
}