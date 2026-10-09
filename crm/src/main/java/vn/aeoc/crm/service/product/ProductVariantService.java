package vn.aeoc.crm.service.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.product.ProductVariantRepository;
import vn.aeoc.entity.data.mysql.product.ProductVariant;

@Slf4j(topic = "PRODUCT-VARIANT-SERVICE")
@Service
public class ProductVariantService extends BaseService<
        ProductVariantRepository,
        ProductVariant,
        Integer> {

    public ProductVariantService(ProductVariantRepository repository) {
        super(repository);
    }

    public Page<ProductVariant> getByCriteria(
            Integer productId,
            String sku,
            Integer minStock,
            Integer maxStock,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(productId, sku, minStock, maxStock, pageable),
                () -> repository.countByCriteria(productId, sku, minStock, maxStock)
        );
    }
}
