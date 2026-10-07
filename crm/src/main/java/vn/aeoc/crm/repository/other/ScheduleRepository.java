package vn.aeoc.crm.repository.other;

import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.Schedule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static vn.entity.backend.tables.Branch.BRANCH;
import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.Schedule.SCHEDULE;
import static vn.entity.backend.tables.User.USER;

@Repository
public class ScheduleRepository {

    @Autowired
    private DSLContext dslContext;

    // ==========================
    //   FIELDS
    // ==========================

    private static List<Field<?>> getFields() {
        return asList(
                SCHEDULE.ID,
                SCHEDULE.TITLE,
                SCHEDULE.CREATOR_ID,
                SCHEDULE.CONTENT,
                SCHEDULE.CUSTOMER_ID,
                SCHEDULE.USER_ID,
                SCHEDULE.NOTE,
                SCHEDULE.START_TIME,
                SCHEDULE.END_TIME,
                SCHEDULE.TYPE,
                SCHEDULE.BRANCH_ID,
                SCHEDULE.EMAIL_SENT
        );
    }

    private Condition getWhereCondition(String keyword, Integer customerId, Integer type, Integer branchId) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(SCHEDULE.BRANCH_ID.eq(branchId));
        }

        if (!StringUtils.isBlank(keyword)) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            condition = condition.and(DSL.lower(SCHEDULE.TITLE).like(kw));
        }

        if (CommonUtils.NVL(customerId) > 0) {
            condition = condition.and(SCHEDULE.CUSTOMER_ID.eq(customerId));
        }

        if (CommonUtils.NVL(type) > 0) {
            condition = condition.and(SCHEDULE.TYPE.eq(type));
        }

        return condition;
    }

    // ==========================
    //   LIST
    // ==========================

    public List<Schedule> getByCriteria(String keyword, Integer customerId, Integer type, Integer branchId, Pageable pageable) {

        var creator = USER.as("creator");
        var c = CUSTOMER.as("c");
        var b = BRANCH.as("b");

        List<Field<?>> selected = new ArrayList<>(getFields());
        selected.add(creator.NAME.as("creator_name"));
        selected.add(c.NAME.as("customer_name"));
        selected.add(b.NAME.as("branch_name"));

        Condition condition = getWhereCondition(keyword, customerId, type, branchId);

        SelectConditionStep<Record> select = dslContext
                .select(selected)
                .from(SCHEDULE)
                .leftJoin(creator).on(creator.ID.eq(SCHEDULE.CREATOR_ID))
                .leftJoin(c).on(c.ID.eq(SCHEDULE.CUSTOMER_ID))
                .leftJoin(b).on(b.ID.eq(SCHEDULE.BRANCH_ID))
                .where(condition);

        select.orderBy(SCHEDULE.START_TIME.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getLimit());

        return select.fetchInto(Schedule.class);
    }

    public Long countByCriteria(String keyword, Integer customerId, Integer type, Integer branchId) {
        Condition condition = getWhereCondition(keyword, customerId, type, branchId);

        return dslContext
                .selectCount()
                .from(SCHEDULE)
                .where(condition)
                .fetchOne(0, Long.class);
    }

    // ==========================
    //   GET ONE
    // ==========================

    public Schedule getById(Integer id) {

        var creator = USER.as("creator");
        var c = CUSTOMER.as("c");
        var b = BRANCH.as("b");

        List<Field<?>> selected = new ArrayList<>(getFields());
        selected.add(creator.NAME.as("creator_name"));
        selected.add(c.NAME.as("customer_name"));
        selected.add(b.NAME.as("branch_name"));

        return dslContext
                .select(selected)
                .from(SCHEDULE)
                .leftJoin(creator).on(creator.ID.eq(SCHEDULE.CREATOR_ID))
                .leftJoin(c).on(c.ID.eq(SCHEDULE.CUSTOMER_ID))
                .leftJoin(b).on(b.ID.eq(SCHEDULE.BRANCH_ID))
                .where(SCHEDULE.ID.eq(id))
                .fetchOptionalInto(Schedule.class)
                .orElse(null);
    }

    // ==========================
    //   INSERT
    // ==========================

    public Schedule insert(Schedule item) {
        dslContext.insertInto(SCHEDULE)
                .set(SCHEDULE.TITLE, item.getTitle())
                .set(SCHEDULE.CREATOR_ID, item.getCreatorId())
                .set(SCHEDULE.CONTENT, item.getContent())
                .set(SCHEDULE.CUSTOMER_ID, item.getCustomerId())
                .set(SCHEDULE.USER_ID, item.getUserId())
                .set(SCHEDULE.NOTE, item.getNote())
                .set(SCHEDULE.START_TIME, item.getStartTime())
                .set(SCHEDULE.END_TIME, item.getEndTime())
                .set(SCHEDULE.TYPE, item.getType())
                .set(SCHEDULE.BRANCH_ID, item.getBranchId())
                .set(SCHEDULE.EMAIL_SENT, 0)
                .execute();

        item.setId(dslContext.lastID().intValue());
        return item;
    }

    // ==========================
    //   UPDATE
    // ==========================

    public Schedule update(Schedule item) {
        dslContext.update(SCHEDULE)
                .set(SCHEDULE.TITLE, item.getTitle())
                .set(SCHEDULE.CONTENT, item.getContent())
                .set(SCHEDULE.CUSTOMER_ID, item.getCustomerId())
                .set(SCHEDULE.USER_ID, item.getUserId())
                .set(SCHEDULE.NOTE, item.getNote())
                .set(SCHEDULE.START_TIME, item.getStartTime())
                .set(SCHEDULE.END_TIME, item.getEndTime())
                .set(SCHEDULE.TYPE, item.getType())
                .set(SCHEDULE.BRANCH_ID, item.getBranchId())
                .set(SCHEDULE.EMAIL_SENT, 0)
                .where(SCHEDULE.ID.eq(item.getId()))
                .execute();

        return item;
    }

    // ==========================
    //   DELETE
    // ==========================

    public Integer delete(Integer id) {
        return dslContext
                .delete(SCHEDULE)
                .where(SCHEDULE.ID.eq(id))
                .execute();
    }

    /**
     * Lấy danh sách lịch sắp diễn ra trong 1 tiếng tới
     * và chưa gửi email
     */
    public List<Schedule> getSchedulesAfter(LocalDateTime fromTime) {

        var u = USER.as("u");
        var c = CUSTOMER.as("c");
        var b = BRANCH.as("b");

        List<Field<?>> selected = new ArrayList<>(getFields());
        selected.add(u.EMAIL.as("userEmail"));
        selected.add(u.NAME.as("userName"));
        selected.add(c.NAME.as("customerName"));
        selected.add(b.NAME.as("branchName"));

        SelectConditionStep<?> select =
                dslContext
                        .select(selected)
                        .from(SCHEDULE)
                        .join(u).on(u.ID.eq(SCHEDULE.USER_ID))
                        .leftJoin(c).on(c.ID.eq(SCHEDULE.CUSTOMER_ID))
                        .leftJoin(b).on(b.ID.eq(SCHEDULE.BRANCH_ID))
                        .where(SCHEDULE.EMAIL_SENT.eq(0))
                        .and(SCHEDULE.START_TIME.gt(fromTime))
                ;
        return select
                .fetchInto(Schedule.class);
    }

    /**
     * Đánh dấu đã gửi email
     */
    public void markEmailSent(Integer scheduleId) {
        dslContext.update(SCHEDULE)
                .set(SCHEDULE.EMAIL_SENT, 1)
                .where(SCHEDULE.ID.eq(scheduleId))
                .execute();
    }
}
