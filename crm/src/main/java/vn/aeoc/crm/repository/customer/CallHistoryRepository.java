package vn.aeoc.crm.repository.customer;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.customer.CallHistory;
import vn.entity.backend.tables.records.CallHistoryRecord;

import java.util.List;

import static org.jooq.impl.DSL.noCondition;
import static vn.entity.backend.tables.CallHistory.CALL_HISTORY;
import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.User.USER;

@Repository
public class CallHistoryRepository extends AbsMysqlRepository<CallHistoryRecord, CallHistory, Integer> {
    @Override
    protected TableImpl<CallHistoryRecord> getTable() {
        return CALL_HISTORY;
    }
    private Condition where(
            Integer userId,
            Integer customerId,
            Integer callType,
            Integer status,
            Integer branchId) {

        Condition condition = noCondition();

        condition = condition.and(BranchFilter.branchEq(CALL_HISTORY.BRANCH_ID, CommonUtils.NVL(branchId)));

        if (userId != null) {
            condition = condition.and(CALL_HISTORY.USER_ID.eq(userId));
        }
        if (customerId != null) {
            condition = condition.and(CALL_HISTORY.CUSTOMER_ID.eq(customerId));
        }
        if (callType != null) {
            condition = condition.and(CALL_HISTORY.CALL_TYPE.eq(callType));
        }
        if (status != null) {
            condition = condition.and(CALL_HISTORY.STATUS.eq(status));
        }
        return condition;
    }

    private List<Field<?>> getJoinFields() {
        return mergeFields(
                CUSTOMER.NAME.as("customer_name"),
                USER.NAME.as("userName")
        );
    }

    private Table<?> getJoinTable() {
        return CALL_HISTORY
                .leftJoin(CUSTOMER).on(CUSTOMER.ID.eq(CALL_HISTORY.CUSTOMER_ID))
                .leftJoin(USER).on(USER.ID.eq(CALL_HISTORY.USER_ID));
    }

    public List<CallHistory> getByCriteria(
            Integer userId,
            Integer customerId,
            Integer callType,
            Integer status,
            Integer branchId,
            Pageable pageable) {

        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                where(userId, customerId, callType, status, branchId),
                CALL_HISTORY.ID.desc(),
                pageable
                );
    }

    public Long countByCriteria(
            Integer userId,
            Integer customerId,
            Integer callType,
            Integer status,
            Integer branchId) {

        return getCountWithCriteria(where(userId, customerId, callType, status, branchId));
    }

    public Integer updateStatus(Integer id, Integer status) {
        return updateField(id, CALL_HISTORY.STATUS, status);
    }
}
