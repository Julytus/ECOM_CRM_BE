package vn.aeoc.crm.repository.sale;

import org.jooq.Field;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.entity.data.mysql.CampaignSnapshot;
import vn.entity.backend.tables.records.CampaignSnapshotRecord;

import java.util.Arrays;
import java.util.List;

import static vn.entity.backend.tables.CampaignSnapshot.CAMPAIGN_SNAPSHOT;

@Repository
public class CampaignSnapshotRepository extends AbsMysqlRepository<CampaignSnapshotRecord, CampaignSnapshot, Integer> {
    @Override
    protected TableImpl<CampaignSnapshotRecord> getTable() {
        return CAMPAIGN_SNAPSHOT;
    }

    private List<Field<?>> getMyFields() {
        return Arrays.asList(
                CAMPAIGN_SNAPSHOT.CAMPAIGN,
                CAMPAIGN_SNAPSHOT.ID,
                CAMPAIGN_SNAPSHOT.CREATED_TIME,
                CAMPAIGN_SNAPSHOT.CUSTOMER_ID,
                CAMPAIGN_SNAPSHOT.STATUS
        );
    }

    public List<CampaignSnapshot> getByCriteria(
            Integer customerId,
            Pageable pageable
    ) {
        var select = dslContext.select(getMyFields())
                .from(CAMPAIGN_SNAPSHOT)
                .where(CAMPAIGN_SNAPSHOT.CUSTOMER_ID.eq(customerId))
                .orderBy(CAMPAIGN_SNAPSHOT.ID.desc())
                .limit(pageable.getLimit())
                .offset(pageable.getOffset());
        return select.fetchInto(CampaignSnapshot.class);
    }

    public Long countByCriteria(Integer customerId) {
        return getCountWithCriteria(
                CAMPAIGN_SNAPSHOT.CUSTOMER_ID.eq(customerId)
        );
    }

    public boolean existLoading(Integer customerId) {
        return exists(CAMPAIGN_SNAPSHOT.CUSTOMER_ID.eq(customerId).and(CAMPAIGN_SNAPSHOT.STATUS.eq(0)));
    }

    public List<CampaignSnapshot> getLoadingCampaign(
            Integer customerId
    ) {
        Pageable pageable = new Pageable();
        pageable.setLimit(1);
        return getListByCriteria(CAMPAIGN_SNAPSHOT.CUSTOMER_ID.eq(customerId).and(CAMPAIGN_SNAPSHOT.STATUS.eq(0)),
                pageable);
    }
}
