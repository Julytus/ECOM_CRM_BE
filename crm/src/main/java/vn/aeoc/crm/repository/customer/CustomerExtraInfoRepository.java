package vn.aeoc.crm.repository.customer;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.entity.data.mysql.customer.CustomerExtraInfo;
import vn.entity.backend.tables.records.CustomerExtraInfoRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static vn.entity.backend.tables.CustomerAttribute.CUSTOMER_ATTRIBUTE;
import static vn.entity.backend.tables.CustomerExtraInfo.CUSTOMER_EXTRA_INFO;

@Repository
public class CustomerExtraInfoRepository extends AbsMysqlRepository<
        CustomerExtraInfoRecord,
        CustomerExtraInfo,
        Integer> {
    public CustomerExtraInfoRepository(DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    protected TableImpl<CustomerExtraInfoRecord> getTable() {
        return CUSTOMER_EXTRA_INFO;
    }

    public List<CustomerExtraInfo> getCustomerExtraInfoByCustomerId(Integer customerId) {
        List<Field<?>> selectedFields = new ArrayList<>(getFields());
        selectedFields.addAll(asList(
                CUSTOMER_ATTRIBUTE.DATATYPE.as("datatype"),
                CUSTOMER_ATTRIBUTE.NAME.as("attribute_name"),
                CUSTOMER_ATTRIBUTE.ATTRIBUTES
        ));

        return dslContext
                .select(selectedFields)
                .from(CUSTOMER_EXTRA_INFO)
                .join(CUSTOMER_ATTRIBUTE)
                .on(CUSTOMER_EXTRA_INFO.ATTRIBUTE_ID.eq(CUSTOMER_ATTRIBUTE.ID))
                .where(CUSTOMER_EXTRA_INFO.CUSTOMER_ID.eq(customerId))
                .orderBy(CUSTOMER_EXTRA_INFO.ATTRIBUTE_ID.asc())
                .fetchInto(CustomerExtraInfo.class);
    }

    public Map<Integer, List<CustomerExtraInfo>> getExtraInfoByCustomerIds(List<Integer> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Map.of();
        }

        List<Field<?>> fields = mergeFields(
                CUSTOMER_ATTRIBUTE.DATATYPE.as("datatype"),
                CUSTOMER_ATTRIBUTE.FIELD_NAME.as("field_name"),
                CUSTOMER_ATTRIBUTE.NAME.as("attribute_name"),
                CUSTOMER_ATTRIBUTE.ATTRIBUTES
        );

        Table<?> from = CUSTOMER_EXTRA_INFO
                .join(CUSTOMER_ATTRIBUTE)
                .on(CUSTOMER_EXTRA_INFO.ATTRIBUTE_ID.eq(CUSTOMER_ATTRIBUTE.ID));

        List<CustomerExtraInfo> list = getListByCriteriaWithJoin(
                fields,
                from,
                CUSTOMER_EXTRA_INFO.CUSTOMER_ID.in(customerIds),
                CUSTOMER_EXTRA_INFO.ATTRIBUTE_ID.asc()
        );

        return list.stream()
                .collect(Collectors.groupingBy(CustomerExtraInfo::getCustomerId));
    }

    /**
     * Bulk upsert by unique key (customer_id, attribute_id).
     * Requires a unique index on (customer_id, attribute_id).
     */
    public Integer upsertBatchCustomerExtraInfo(List<CustomerExtraInfo> list, Integer customerId) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int affected = 0;
        for (CustomerExtraInfo info : list) {
            affected += dslContext.insertInto(
                            CUSTOMER_EXTRA_INFO,
                            CUSTOMER_EXTRA_INFO.ATTRIBUTE_ID,
                            CUSTOMER_EXTRA_INFO.CUSTOMER_ID,
                            CUSTOMER_EXTRA_INFO.ATTRIBUTE_VALUE
                    )
                    .values(
                            DSL.value(info.getAttributeId()),
                            DSL.value(customerId),
                            DSL.value(info.getAttributeValue())
                    )
                    .onDuplicateKeyUpdate()
                    .set(CUSTOMER_EXTRA_INFO.ATTRIBUTE_VALUE, DSL.value(info.getAttributeValue()))
                    .execute();
        }
        return affected;
    }

    /**
     * Xóa theo danh sách attributeId
     */
    public Integer deleteCustomerExtraInfos(List<CustomerExtraInfo> list, Integer customerId) {
        if (list == null || list.isEmpty()) return 0;

        List<Integer> attributeIds = list.stream()
                .map(CustomerExtraInfo::getAttributeId)
                .collect(Collectors.toList());

        return dslContext
                .delete(CUSTOMER_EXTRA_INFO)
                .where(CUSTOMER_EXTRA_INFO.CUSTOMER_ID.eq(customerId)
                        .and(CUSTOMER_EXTRA_INFO.ATTRIBUTE_ID.in(attributeIds)))
                .execute();
    }

    public Integer deleteCustomerExtraInfoByCustomerId(Integer customerId) {
        return dslContext
                .delete(CUSTOMER_EXTRA_INFO)
                .where(CUSTOMER_EXTRA_INFO.CUSTOMER_ID.eq(customerId))
                .execute();
    }

    public boolean existsByAttributeIdAndValue(Integer branchId, Integer attributeId, String attributeValue, Integer excludedCustomerId) {
        if (branchId == null || attributeId == null || attributeValue == null) {
            return false;
        }

        var condition = CUSTOMER_EXTRA_INFO.ATTRIBUTE_ID.eq(attributeId)
                .and(CUSTOMER_EXTRA_INFO.ATTRIBUTE_VALUE.eq(attributeValue))
                .and(CUSTOMER_ATTRIBUTE.BRANCH_ID.eq(branchId));

        if (excludedCustomerId != null) {
            condition = condition.and(CUSTOMER_EXTRA_INFO.CUSTOMER_ID.ne(excludedCustomerId));
        }

        return dslContext
                .fetchExists(
                        dslContext.selectOne()
                                .from(CUSTOMER_EXTRA_INFO)
                                .join(CUSTOMER_ATTRIBUTE)
                                .on(CUSTOMER_EXTRA_INFO.ATTRIBUTE_ID.eq(CUSTOMER_ATTRIBUTE.ID))
                                .where(condition)
                );
    }
}
