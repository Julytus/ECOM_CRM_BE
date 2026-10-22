package vn.aeoc.crm.repository.customer;

import org.jooq.DSLContext;
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
import vn.aeoc.entity.data.mysql.customer.Customer;
import vn.entity.backend.tables.records.CustomerRecord;

import java.util.List;

import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.CustomerSegmentSnapshot.CUSTOMER_SEGMENT_SNAPSHOT;
import static vn.entity.backend.tables.CustomerSource.CUSTOMER_SOURCE;
import static vn.entity.backend.tables.User.USER;

@Repository
public class CustomerRepository
        extends AbsMysqlRepository<CustomerRecord, Customer, Integer> {
    public CustomerRepository(DSLContext dslContext) {
        super(dslContext);
    }

    /* ==============================
            CONDITION
       ============================== */
    @Override
    protected TableImpl<CustomerRecord> getTable() {
        return CUSTOMER;
    }

    private Condition getWhereCondition(String keyword, Integer branchId, String segmentCode) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(CUSTOMER.BRANCH_ID.eq(branchId));
        }

        if (!StringUtils.isBlank(segmentCode)) {
            condition = condition.and(CUSTOMER_SEGMENT_SNAPSHOT.SEGMENT_CODE.eq(segmentCode.trim().toUpperCase()));
        }

        if (!StringUtils.isBlank(keyword)) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            condition = condition.and(
                    DSL.lower(CUSTOMER.NAME).like(kw)
                            .or(DSL.lower(CUSTOMER.EMAIL).like(kw))
                            .or(DSL.lower(CUSTOMER.PHONE).like(kw))
            );
        }

        return condition;
    }

    /* ==============================
            JOIN
       ============================== */

    private List<Field<?>> getJoinFields() {
        return mergeFields(
                USER.as("u1").NAME.as("user_name"),
                USER.as("u2").NAME.as("creator_name"),
                CUSTOMER_SOURCE.NAME.as("source_name"),
                CUSTOMER_SEGMENT_SNAPSHOT.SEGMENT_CODE.as("segment_code")
        );
    }

    private Table<?> getJoinTable() {
        var u1 = USER.as("u1");
        var u2 = USER.as("u2");

        return CUSTOMER
                .leftJoin(u1).on(u1.ID.eq(CUSTOMER.USER_ID))
                .leftJoin(u2).on(u2.ID.eq(CUSTOMER.CREATOR_ID))
                .leftJoin(CUSTOMER_SOURCE).on(CUSTOMER_SOURCE.ID.eq(CUSTOMER.SOURCE_ID))
                .leftJoin(CUSTOMER_SEGMENT_SNAPSHOT).on(CUSTOMER_SEGMENT_SNAPSHOT.CUSTOMER_ID.eq(CUSTOMER.ID)
                        .and(CUSTOMER_SEGMENT_SNAPSHOT.BRANCH_ID.eq(CUSTOMER.BRANCH_ID)));
    }

    /* ==============================
            QUERY
       ============================== */

    public List<Customer> getByCriteria(
            String keyword,
            Integer branchId,
            String segmentCode,
            Pageable pageable
    ) {
        if (StringUtils.isBlank(keyword) && StringUtils.isBlank(segmentCode)) {
            return getByCriteriaNoKeywordFast(branchId, pageable);
        }

        if (StringUtils.isBlank(keyword)) {
            return getByCriteriaWithSegmentNoKeywordFast(branchId, segmentCode, pageable);
        }

        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                getWhereCondition(keyword, branchId, segmentCode),
                CUSTOMER.ID.desc(),
                pageable
        );
    }

    private List<Customer> getByCriteriaNoKeywordFast(Integer branchId, Pageable pageable) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(CUSTOMER.BRANCH_ID.eq(branchId));
        }

        List<Integer> customerIds = dslContext
                .select(CUSTOMER.ID)
                .from(CUSTOMER)
                .where(condition)
                .orderBy(CUSTOMER.UPDATED_TIME.desc(), CUSTOMER.ID.desc())
                .limit(pageable.getLimit())
                .offset(pageable.getOffset())
                .fetchInto(Integer.class);

        if (customerIds.isEmpty()) {
            return List.of();
        }

        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                CUSTOMER.ID.in(customerIds),
                CUSTOMER.ID.desc()
        );
    }

    private List<Customer> getByCriteriaWithSegmentNoKeywordFast(Integer branchId, String segmentCode, Pageable pageable) {

        return getListByCriteriaWithJoin(
               getJoinFields(),
               getJoinTable(),
               getWhereCondition(null, branchId, segmentCode),
               CUSTOMER.ID.desc(),
               pageable
       );
    }

    public Long countByCriteria(String keyword, Integer branchId, String segmentCode) {
        return dslContext
                .selectCount()
                .from(getJoinTable())
                .where(getWhereCondition(keyword, branchId, segmentCode))
                .fetchOne(0, Long.class);
    }

    public Customer getByIdWithJoin(Integer id) {
        return getOneByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                CUSTOMER.ID.eq(id)
        );
    }

    public Integer findAccountUserIdById(Integer customerId) {
        return dslContext.select(CUSTOMER.ACCOUNT_USER_ID)
                .from(CUSTOMER)
                .where(CUSTOMER.ID.eq(customerId))
                .fetchOne(CUSTOMER.ACCOUNT_USER_ID);
    }

    public Integer findCustomerIdByAccountUserId(Integer accountUserId) {
        return dslContext.select(CUSTOMER.ID)
                .from(CUSTOMER)
                .where(CUSTOMER.ACCOUNT_USER_ID.eq(accountUserId))
                .fetchOne(CUSTOMER.ID);
    }

}
