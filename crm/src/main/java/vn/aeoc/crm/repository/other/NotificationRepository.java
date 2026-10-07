package vn.aeoc.crm.repository.other;

import org.jooq.Condition;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.Notification;
import vn.entity.backend.tables.records.NotificationRecord;

import java.util.List;

import static vn.entity.backend.tables.Notification.NOTIFICATION;

@Repository
public class NotificationRepository extends AbsMysqlRepository<
        NotificationRecord,
        Notification,
        Integer> {
    @Override
    protected TableImpl<NotificationRecord> getTable() {
        return NOTIFICATION;
    }

    public List<Notification> getByCriteria(
            Integer userId,
            Integer isRead,
            Integer branchId,
            Pageable pageable
    ) {

        Condition condition = NOTIFICATION.USER_ID.eq(userId);

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(NOTIFICATION.BRANCH_ID.eq(branchId));
        }

        if (isRead != null) {
            condition = condition.and(NOTIFICATION.IS_READ.eq(isRead));
        }

        return getListByCriteria(
                condition,
                NOTIFICATION.CREATED_TIME.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            Integer userId,
            Integer isRead,
            Integer branchId
    ) {

        Condition condition = NOTIFICATION.USER_ID.eq(userId);

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(NOTIFICATION.BRANCH_ID.eq(branchId));
        }

        if (isRead != null) {
            condition = condition.and(NOTIFICATION.IS_READ.eq(isRead));
        }

        return getCountWithCriteria(condition);
    }

    public Integer markRead(Integer id) {

        return dslContext.update(NOTIFICATION)
                .set(NOTIFICATION.IS_READ, 1)
                .where(NOTIFICATION.ID.eq(id))
                .execute();
    }

    public Integer markAllRead(Integer userId) {

        return dslContext.update(NOTIFICATION)
                .set(NOTIFICATION.IS_READ, 1)
                .where(NOTIFICATION.USER_ID.eq(userId))
                .execute();
    }

    public Long countUnread(Integer userId, Integer branchId) {
        Condition condition = NOTIFICATION.USER_ID.eq(userId)
                .and(NOTIFICATION.IS_READ.eq(0));

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(NOTIFICATION.BRANCH_ID.eq(branchId));
        }

        return getCountWithCriteria(condition);
    }
}