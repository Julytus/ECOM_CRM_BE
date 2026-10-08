package vn.aeoc.crm.repository.customer;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.customer.Opportunity;
import vn.entity.backend.tables.records.OpportunityRecord;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.sum;

import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.Opportunity.OPPORTUNITY;
import static vn.entity.backend.tables.User.USER;

@Repository
public class OpportunityRepository
        extends AbsMysqlRepository<OpportunityRecord, Opportunity, Integer> {

    @Override
    protected TableImpl<OpportunityRecord> getTable() {
        return OPPORTUNITY;
    }

    private List<Field<?>> getJoinFields() {
        return mergeFields(
                CUSTOMER.NAME.as("customer_name"),
                USER.NAME.as("user_name")
        );
    }

    private Table<?> getJoinTable() {
        return OPPORTUNITY
                .leftJoin(CUSTOMER).on(CUSTOMER.ID.eq(OPPORTUNITY.CUSTOMER_ID))
                .leftJoin(USER).on(USER.ID.eq(OPPORTUNITY.USER_ID));
    }

    private Condition getWhereCondition(
            String keyword,
            Integer customerId,
            Integer userId,
            Integer stage,
            List<Integer> priorities,
            Integer status,
            Boolean unassignedOnly,
            Boolean assignedOnly,
            Integer branchId
    ) {

        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(OPPORTUNITY.BRANCH_ID.eq(branchId));
        }

        if (!StringUtils.isBlank(keyword)) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            condition = condition.and(
                    OPPORTUNITY.NAME.likeIgnoreCase(kw)
                            .or(OPPORTUNITY.CODE.likeIgnoreCase(kw))
            );
        }

        if (CommonUtils.NVL(customerId) > 0) {
            condition = condition.and(OPPORTUNITY.CUSTOMER_ID.eq(customerId));
        }

        if (CommonUtils.NVL(userId) > 0) {
            condition = condition.and(OPPORTUNITY.USER_ID.eq(userId));
        }

        if (CommonUtils.NVL(stage) > 0) {
            condition = condition.and(OPPORTUNITY.STAGE.eq(stage));
        }

        if (priorities != null && !priorities.isEmpty()) {
            List<Integer> safePriorities = priorities.stream()
                    .filter(Objects::nonNull)
                    .toList();
            if (!safePriorities.isEmpty()) {
                condition = condition.and(OPPORTUNITY.PRIORITY.in(safePriorities));
            }
        }

        if (status != null) {
            condition = condition.and(OPPORTUNITY.STATUS.eq(status));
        }

        if (Boolean.TRUE.equals(unassignedOnly)) {
            condition = condition.and(OPPORTUNITY.USER_ID.isNull());
        }

        if (Boolean.TRUE.equals(assignedOnly)) {
            condition = condition.and(OPPORTUNITY.USER_ID.isNotNull());
        }

        return condition;
    }

    @Override
    public Opportunity getById(Integer id) {
        return getByIdWithJoin(getJoinFields(), getJoinTable(), id);
    }

    public List<Opportunity> getByCriteria(
            String keyword,
            Integer customerId,
            Integer userId,
            Integer stage,
            List<Integer> priorities,
            Integer status,
            Boolean unassignedOnly,
            Boolean assignedOnly,
            Integer branchId,
            Pageable pageable
    ) {

        Condition condition = getWhereCondition(
                keyword,
                customerId,
                userId,
                stage,
                priorities,
                status,
                unassignedOnly,
                assignedOnly,
                branchId
        );

        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                condition,
                OPPORTUNITY.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            String keyword,
            Integer customerId,
            Integer userId,
            Integer stage,
            List<Integer> priorities,
            Integer status,
            Boolean unassignedOnly,
            Boolean assignedOnly,
            Integer branchId
    ) {

        Condition condition = getWhereCondition(
                keyword,
                customerId,
                userId,
                stage,
                priorities,
                status,
                unassignedOnly,
                assignedOnly,
                branchId
        );

        return getCountWithCriteria(condition);
    }

    public OpportunityMetrics getMetricsByCriteria(
            String keyword,
            Integer customerId,
            Integer userId,
            Integer stage,
            List<Integer> priorities,
            Integer status,
            Boolean unassignedOnly,
            Boolean assignedOnly,
            Integer branchId
    ) {
        Condition condition = getWhereCondition(
                keyword,
                customerId,
                userId,
                stage,
                priorities,
                status,
                unassignedOnly,
                assignedOnly,
                branchId
        );

        return dslContext
                .select(
                        DSL.count().as("total"),
                        coalesce(sum(OPPORTUNITY.EXPECTED_VALUE), 0d).as("total_expected_value"),
                        coalesce(sum(OPPORTUNITY.EXPECTED_VALUE.mul(coalesce(OPPORTUNITY.PROBABILITY, 0).div(100.0))), 0d).as("total_weighted_value")
                )
                .from(OPPORTUNITY)
                .where(condition)
                .fetchOne(record -> new OpportunityMetrics(
                        record.get("total", Long.class),
                        record.get("total_expected_value", Double.class),
                        record.get("total_weighted_value", Double.class)
                ));
    }

    public record OpportunityMetrics(Long total, Double totalExpectedValue, Double totalWeightedValue) {}

    public Integer updateStage(Integer id, Integer stage) {
        return dslContext.update(OPPORTUNITY)
                .set(OPPORTUNITY.STAGE, stage)
                .where(OPPORTUNITY.ID.eq(id))
                .execute();
    }

    public Integer markLost(Integer id, String reason) {
        return dslContext.update(OPPORTUNITY)
                .set(OPPORTUNITY.STAGE, 7)
                .set(OPPORTUNITY.LOST_REASON, reason)
                .set(OPPORTUNITY.STATUS, 1)
                .where(OPPORTUNITY.ID.eq(id))
                .execute();
    }

    public Integer markWon(Integer id, java.time.LocalDateTime actualCloseDate) {
        var update = dslContext.update(OPPORTUNITY)
                .set(OPPORTUNITY.STAGE, 6)
                .set(OPPORTUNITY.STATUS, 1);

        if (actualCloseDate != null) {
            update.set(OPPORTUNITY.ACTUAL_CLOSE_DATE, actualCloseDate);
        }

        return update
                .where(OPPORTUNITY.ID.eq(id))
                .execute();
    }

    public Map<Integer, Integer> countOpportunityByCustomerIds(List<Integer> customerIds) {
        return countGroupBy(
                OPPORTUNITY.CUSTOMER_ID,
                OPPORTUNITY.ID,
                OPPORTUNITY,
                OPPORTUNITY.CUSTOMER_ID.in(customerIds)
        );
    }

    public Map<Integer, Integer> countActiveOpportunityByUserIds(List<Integer> userIds, Integer branchId) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Condition condition = OPPORTUNITY.USER_ID.in(userIds).and(OPPORTUNITY.STATUS.eq(1));
        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(OPPORTUNITY.BRANCH_ID.eq(branchId));
        }

        return countGroupBy(
                OPPORTUNITY.USER_ID,
                OPPORTUNITY.ID,
                OPPORTUNITY,
                condition
        );
    }

    public Integer assignUser(Integer id, Integer userId) {
        return dslContext.update(OPPORTUNITY)
                .set(OPPORTUNITY.USER_ID, userId)
                .where(OPPORTUNITY.ID.eq(id))
                .execute();
    }
}
