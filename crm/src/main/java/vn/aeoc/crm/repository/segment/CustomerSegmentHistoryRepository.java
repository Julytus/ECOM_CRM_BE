package vn.aeoc.crm.repository.segment;

import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.data.segment.CustomerSegmentHistoryRow;
import vn.aeoc.crm.data.segment.SegmentTrendItem;
import vn.entity.backend.tables.records.CustomerSegmentHistoryRecord;

import java.util.List;

import static vn.entity.backend.tables.CustomerSegmentHistory.CUSTOMER_SEGMENT_HISTORY;

@Repository
public class CustomerSegmentHistoryRepository extends AbsMysqlRepository<
        CustomerSegmentHistoryRecord,
        CustomerSegmentHistoryRow,
        Integer> {

    @Override
    protected TableImpl<CustomerSegmentHistoryRecord> getTable() {
        return CUSTOMER_SEGMENT_HISTORY;
    }

    /*
     * Insert segment-change history rows in a single DB batch.
     */
    public void insertBatchHistory(List<CustomerSegmentHistoryRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        insertBatchReturning(rows);
    }

    /*
     * Return trend data grouped by month and segment code from history table.
     */
    public List<SegmentTrendItem> getTrendByMonth(Integer branchId, Integer monthsBack) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(CUSTOMER_SEGMENT_HISTORY.BRANCH_ID.eq(branchId));
        }

        if (CommonUtils.NVL(monthsBack) > 0) {
            condition = condition.and(
                    CUSTOMER_SEGMENT_HISTORY.CREATED_TIME.ge(
                            DSL.localDateTimeSub(DSL.currentLocalDateTime(), monthsBack, org.jooq.DatePart.MONTH)
                    )
            );
        }

        org.jooq.Field<String> periodField = DSL.field(
                "DATE_FORMAT({0}, '%Y-%m')",
                String.class,
                CUSTOMER_SEGMENT_HISTORY.CREATED_TIME
        );

        return dslContext.select(
                        periodField.as("period"),
                        CUSTOMER_SEGMENT_HISTORY.TO_SEGMENT.as("segmentCode"),
                        DSL.countDistinct(CUSTOMER_SEGMENT_HISTORY.CUSTOMER_ID).as("totalCustomer")
                )
                .from(CUSTOMER_SEGMENT_HISTORY)
                .where(condition)
                .groupBy(periodField, CUSTOMER_SEGMENT_HISTORY.TO_SEGMENT)
                .orderBy(periodField.asc(), CUSTOMER_SEGMENT_HISTORY.TO_SEGMENT.asc())
                .fetchInto(SegmentTrendItem.class);
    }
}
