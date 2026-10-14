package vn.aeoc.crm.repository.product;

import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.product.ProductVariantAttribute;
import vn.entity.backend.tables.records.ProductVariantAttributeRecord;

import java.util.List;

import static vn.entity.backend.tables.ProductVariantAttribute.PRODUCT_VARIANT_ATTRIBUTE;

@Repository
public class ProductVariantAttributeRepository
        extends AbsMysqlRepository<ProductVariantAttributeRecord, ProductVariantAttribute, Integer> {
    public ProductVariantAttributeRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<ProductVariantAttributeRecord> getTable() {
        return PRODUCT_VARIANT_ATTRIBUTE;
    }

    private Condition getWhereCondition(
            Integer variantId,
            Integer attributeValueId
    ) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(variantId) > 0) {
            condition = condition.and(PRODUCT_VARIANT_ATTRIBUTE.VARIANT_ID.eq(variantId));
        }

        if (CommonUtils.NVL(attributeValueId) > 0) {
            condition = condition.and(PRODUCT_VARIANT_ATTRIBUTE.ATTRIBUTE_VALUE_ID.eq(attributeValueId));
        }

        return condition;
    }

    public List<ProductVariantAttribute> getByCriteria(
            Integer variantId,
            Integer attributeValueId,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(variantId, attributeValueId);
        return getListByCriteriaWithJoin(
                mergeFields(),
                PRODUCT_VARIANT_ATTRIBUTE,
                condition,
                PRODUCT_VARIANT_ATTRIBUTE.VARIANT_ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            Integer variantId,
            Integer attributeValueId
    ) {
        Condition condition = getWhereCondition(variantId, attributeValueId);
        return getCountWithCriteria(condition);
    }

    public int deleteByVariantIdAndAttributeValueId(Integer variantId, Integer attributeValueId) {
        return dslContext.deleteFrom(PRODUCT_VARIANT_ATTRIBUTE)
                .where(PRODUCT_VARIANT_ATTRIBUTE.VARIANT_ID.eq(variantId))
                .and(PRODUCT_VARIANT_ATTRIBUTE.ATTRIBUTE_VALUE_ID.eq(attributeValueId))
                .execute();
    }
}
