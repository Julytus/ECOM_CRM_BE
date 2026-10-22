package vn.aeoc.crm.repository.loyalty;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static vn.entity.backend.tables.LoyaltyConfig.LOYALTY_CONFIG;

@Repository
public class LoyaltyConfigRepository {
    private final DSLContext dsl;

    public LoyaltyConfigRepository(DSLContext dsl) { this.dsl = dsl; }

    public Integer getEarnRate() {
        return dsl.select(LOYALTY_CONFIG.EARN_RATE)
                .from(LOYALTY_CONFIG)
                .where(LOYALTY_CONFIG.ID.eq(1))
                .fetchOne(LOYALTY_CONFIG.EARN_RATE);
    }
}
