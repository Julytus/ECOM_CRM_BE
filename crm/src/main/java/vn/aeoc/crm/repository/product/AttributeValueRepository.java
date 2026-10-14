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
import vn.aeoc.entity.data.mysql.product.AttributeValue;
import vn.entity.backend.tables.records.AttributeValueRecord;

import java.util.List;

import static vn.entity.backend.tables.AttributeValue.ATTRIBUTE_VALUE;

@Repository
public class AttributeValueRepository
        extends AbsMysqlRepository<AttributeValueRecord, AttributeValue, Integer> {
    public AttributeValueRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<AttributeValueRecord> getTable() {
        return ATTRIBUTE_VALUE;
    }

    private Condition getWhereCondition(
            Integer attributeId,
            String value
    ) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(attributeId) > 0) {
            condition = condition.and(ATTRIBUTE_VALUE.ATTRIBUTE_ID.eq(attributeId));
        }

        if (!StringUtils.isBlank(value)) {
            String kw = "%" + value.trim().toLowerCase() + "%";
            condition = condition.and(DSL.lower(ATTRIBUTE_VALUE.VALUE).like(kw));
        }

        return condition;
    }

    public List<AttributeValue> getByCriteria(
            Integer attributeId,
            String value,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(attributeId, value);
        return getListByCriteriaWithJoin(
                mergeFields(),
                ATTRIBUTE_VALUE,
                condition,
                ATTRIBUTE_VALUE.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            Integer attributeId,
            String value
    ) {
        Condition condition = getWhereCondition(attributeId, value);
        return getCountWithCriteria(condition);
    }
}
