package vn.aeoc.crm.repository.loyalty;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import vn.aeoc.entity.data.mysql.sale.LoyaltyAccount;
import vn.entity.backend.tables.records.LoyaltyAccountRecord;

import static vn.entity.backend.tables.LoyaltyAccount.LOYALTY_ACCOUNT;

@Repository
public class LoyaltyAccountRepository {
    private final DSLContext dsl;

    public LoyaltyAccountRepository(DSLContext dsl) { this.dsl = dsl; }

    public LoyaltyAccount getOrCreate(Integer customerId) {
        dsl.insertInto(LOYALTY_ACCOUNT)
                .set(LOYALTY_ACCOUNT.CUSTOMER_ID, customerId)
                .onDuplicateKeyIgnore()
                .execute();
        return dsl.selectFrom(LOYALTY_ACCOUNT)
                .where(LOYALTY_ACCOUNT.CUSTOMER_ID.eq(customerId))
                .fetchOneInto(LoyaltyAccount.class);
    }

    public LoyaltyAccount lockByCustomerId(Integer customerId) {
        return dsl.selectFrom(LOYALTY_ACCOUNT)
                .where(LOYALTY_ACCOUNT.CUSTOMER_ID.eq(customerId))
                .forUpdate()
                .fetchOneInto(LoyaltyAccount.class);
    }

    public int applyDelta(Integer accountId, long balanceDelta, long rankDelta, Integer tierId) {
        return dsl.update(LOYALTY_ACCOUNT)
                .set(LOYALTY_ACCOUNT.BALANCE, LOYALTY_ACCOUNT.BALANCE.plus(balanceDelta))
                .set(LOYALTY_ACCOUNT.RANK_POINTS, LOYALTY_ACCOUNT.RANK_POINTS.plus(rankDelta))
                .set(LOYALTY_ACCOUNT.TIER_ID, tierId)
                .set(LOYALTY_ACCOUNT.VERSION, LOYALTY_ACCOUNT.VERSION.plus(1))
                .where(LOYALTY_ACCOUNT.ID.eq(accountId))
                .execute();
    }
}
