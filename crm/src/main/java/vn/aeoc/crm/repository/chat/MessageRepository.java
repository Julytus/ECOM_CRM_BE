package vn.aeoc.crm.repository.chat;

import org.jooq.Condition;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.entity.data.mysql.chat.Message;
import vn.entity.backend.tables.records.MessageRecord;

import java.util.List;

import static vn.entity.backend.tables.Message.MESSAGE;

@Repository
public class MessageRepository
        extends AbsMysqlRepository<MessageRecord, Message, Integer> {

    public MessageRepository() {
        this.pojoClass = Message.class;
        this.fieldID   = MESSAGE.ID;
    }

    @Override
    protected TableImpl<MessageRecord> getTable() {
        return MESSAGE;
    }

    public List<Message> getByChatIdCursor(
            Integer chatId,
            Integer beforeMessageId,  // null = first page (newest messages)
            int     limit
    ) {
        Condition cond = MESSAGE.CHAT_ID.eq(chatId)
                .and(MESSAGE.IS_DELETED.isFalse());

        if (beforeMessageId != null) {
            cond = cond.and(MESSAGE.ID.lt(beforeMessageId));
        }

        return dslContext
                .select(getFiled())
                .from(MESSAGE)
                .where(cond)
                .orderBy(MESSAGE.ID.desc())
                .limit(limit)
                .fetchInto(Message.class);
    }

    // Tìm theo content trong 1 chat
    public List<Message> searchMessages(
            Integer chatId,
            String  keyword,
            Integer beforeMessageId,  // null = first page
            int     limit
    ) {
        Condition cond = MESSAGE.CHAT_ID.eq(chatId)
                .and(MESSAGE.IS_DELETED.isFalse())
                .and(MESSAGE.CONTENT.likeIgnoreCase("%" + keyword.trim() + "%"));

        if (beforeMessageId != null) {
            cond = cond.and(MESSAGE.ID.lt(beforeMessageId));
        }

        return dslContext
                .select(getFiled())
                .from(MESSAGE)
                .where(cond)
                .orderBy(MESSAGE.ID.desc())
                .limit(limit)
                .fetchInto(Message.class);
    }

    public List<Message> getByChatId(Integer chatId, Integer lastMessageId, Integer limit) {
        var condition = MESSAGE.CHAT_ID.eq(chatId).and(MESSAGE.IS_DELETED.isFalse());
        if (lastMessageId != null) {
            condition = condition.and(MESSAGE.ID.lt(lastMessageId));
        }
        return dslContext.select(getFiled())
                .from(MESSAGE)
                .where(condition)
                .orderBy(MESSAGE.CREATED_AT.desc())
                .limit(limit)
                .fetchInto(Message.class);
    }

    public List<Message> getByChatId(Integer chatId, Pageable pageable) {
        return dslContext
                .select(getFiled())
                .from(MESSAGE)
                .where(MESSAGE.CHAT_ID.eq(chatId)
                        .and(MESSAGE.IS_DELETED.isFalse()))
                .orderBy(MESSAGE.CREATED_AT.desc())
                .limit(pageable.getLimit())
                .offset(pageable.getOffset())
                .fetchInto(Message.class);
    }

    public Long countByChatId(Integer chatId) {
        return dslContext
                .selectCount()
                .from(MESSAGE)
                .where(MESSAGE.CHAT_ID.eq(chatId)
                        .and(MESSAGE.IS_DELETED.isFalse()))
                .fetchOneInto(Long.class);
    }

    public Integer softDelete(Integer id) {
        return dslContext.update(MESSAGE)
                .set(MESSAGE.IS_DELETED, (byte) 1)
                .where(MESSAGE.ID.eq(id))
                .execute();
    }

    public Integer editContent(Integer id, String newContent) {
        return dslContext.update(MESSAGE)
                .set(MESSAGE.CONTENT, newContent)
                .set(MESSAGE.IS_EDITED, (byte) 1)
                .where(MESSAGE.ID.eq(id))
                .execute();
    }
}
