package vn.aeoc.crm.repository.segment;

import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.Query;
import org.jooq.SelectHavingStep;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.data.segment.CustomerInvoiceAggregate;
import vn.aeoc.crm.data.segment.CustomerSegmentSnapshotRow;
import vn.aeoc.crm.data.segment.SegmentSummaryItem;
import vn.entity.backend.tables.records.CustomerSegmentSnapshotRecord;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.CustomerSegmentSnapshot.CUSTOMER_SEGMENT_SNAPSHOT;
import static vn.entity.backend.tables.Invoice.INVOICE;

@Repository
public class CustomerSegmentRepository extends AbsMysqlRepository<
        CustomerSegmentSnapshotRecord,
        CustomerSegmentSnapshotRow,
        Integer> {
    public CustomerSegmentRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<CustomerSegmentSnapshotRecord> getTable() {
        return CUSTOMER_SEGMENT_SNAPSHOT;
    }

    /*
     * Build invoice aggregate for each customer in branch within the rolling 90-day window.
     */
    public List<CustomerInvoiceAggregate> aggregateInvoice90d(Integer branchId, LocalDateTime since) {
        Condition condition = INVOICE.STATUS.eq(InvoiceConstant.STATUS_DONE)
                .and(INVOICE.CREATED_TIME.isNotNull())
                .and(INVOICE.CREATED_TIME.ge(since));

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(INVOICE.BRANCH_ID.eq(branchId));
        }

        return dslContext.select(
                        INVOICE.CUSTOMER_ID.as("customerId"),
                        INVOICE.BRANCH_ID.as("branchId"),
                        DSL.coalesce(DSL.count(INVOICE.ID), 0).as("frequency90d"),
                        DSL.coalesce(DSL.sum(INVOICE.FEE), 0.0).as("monetary90d"),
                        DSL.max(INVOICE.RECEIPT_DATE).as("lastReceiptTime")
                )
                .from(INVOICE)
                .where(condition)
                .groupBy(INVOICE.CUSTOMER_ID, INVOICE.BRANCH_ID)
                .fetchInto(CustomerInvoiceAggregate.class);
    }

    /*
     * Get snapshot rows by customer ids for one branch/all branches.
     */
    public List<CustomerSegmentSnapshotRow> getSnapshotByCustomerIds(Integer branchId, Collection<Integer> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return List.of();
        }

        Condition condition = CUSTOMER_SEGMENT_SNAPSHOT.CUSTOMER_ID.in(customerIds);
        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(CUSTOMER_SEGMENT_SNAPSHOT.BRANCH_ID.eq(branchId));
        }

        return getListByCriteria(condition);
    }

    /*
     * Upsert snapshot rows into customer_segment_snapshot in a batch.
     */
    public void upsertSnapshots(List<CustomerSegmentSnapshotRow> rows) {

        if (rows == null || rows.isEmpty()) {
            return;
        }

        List<Query> queries = rows.stream()
                .map(row -> (Query) dslContext.insertInto(CUSTOMER_SEGMENT_SNAPSHOT)
                        .set(dslContext.newRecord(CUSTOMER_SEGMENT_SNAPSHOT, row))
                        .onDuplicateKeyUpdate()
                        .set(CUSTOMER_SEGMENT_SNAPSHOT.SEGMENT_CODE, row.getSegmentCode())
                        .set(CUSTOMER_SEGMENT_SNAPSHOT.RECENCY_DAYS, row.getRecencyDays())
                        .set(CUSTOMER_SEGMENT_SNAPSHOT.FREQUENCY_90D, row.getFrequency90d())
                        .set(CUSTOMER_SEGMENT_SNAPSHOT.MONETARY_90D, row.getMonetary90d())
                        .set(CUSTOMER_SEGMENT_SNAPSHOT.TOTAL_SCORE, row.getTotalScore())
                        .set(CUSTOMER_SEGMENT_SNAPSHOT.RULE_VERSION, row.getRuleVersion())
                        .set(CUSTOMER_SEGMENT_SNAPSHOT.ACTIVE, row.getActive())
                        .set(CUSTOMER_SEGMENT_SNAPSHOT.UPDATED_TIME, row.getUpdatedTime())
                )
                .toList();

        dslContext.batch(queries).execute();
    }

    /*
     * Fetch snapshot list by criteria with paging.
     */
    public List<CustomerSegmentSnapshotRow> getSnapshotByCriteria(Integer branchId, String segmentCode, Pageable pageable) {
        Condition condition = DSL.trueCondition();
        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(CUSTOMER_SEGMENT_SNAPSHOT.BRANCH_ID.eq(branchId));
        }
        if (!CommonUtils.NVL(segmentCode).isEmpty()) {
            condition = condition.and(CUSTOMER_SEGMENT_SNAPSHOT.SEGMENT_CODE.eq(segmentCode));
        }

        return getListByCriteria(condition, CUSTOMER_SEGMENT_SNAPSHOT.UPDATED_TIME.desc(), pageable);
    }

    /*
     * Count snapshot rows by criteria.
     */
    public Long countSnapshotByCriteria(Integer branchId, String segmentCode) {
        Condition condition = DSL.trueCondition();
        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(CUSTOMER_SEGMENT_SNAPSHOT.BRANCH_ID.eq(branchId));
        }
        if (!CommonUtils.NVL(segmentCode).isEmpty()) {
            condition = condition.and(CUSTOMER_SEGMENT_SNAPSHOT.SEGMENT_CODE.eq(segmentCode));
        }

        return getCountWithCriteria(condition);
    }

    /*
     * Get summary count by segment code for dashboard chart.
     */
    public List<SegmentSummaryItem> getSummary(Integer branchId) {
        Condition condition = DSL.trueCondition();
        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(CUSTOMER_SEGMENT_SNAPSHOT.BRANCH_ID.eq(branchId));
        }

        return dslContext.select(
                        CUSTOMER_SEGMENT_SNAPSHOT.SEGMENT_CODE.as("segmentCode"),
                        DSL.count(CUSTOMER_SEGMENT_SNAPSHOT.CUSTOMER_ID).as("totalCustomer")
                )
                .from(CUSTOMER_SEGMENT_SNAPSHOT)
                .where(condition)
                .groupBy(CUSTOMER_SEGMENT_SNAPSHOT.SEGMENT_CODE)
                .fetchInto(SegmentSummaryItem.class);
    }

    /*
     * Get customer ids in branch scope for full recompute.
     */
    public List<Integer> getCustomerIdsByBranch(Integer branchId) {
        return dslContext.select(CUSTOMER.ID)
                .from(CUSTOMER)
                .where(CommonUtils.NVL(branchId) > 0 ? CUSTOMER.BRANCH_ID.eq(branchId) : DSL.trueCondition())
                .fetchInto(Integer.class);
    }

    /*
     * Count total customers in branch scope.
     */
    public Integer countCustomersByBranch(Integer branchId) {
        return dslContext.selectCount()
                .from(CUSTOMER)
                .where(CommonUtils.NVL(branchId) > 0 ? CUSTOMER.BRANCH_ID.eq(branchId) : DSL.trueCondition())
                .fetchOne(0, Integer.class);
    }

    /*
     * Get a customer-id chunk by offset/limit to avoid loading all ids into memory.
     * Kept for backward compatibility with existing callers.
     */
    public List<Integer> getCustomerIdsByBranchChunk(Integer branchId, Integer offset, Integer limit) {
        return dslContext.select(CUSTOMER.ID)
                .from(CUSTOMER)
                .where(CommonUtils.NVL(branchId) > 0 ? CUSTOMER.BRANCH_ID.eq(branchId) : DSL.trueCondition())
                .orderBy(CUSTOMER.ID.asc())
                .offset(offset)
                .limit(limit)
                .fetchInto(Integer.class);
    }

    /*
     * Cursor-based chunk fetch for faster recompute on large datasets.
     */
    public List<Integer> getCustomerIdsByBranchAfterId(Integer branchId, Integer afterCustomerId, Integer limit) {
        Condition condition = CommonUtils.NVL(branchId) > 0 ? CUSTOMER.BRANCH_ID.eq(branchId) : DSL.trueCondition();
        if (afterCustomerId != null && afterCustomerId > 0) {
            condition = condition.and(CUSTOMER.ID.gt(afterCustomerId));
        }

        return dslContext.select(CUSTOMER.ID)
                .from(CUSTOMER)
                .where(condition)
                .orderBy(CUSTOMER.ID.asc())
                .limit(limit)
                .fetchInto(Integer.class);
    }

    /*
     * Build invoice aggregate for a specific customer-id chunk.
     */
    public List<CustomerInvoiceAggregate> aggregateInvoice90dByCustomerIds(
            Integer branchId,
            LocalDateTime since,
            Collection<Integer> customerIds
    ) {
        if (customerIds == null || customerIds.isEmpty()) {
            return List.of();
        }

        Condition condition = INVOICE.STATUS.eq(InvoiceConstant.STATUS_DONE)
                .and(INVOICE.CREATED_TIME.isNotNull())
                .and(INVOICE.CREATED_TIME.ge(since))
                .and(INVOICE.CUSTOMER_ID.in(customerIds));

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(INVOICE.BRANCH_ID.eq(branchId));
        }

        SelectHavingStep<?> select = dslContext.select(
                        INVOICE.CUSTOMER_ID.as("customerId"),
                        INVOICE.BRANCH_ID.as("branchId"),
                        DSL.coalesce(DSL.count(INVOICE.ID), 0).as("frequency90d"),
                        DSL.coalesce(DSL.sum(INVOICE.FEE), 0.0).as("monetary90d"),
                        DSL.max(INVOICE.CREATED_TIME).as("lastReceiptTime")
                )
                .from(INVOICE)
                .where(condition)
                .groupBy(INVOICE.CUSTOMER_ID, INVOICE.BRANCH_ID);

        return select
                .fetchInto(CustomerInvoiceAggregate.class);
    }

    public CustomerSegmentSnapshotRow getCustomerSegmentSnapshot(Integer customerId) {

        var record = dslContext
                .select(
                        CUSTOMER_SEGMENT_SNAPSHOT.ID,
                        CUSTOMER_SEGMENT_SNAPSHOT.BRANCH_ID,
                        CUSTOMER_SEGMENT_SNAPSHOT.CUSTOMER_ID,
                        CUSTOMER_SEGMENT_SNAPSHOT.SEGMENT_CODE,
                        CUSTOMER_SEGMENT_SNAPSHOT.RECENCY_DAYS,
                        CUSTOMER_SEGMENT_SNAPSHOT.FREQUENCY_90D.as("frequency90d"),
                        CUSTOMER_SEGMENT_SNAPSHOT.MONETARY_90D.as("monetary90d"),
                        CUSTOMER_SEGMENT_SNAPSHOT.TOTAL_SCORE,
                        CUSTOMER_SEGMENT_SNAPSHOT.RULE_VERSION,
                        CUSTOMER_SEGMENT_SNAPSHOT.ACTIVE,
                        CUSTOMER_SEGMENT_SNAPSHOT.CREATED_TIME,
                        CUSTOMER_SEGMENT_SNAPSHOT.UPDATED_TIME
                )
                .from(CUSTOMER_SEGMENT_SNAPSHOT)
                .where(CUSTOMER_SEGMENT_SNAPSHOT.CUSTOMER_ID.eq(customerId))
                .and(CUSTOMER_SEGMENT_SNAPSHOT.ACTIVE.eq(1))
                .orderBy(CUSTOMER_SEGMENT_SNAPSHOT.UPDATED_TIME.desc())
                .limit(1)
                .fetchOneInto(CustomerSegmentSnapshotRow.class);

        return record;
    }
}
