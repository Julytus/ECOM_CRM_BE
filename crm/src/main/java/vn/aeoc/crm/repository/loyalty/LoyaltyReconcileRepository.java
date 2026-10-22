package vn.aeoc.crm.repository.loyalty;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.List;

import static vn.entity.backend.tables.LoyaltyAccount.LOYALTY_ACCOUNT;
import static vn.entity.backend.tables.LoyaltyTransaction.LOYALTY_TRANSACTION;

@Repository
public class LoyaltyReconcileRepository {
    private final DSLContext dsl;

    public LoyaltyReconcileRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<Mismatch> findMismatches() {
        Field<Long> ledgerPoints = DSL.coalesce(DSL.sum(LOYALTY_TRANSACTION.POINTS).cast(Long.class), DSL.val(0L));
        Field<Long> ledgerRankPoints = DSL.coalesce(
                DSL.sum(LOYALTY_TRANSACTION.RANK_POINTS_DELTA).cast(Long.class), DSL.val(0L));
        return dsl.select(LOYALTY_ACCOUNT.CUSTOMER_ID, LOYALTY_ACCOUNT.BALANCE, ledgerPoints,
                        LOYALTY_ACCOUNT.RANK_POINTS, ledgerRankPoints)
                .from(LOYALTY_ACCOUNT.leftJoin(LOYALTY_TRANSACTION)
                        .on(LOYALTY_TRANSACTION.CUSTOMER_ID.eq(LOYALTY_ACCOUNT.CUSTOMER_ID)))
                .groupBy(LOYALTY_ACCOUNT.CUSTOMER_ID, LOYALTY_ACCOUNT.BALANCE, LOYALTY_ACCOUNT.RANK_POINTS)
                .having(LOYALTY_ACCOUNT.BALANCE.ne(ledgerPoints)
                        .or(LOYALTY_ACCOUNT.RANK_POINTS.ne(ledgerRankPoints)))
                .fetch(row -> new Mismatch(
                        row.get(LOYALTY_ACCOUNT.CUSTOMER_ID),
                        row.get(LOYALTY_ACCOUNT.BALANCE), row.get(ledgerPoints),
                        row.get(LOYALTY_ACCOUNT.RANK_POINTS), row.get(ledgerRankPoints)));
    }

    public record Mismatch(Integer customerId, Long balance, Long ledgerPoints,
                           Long rankPoints, Long ledgerRankPoints) { }
}
