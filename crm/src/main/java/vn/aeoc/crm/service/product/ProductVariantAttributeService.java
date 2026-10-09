package vn.aeoc.crm.service.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.product.ProductVariantAttributeRepository;
import vn.aeoc.entity.data.mysql.product.ProductVariantAttribute;

@Slf4j(topic = "PRODUCT-VARIANT-ATTRIBUTE-SERVICE")
@Service
public class ProductVariantAttributeService extends BaseService<
        ProductVariantAttributeRepository,
        ProductVariantAttribute,
        Integer> {

    public ProductVariantAttributeService(ProductVariantAttributeRepository repository) {
        super(repository);
    }

    public Page<ProductVariantAttribute> getByCriteria(
            Integer variantId,
            Integer attributeValueId,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(variantId, attributeValueId, pageable),
                () -> repository.countByCriteria(variantId, attributeValueId)
        );
    }

    public int deleteByCompositeKey(Integer variantId, Integer attributeValueId) {
        return repository.deleteByVariantIdAndAttributeValueId(variantId, attributeValueId);
    }
}
