package vn.aeoc.crm.service.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.product.ProductAttributeRepository;
import vn.aeoc.entity.data.mysql.product.ProductAttribute;

@Slf4j(topic = "PRODUCT-ATTRIBUTE-SERVICE")
@Service
public class ProductAttributeService extends BaseService<
        ProductAttributeRepository,
        ProductAttribute,
        Integer> {

    public ProductAttributeService(ProductAttributeRepository repository) {
        super(repository);
    }

    public Page<ProductAttribute> getByCriteria(
            String fieldName,
            String name,
            Integer productId,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(fieldName, name, productId, pageable),
                () -> repository.countByCriteria(fieldName, name, productId)
        );
    }
}
