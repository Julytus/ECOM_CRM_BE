package vn.aeoc.crm.service.customer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.customer.CustomerSourceRepository;
import vn.aeoc.entity.data.mysql.CustomerSource;

@Slf4j(topic = "CUSTOMER-SOURCE-SERVICE")
@Service
public class CustomerSourceService extends
        BaseService<CustomerSourceRepository, CustomerSource, Integer> {


    protected CustomerSourceService(CustomerSourceRepository repository) {
        super(repository);
    }

    public Page<CustomerSource> getByCriteria(
            String name,
            Integer status,
            Integer branchId,
            Pageable pageable) {


        return pageQuery(
                pageable,
                () -> repository.getByCriteria(name, status, branchId, pageable),
                () -> repository.countByCriteria(name, status, branchId)
        );
    }

    public Integer updateStatus(Integer id, Integer status) {
        return repository.updateStatus(id, status);
    }

}
