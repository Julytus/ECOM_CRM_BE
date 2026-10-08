package vn.aeoc.crm.repository.chat;


import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.entity.data.mysql.chat.ChatParticipant;
import vn.entity.backend.tables.records.ChatParticipantRecord;

import java.time.LocalDateTime;
import java.util.List;

import static vn.entity.backend.tables.ChatParticipant.CHAT_PARTICIPANT;

@Repository
public class ChatParticipantRepository
        extends AbsMysqlRepository<ChatParticipantRecord, ChatParticipant, Integer> {

    public ChatParticipantRepository() {
        this.pojoClass = ChatParticipant.class;
        this.fieldID   = CHAT_PARTICIPANT.ID;
    }

    @Override
    protected TableImpl<ChatParticipantRecord> getTable() {
        return CHAT_PARTICIPANT;
    }

    public List<ChatParticipant> getByChatId(Integer chatId) {
        return dslContext
                .select(getFiled())
                .from(CHAT_PARTICIPANT)
                .where(CHAT_PARTICIPANT.CHAT_ID.eq(chatId)
                        .and(CHAT_PARTICIPANT.IS_DELETED.isFalse()))
                .fetchInto(ChatParticipant.class);
    }

    public ChatParticipant getByUserAndChat(Integer userId, Integer chatId) {
        return dslContext
                .select(getFiled())
                .from(CHAT_PARTICIPANT)
                .where(CHAT_PARTICIPANT.USER_ID.eq(userId)
                        .and(CHAT_PARTICIPANT.CHAT_ID.eq(chatId)))
                .fetchOneInto(ChatParticipant.class);
    }

    public boolean isMember(Integer userId, Integer chatId) {
        return dslContext.fetchExists(
                dslContext.selectOne()
                        .from(CHAT_PARTICIPANT)
                        .where(CHAT_PARTICIPANT.USER_ID.eq(userId)
                                .and(CHAT_PARTICIPANT.CHAT_ID.eq(chatId))
                                .and(CHAT_PARTICIPANT.IS_DELETED.isFalse()))
        );
    }

    public Integer softDelete(Integer userId, Integer chatId) {
        return dslContext.update(CHAT_PARTICIPANT)
                .set(CHAT_PARTICIPANT.IS_DELETED, (byte) 1)
                .set(CHAT_PARTICIPANT.DELETED_AT, LocalDateTime.now())
                .where(CHAT_PARTICIPANT.USER_ID.eq(userId)
                        .and(CHAT_PARTICIPANT.CHAT_ID.eq(chatId)))
                .execute();
    }
}
