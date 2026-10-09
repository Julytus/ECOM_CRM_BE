package vn.aeoc.crm.service.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.product.ProductRepository;
import vn.aeoc.entity.data.mysql.product.Product;

@Slf4j(topic = "PRODUCT-SERVICE")
@Service
public class ProductService extends BaseService<
        ProductRepository,
        Product,
        Integer> {

    public ProductService(ProductRepository repository) {
        super(repository);
    }

    public Page<Product> getByCriteria(
            Integer id,
            String name,
            Integer status,
            Integer categoryId,
            Integer branchId,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(id, name, status, categoryId, branchId, pageable),
                () -> repository.countByCriteria(id, name, status, categoryId, branchId)
        );
    }
}
