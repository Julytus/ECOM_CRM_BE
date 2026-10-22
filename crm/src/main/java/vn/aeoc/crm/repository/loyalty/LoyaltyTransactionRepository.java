package vn.aeoc.crm.repository.loyalty;

import org.jooq.DSLContext;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTransaction;
import vn.entity.backend.tables.records.LoyaltyTransactionRecord;

import java.util.List;

import static vn.entity.backend.tables.LoyaltyTransaction.LOYALTY_TRANSACTION;

@Repository
public class LoyaltyTransactionRepository {
    private final DSLContext dsl;

    public LoyaltyTransactionRepository(DSLContext dsl) { this.dsl = dsl; }

    /** Returns false when the unique reference has already been recorded. */
    public boolean insert(LoyaltyTransaction transaction) {
        try {
            dsl.insertInto(LOYALTY_TRANSACTION)
                    .set(LOYALTY_TRANSACTION.CUSTOMER_ID, transaction.getCustomerId())
                    .set(LOYALTY_TRANSACTION.TYPE, transaction.getType())
                    .set(LOYALTY_TRANSACTION.POINTS, transaction.getPoints())
                    .set(LOYALTY_TRANSACTION.RANK_POINTS_DELTA, transaction.getRankPointsDelta())
                    .set(LOYALTY_TRANSACTION.BALANCE_AFTER, transaction.getBalanceAfter())
                    .set(LOYALTY_TRANSACTION.RANK_POINTS_AFTER, transaction.getRankPointsAfter())
                    .set(LOYALTY_TRANSACTION.REF_TYPE, transaction.getRefType())
                    .set(LOYALTY_TRANSACTION.REF_ID, transaction.getRefId())
                    .set(LOYALTY_TRANSACTION.NOTE, transaction.getNote())
                    .set(LOYALTY_TRANSACTION.CREATED_BY, transaction.getCreatedBy())
                    .execute();
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    public LoyaltyTransaction findByRef(String type, String refType, String refId) {
        return dsl.selectFrom(LOYALTY_TRANSACTION)
                .where(LOYALTY_TRANSACTION.TYPE.eq(type)
                        .and(LOYALTY_TRANSACTION.REF_TYPE.eq(refType))
                        .and(LOYALTY_TRANSACTION.REF_ID.eq(refId)))
                .fetchOneInto(LoyaltyTransaction.class);
    }

    public Page<LoyaltyTransaction> pageByCustomer(Integer customerId, Pageable pageable) {
        long total = dsl.fetchCount(LOYALTY_TRANSACTION, LOYALTY_TRANSACTION.CUSTOMER_ID.eq(customerId));
        List<LoyaltyTransaction> rows = dsl.selectFrom(LOYALTY_TRANSACTION)
                .where(LOYALTY_TRANSACTION.CUSTOMER_ID.eq(customerId))
                .orderBy(LOYALTY_TRANSACTION.ID.desc())
                .limit(pageable.getLimit()).offset(pageable.getOffset())
                .fetchInto(LoyaltyTransaction.class);
        pageable.setTotal(total);
        return new Page<>(pageable, rows);
    }

    public PointTotals sumPoints(Integer customerId) {
        var row = dsl.select(
                        org.jooq.impl.DSL.coalesce(org.jooq.impl.DSL.sum(LOYALTY_TRANSACTION.POINTS), 0L).as("points"),
                        org.jooq.impl.DSL.coalesce(org.jooq.impl.DSL.sum(LOYALTY_TRANSACTION.RANK_POINTS_DELTA), 0L).as("rank_points"))
                .from(LOYALTY_TRANSACTION)
                .where(LOYALTY_TRANSACTION.CUSTOMER_ID.eq(customerId))
                .fetchOne();
        return new PointTotals(row.get("points", Long.class), row.get("rank_points", Long.class));
    }

    public record PointTotals(long points, long rankPoints) { }
}
