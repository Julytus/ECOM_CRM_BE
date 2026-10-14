package vn.aeoc.crm.service.sale;

import vn.aeoc.base.util.QueryExecutor;

import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.sale.CampaignSnapshotRepository;
import vn.aeoc.entity.data.mysql.customer.CampaignSnapshot;

@Service
public class CampaignSnapshotService extends BaseService<CampaignSnapshotRepository, CampaignSnapshot, Integer> {
    protected CampaignSnapshotService(CampaignSnapshotRepository repository, QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
    }

    public Page<CampaignSnapshot> getByCriteria(Integer customerId, Pageable pageable) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(customerId, pageable),
                () -> repository.countByCriteria(customerId)
        );
    }

    public CampaignSnapshot getLoading(Integer customerId) {

        var lst = repository.getLoadingCampaign(customerId);
        if (lst.isEmpty()) {
            return null;
        }
        return lst.getFirst();
    }

    public Integer deleteCampaign(Integer id) {
        return repository.delete(id);
    }
}
