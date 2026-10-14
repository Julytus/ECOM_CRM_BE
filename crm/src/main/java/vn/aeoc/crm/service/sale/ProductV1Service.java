package vn.aeoc.crm.service.sale;

import vn.aeoc.base.util.QueryExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.sale.ProductV1Repository;
import vn.aeoc.entity.data.mysql.sale.ProductV1;

@Slf4j(topic = "PRODUCT_V1-SERVICE")
@Service
public class ProductV1Service extends BaseService<
        ProductV1Repository,
        ProductV1,
        Integer> {

    protected ProductV1Service(ProductV1Repository repository, QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
    }

    public Page<ProductV1> getListProductByCriteria(
            String keyword, Integer categoryId, Integer status, Integer type, Integer branchId, Pageable pageable) {
        return pageQuery(
                pageable,
                () -> repository.getProductByCriteria(keyword, categoryId, status, type, branchId, pageable),
                () -> repository.countProductByCriteria(keyword, categoryId, status, type, branchId)
        );
    }
}
