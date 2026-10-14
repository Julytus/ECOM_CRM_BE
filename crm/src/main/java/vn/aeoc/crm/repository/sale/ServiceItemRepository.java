package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.sale.ServiceItem;
import vn.entity.backend.tables.records.ServiceRecord;

import java.util.ArrayList;
import java.util.List;

import static vn.entity.backend.tables.CategoryItem.CATEGORY_ITEM;
import static vn.entity.backend.tables.Service.SERVICE;

@Repository
public class ServiceItemRepository
        extends AbsMysqlRepository<ServiceRecord, ServiceItem, Integer> {
    public ServiceItemRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<ServiceRecord> getTable() {
        return SERVICE;
    }

    /* ==============================
     * JOIN
     * ============================== */

    private List<Field<?>> getJoinFields() {
        return mergeFields(
                CATEGORY_ITEM.NAME.as("category_name")
        );
    }

    private Table<?> getJoinTable() {
        return SERVICE
                .leftJoin(CATEGORY_ITEM)
                .on(CATEGORY_ITEM.ID.eq(SERVICE.CATEGORY_ID));
    }

    /* ==============================
     * WHERE
     * ============================== */

    private Condition getWhereCondition(
            String keyword,
            Integer categoryId,
            Integer isCombo,
            Integer featured,
            Integer status,
            Integer branchId
    ) {

        Condition condition = DSL.trueCondition();

        condition = condition.and(
                BranchFilter.branchEq(SERVICE.BRANCH_ID, CommonUtils.NVL(branchId))
        );

        if (!StringUtils.isBlank(keyword)) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";

            condition = condition.and(
                    DSL.lower(SERVICE.NAME).like(kw)
                            .or(DSL.lower(SERVICE.CODE).like(kw))
            );
        }

        if (CommonUtils.NVL(categoryId) > 0) {
            condition = condition.and(SERVICE.CATEGORY_ID.eq(categoryId));
        }

        if (CommonUtils.NVL(status) > 0) {
            condition = condition.and(SERVICE.STATUS.eq(status));
        }

        if (isCombo != null && isCombo >= 0) {
            condition = condition.and(SERVICE.IS_COMBO.eq(isCombo));
        }

        if (featured != null && featured >= 0) {
            condition = condition.and(SERVICE.FEATURED.eq(featured));
        }

        return condition;
    }

    public List<ServiceItem> getByCriteria(
            String keyword,
            Integer categoryId,
            Integer isCombo,
            Integer featured,
            Integer status,
            Integer branchId,
            Pageable pageable
    ) {

        Condition condition = getWhereCondition(
                keyword, categoryId, isCombo, featured, status, branchId
        );

        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                condition,
                SERVICE.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            String keyword,
            Integer categoryId,
            Integer isCombo,
            Integer featured,
            Integer status,
            Integer branchId
    ) {

        Condition condition = getWhereCondition(
                keyword, categoryId, isCombo, featured, status, branchId
        );

        return getCountWithCriteria(condition);
    }

    @Override
    public ServiceItem getById(Integer id) {
        var c = CATEGORY_ITEM.as("c");

        List<Field<?>> fields = new ArrayList<>(getFields());
        fields.add(c.NAME.as("categoryName"));

        return dslContext.select(fields)
                .from(SERVICE)
                .leftJoin(c).on(c.ID.eq(SERVICE.CATEGORY_ID))
                .where(SERVICE.ID.eq(id))
                .fetchOptionalInto(ServiceItem.class)
                .orElse(new ServiceItem());
    }
}