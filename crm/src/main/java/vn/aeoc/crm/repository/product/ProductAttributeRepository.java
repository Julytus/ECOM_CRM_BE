package vn.aeoc.crm.repository.product;

import org.jooq.DSLContext;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.product.ProductAttribute;
import vn.entity.backend.tables.records.ProductAttributeRecord;

import java.util.List;

import static vn.entity.backend.tables.ProductAttribute.PRODUCT_ATTRIBUTE;

@Repository
public class ProductAttributeRepository
        extends AbsMysqlRepository<ProductAttributeRecord, ProductAttribute, Integer> {
    public ProductAttributeRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<ProductAttributeRecord> getTable() {
        return PRODUCT_ATTRIBUTE;
    }

    private Condition getWhereCondition(
            String fieldName,
            String name,
            Integer productId
    ) {
        Condition condition = DSL.trueCondition();

        if (!StringUtils.isBlank(fieldName)) {
            String kw = "%" + fieldName.trim().toLowerCase() + "%";
            condition = condition.and(DSL.lower(PRODUCT_ATTRIBUTE.FIELD_NAME).like(kw));
        }

        if (!StringUtils.isBlank(name)) {
            String kw = "%" + name.trim().toLowerCase() + "%";
            condition = condition.and(DSL.lower(PRODUCT_ATTRIBUTE.NAME).like(kw));
        }

        if (CommonUtils.NVL(productId) > 0) {
            condition = condition.and(PRODUCT_ATTRIBUTE.PRODUCT_ID.eq(productId));
        }

        return condition;
    }

    public List<ProductAttribute> getByCriteria(
            String fieldName,
            String name,
            Integer productId,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(fieldName, name, productId);
        return getListByCriteriaWithJoin(
                mergeFields(),
                PRODUCT_ATTRIBUTE,
                condition,
                PRODUCT_ATTRIBUTE.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            String fieldName,
            String name,
            Integer productId
    ) {
        Condition condition = getWhereCondition(fieldName, name, productId);
        return getCountWithCriteria(condition);
    }
}
