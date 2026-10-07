package vn.aeoc.crm.repository.customer;

import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.CustomerAttribute;
import vn.entity.backend.tables.records.CustomerAttributeRecord;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static vn.entity.backend.tables.CustomerAttribute.CUSTOMER_ATTRIBUTE;

@Repository
public class CustomerAttributeRepository extends AbsMysqlRepository<
        CustomerAttributeRecord,
        CustomerAttribute,
        Integer> {
    @Override
    protected TableImpl<CustomerAttributeRecord> getTable() {
        return CUSTOMER_ATTRIBUTE;
    }

    private Condition getWhereCondition(Integer isParent, Integer isTitle, String name, String dataType, Integer branchId) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(
                    CUSTOMER_ATTRIBUTE.BRANCH_ID.eq(branchId)
            );
        }

        if (!StringUtils.isBlank(name)) {
            condition = condition.and(DSL.lower(CUSTOMER_ATTRIBUTE.NAME)
                    .like(DSL.val("%" + name.trim().toLowerCase() + "%")));
        }

        if (!StringUtils.isBlank(dataType)) {
            condition = condition.and(CUSTOMER_ATTRIBUTE.DATATYPE.eq(dataType));
        }

        if (CommonUtils.NVL(isParent) == 1) {
            condition = condition.and(CUSTOMER_ATTRIBUTE.PARENT_ID.eq(0));
        }

        if (CommonUtils.NVL(isParent) == 2) {
            condition = condition.and(CUSTOMER_ATTRIBUTE.PARENT_ID.greaterThan(0));
        }

        return condition;
    }

    public List<CustomerAttribute> getCustomerAttributeByCriteria(
            Integer isParent,
            Integer isTitle,
            String name,
            String dataType,
            Integer branchId,
            Pageable pageable) {

        Condition condition = getWhereCondition(isParent, isTitle, name, dataType, branchId);

        // Tạo alias cho bảng cha
        Table<?> parent = CUSTOMER_ATTRIBUTE.as("parent");

        // SELECT thêm parent_name
        SelectConditionStep<Record> select = dslContext
                .select(getFields())
                .select(parent.field("name", String.class).as("parent_name"))
                .from(CUSTOMER_ATTRIBUTE)
                .leftJoin(parent)
                .on(CUSTOMER_ATTRIBUTE.PARENT_ID.eq(parent.field("id", Integer.class)))
                .where(condition);

        select.orderBy(CUSTOMER_ATTRIBUTE.POSITION.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getLimit());

        return select.fetchInto(CustomerAttribute.class);
    }

    public Long countCustomerAttributeByCriteria(Integer isParent,
                                                 Integer isTitle,
                                                 String name, String dataType, Integer branchId) {
        Condition condition = getWhereCondition(isParent, isTitle, name, dataType, branchId);
        return dslContext
                .selectCount()
                .from(CUSTOMER_ATTRIBUTE)
                .where(condition)
                .fetchOne(0, Long.class);
    }

    private Condition getWhereCondition(Integer id, String fieldName) {
        Condition condition = DSL.trueCondition();

        if (!CommonUtils.NVL(fieldName).isEmpty()) {
            condition = condition.and(CUSTOMER_ATTRIBUTE.FIELD_NAME.likeRegex(fieldName));
        }

        if (CommonUtils.NVL(id) > 0) {
            condition = condition.and(CUSTOMER_ATTRIBUTE.ID.notEqual(id));
        }
        return condition;
    }

    public Map<Integer, List<CustomerAttribute>> getAttributeMap(Integer branchId) {

        var parent = CUSTOMER_ATTRIBUTE.as("parent");

        List<Field<?>> fields = mergeFields(
                parent.NAME.as("parentName")
        );

        Table<?> from = CUSTOMER_ATTRIBUTE
                .leftJoin(parent)
                .on(CUSTOMER_ATTRIBUTE.PARENT_ID.eq(parent.ID));

        Condition cond = CUSTOMER_ATTRIBUTE.PARENT_ID.gt(0);
        if (CommonUtils.NVL(branchId) > 0) {
            cond = cond.and(CUSTOMER_ATTRIBUTE.BRANCH_ID.eq(branchId));
        }

        List<CustomerAttribute> list = getListByCriteriaWithJoin(
                fields,
                from,
                cond,
                CUSTOMER_ATTRIBUTE.POSITION.asc()
        );

        return list.stream()
                .collect(Collectors.groupingBy(CustomerAttribute::getParentId));
    }

}
