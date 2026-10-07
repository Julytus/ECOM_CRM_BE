package vn.aeoc.crm.service.customer;

import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.customer.CustomerAttributeRepository;
import vn.aeoc.entity.data.mysql.CustomerAttribute;

import java.util.List;
import java.util.Map;

@Service
public class CustomerAttributeService extends BaseService<
        CustomerAttributeRepository,
        CustomerAttribute,
        Integer> {

    public CustomerAttributeService(CustomerAttributeRepository repository) {
        super(repository);
    }

    public Page<CustomerAttribute> getByCriteria(
            Integer isParent,
            Integer isTitle,
            String name,
            String dataType,
            Integer branchId,
            Pageable pageable
    ) {

        return pageQuery(
                pageable,
                () -> repository.getCustomerAttributeByCriteria(isParent, isTitle, name, dataType, branchId, pageable),
                () -> repository.countCustomerAttributeByCriteria(isParent, isTitle, name, dataType, branchId)
        );
    }

    public Map<Integer, List<CustomerAttribute>> getAttributeMap(Integer branchId) {
        return repository.getAttributeMap(branchId);
    }
}
