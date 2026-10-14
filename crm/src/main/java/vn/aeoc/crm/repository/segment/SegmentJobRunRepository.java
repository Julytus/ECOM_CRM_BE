package vn.aeoc.crm.repository.segment;

import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.constant.CustomerSegmentConstant;
import vn.aeoc.crm.data.segment.SegmentJobRunRow;
import vn.entity.backend.tables.records.SegmentJobRunRecord;

import java.time.LocalDateTime;

import static vn.entity.backend.tables.SegmentJobRun.SEGMENT_JOB_RUN;

@Repository
public class SegmentJobRunRepository extends AbsMysqlRepository<
        SegmentJobRunRecord,
        SegmentJobRunRow,
        Integer> {
    public SegmentJobRunRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<SegmentJobRunRecord> getTable() {
        return SEGMENT_JOB_RUN;
    }

    public Integer markSuccess(String runId, Integer totalCustomers, Integer processedCustomers, Integer errorCount, String message) {
        return dslContext.update(SEGMENT_JOB_RUN)
                .set(SEGMENT_JOB_RUN.STATUS, CustomerSegmentConstant.JOB_STATUS_SUCCESS)
                .set(SEGMENT_JOB_RUN.TOTAL_CUSTOMERS, totalCustomers)
                .set(SEGMENT_JOB_RUN.PROCESSED_CUSTOMERS, processedCustomers)
                .set(SEGMENT_JOB_RUN.ERROR_COUNT, errorCount)
                .set(SEGMENT_JOB_RUN.FINISHED_TIME, LocalDateTime.now())
                .set(SEGMENT_JOB_RUN.MESSAGE, message)
                .where(SEGMENT_JOB_RUN.RUN_ID.eq(runId))
                .execute();
    }

    public Integer markRunning(String runId, Integer totalCustomers, Integer processedCustomers, Integer errorCount, Integer checkpointCustomerId, Integer checkpointChunkIndex, String message) {
        return dslContext.update(SEGMENT_JOB_RUN)
                .set(SEGMENT_JOB_RUN.STATUS, CustomerSegmentConstant.JOB_STATUS_RUNNING)
                .set(SEGMENT_JOB_RUN.TOTAL_CUSTOMERS, totalCustomers)
                .set(SEGMENT_JOB_RUN.PROCESSED_CUSTOMERS, processedCustomers)
                .set(SEGMENT_JOB_RUN.ERROR_COUNT, errorCount)
                .set(SEGMENT_JOB_RUN.MESSAGE, message)
                .set(SEGMENT_JOB_RUN.FINISHED_TIME, (LocalDateTime) null)
                .where(SEGMENT_JOB_RUN.RUN_ID.eq(runId))
                .execute();
    }

    public Integer markPaused(String runId, Integer totalCustomers, Integer processedCustomers, Integer errorCount, String message) {
        return dslContext.update(SEGMENT_JOB_RUN)
                .set(SEGMENT_JOB_RUN.STATUS, "PAUSED")
                .set(SEGMENT_JOB_RUN.TOTAL_CUSTOMERS, totalCustomers)
                .set(SEGMENT_JOB_RUN.PROCESSED_CUSTOMERS, processedCustomers)
                .set(SEGMENT_JOB_RUN.ERROR_COUNT, errorCount)
                .set(SEGMENT_JOB_RUN.FINISHED_TIME, LocalDateTime.now())
                .set(SEGMENT_JOB_RUN.MESSAGE, message)
                .where(SEGMENT_JOB_RUN.RUN_ID.eq(runId))
                .execute();
    }

    public Integer markFailed(String runId, Integer totalCustomers, Integer processedCustomers, Integer errorCount, String message) {
        return dslContext.update(SEGMENT_JOB_RUN)
                .set(SEGMENT_JOB_RUN.STATUS, CustomerSegmentConstant.JOB_STATUS_FAILED)
                .set(SEGMENT_JOB_RUN.TOTAL_CUSTOMERS, totalCustomers)
                .set(SEGMENT_JOB_RUN.PROCESSED_CUSTOMERS, processedCustomers)
                .set(SEGMENT_JOB_RUN.ERROR_COUNT, errorCount)
                .set(SEGMENT_JOB_RUN.FINISHED_TIME, LocalDateTime.now())
                .set(SEGMENT_JOB_RUN.MESSAGE, message)
                .where(SEGMENT_JOB_RUN.RUN_ID.eq(runId))
                .execute();
    }

    public SegmentJobRunRow getByRunId(String runId) {
        return dslContext.selectFrom(SEGMENT_JOB_RUN)
                .where(SEGMENT_JOB_RUN.RUN_ID.eq(runId))
                .limit(1)
                .fetchOneInto(SegmentJobRunRow.class);
    }

    public SegmentJobRunRow getLatestByBranch(Integer branchId) {
        Condition condition = DSL.trueCondition();
        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(SEGMENT_JOB_RUN.BRANCH_ID.eq(branchId));
        }

        return dslContext.selectFrom(SEGMENT_JOB_RUN)
                .where(condition)
                .orderBy(SEGMENT_JOB_RUN.STARTED_TIME.desc(), SEGMENT_JOB_RUN.ID.desc())
                .limit(1)
                .fetchOneInto(SegmentJobRunRow.class);
    }
}
