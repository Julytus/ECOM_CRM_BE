package vn.aeoc.crm.repository.loyalty;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTier;

import java.util.List;

import static vn.entity.backend.tables.LoyaltyTier.LOYALTY_TIER;

@Repository
public class LoyaltyTierRepository {
    private final DSLContext dsl;

    public LoyaltyTierRepository(DSLContext dsl) { this.dsl = dsl; }

    public List<LoyaltyTier> listActiveOrderedByThreshold() {
        return dsl.selectFrom(LOYALTY_TIER)
                .where(LOYALTY_TIER.STATUS.eq(1))
                .orderBy(LOYALTY_TIER.MIN_RANK_POINTS.asc())
                .fetchInto(LoyaltyTier.class);
    }

    public LoyaltyTier findHighestTier(long rankPoints) {
        return dsl.selectFrom(LOYALTY_TIER)
                .where(LOYALTY_TIER.STATUS.eq(1).and(LOYALTY_TIER.MIN_RANK_POINTS.le(rankPoints)))
                .orderBy(LOYALTY_TIER.MIN_RANK_POINTS.desc())
                .limit(1)
                .fetchOneInto(LoyaltyTier.class);
    }
}
