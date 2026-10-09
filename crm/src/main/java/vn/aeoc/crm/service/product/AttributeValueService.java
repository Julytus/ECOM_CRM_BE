package vn.aeoc.crm.service.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.product.AttributeValueRepository;
import vn.aeoc.entity.data.mysql.product.AttributeValue;

@Slf4j(topic = "ATTRIBUTE-VALUE-SERVICE")
@Service
public class AttributeValueService extends BaseService<
        AttributeValueRepository,
        AttributeValue,
        Integer> {

    public AttributeValueService(AttributeValueRepository repository) {
        super(repository);
    }

    public Page<AttributeValue> getByCriteria(
            Integer attributeId,
            String value,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(attributeId, value, pageable),
                () -> repository.countByCriteria(attributeId, value)
        );
    }
}
