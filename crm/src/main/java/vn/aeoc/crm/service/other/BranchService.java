package vn.aeoc.crm.service.other;

import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.other.BranchRepository;
import vn.aeoc.entity.data.mysql.Branch;

@Service
public class BranchService extends BaseService<
        BranchRepository,
        Branch,
        Integer> {

    public BranchService(BranchRepository repository) {
        super(repository);
    }

    public Page<Branch> getByCriteria(
            Integer id,
            String name,
            Integer status,
            Pageable pageable
    ) {

        return pageQuery(
                pageable,
                () -> repository.getByCriteria(id, name, status, pageable),
                () -> repository.countByCriteria(id, name, status)
        );
    }

    public Integer updateStatus(Integer id, Integer status) {
        return repository.updateStatus(id, status);
    }

}