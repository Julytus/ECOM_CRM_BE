package vn.aeoc.crm.service.sale;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.sale.ServiceItemRepository;
import vn.aeoc.entity.data.mysql.ServiceItem;

@Slf4j(topic = "SERVICE-SERVICE")
@Service
public class ServiceItemService extends BaseService<
        ServiceItemRepository,
        ServiceItem,
        Integer> {

    public ServiceItemService(ServiceItemRepository repository) {
        super(repository);
    }

    public Page<ServiceItem> getByCriteria(
            String keyword,
            Integer categoryId,
            Integer isCombo,
            Integer featured,
            Integer status,
            Integer branchId,
            Pageable pageable
    ) {

        return pageQuery(
                pageable,
                () -> repository.getByCriteria(
                        keyword, categoryId, isCombo, featured, status,  branchId, pageable
                ),
                () -> repository.countByCriteria(
                        keyword, categoryId, isCombo, featured, status,  branchId
                )
        );
    }
}