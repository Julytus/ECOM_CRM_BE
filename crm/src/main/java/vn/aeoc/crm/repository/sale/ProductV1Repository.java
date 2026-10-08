package vn.aeoc.crm.repository.sale;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.sale.ProductV1;
import vn.entity.backend.tables.records.ProductV1Record;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static vn.entity.backend.tables.CategoryItem.CATEGORY_ITEM;
import static vn.entity.backend.tables.ProductV1.PRODUCT_V1;
import static vn.entity.backend.tables.Unit.UNIT;

@Repository
public class ProductV1Repository extends AbsMysqlRepository<ProductV1Record, ProductV1, Integer> {

    @Override
    protected TableImpl<ProductV1Record> getTable() {
        return PRODUCT_V1;
    }

    private static List<Field<?>> getProductFields() {
        return asList(
                PRODUCT_V1.ID,
                PRODUCT_V1.NAME,
                PRODUCT_V1.CATEGORY_ID,
                PRODUCT_V1.CONTENT,
                PRODUCT_V1.CODE,
                PRODUCT_V1.AVATAR,
                PRODUCT_V1.PRICE,
                PRODUCT_V1.DISCOUNT,
                PRODUCT_V1.DISCOUNT_UNIT,
                PRODUCT_V1.POSITION,
                PRODUCT_V1.STATUS,
                PRODUCT_V1.UNIT_ID,
                PRODUCT_V1.TYPE,
                PRODUCT_V1.EXPIRED_PERIOD,
                PRODUCT_V1.BRANCH_ID
        );
    }

    private Condition getWhereCondition(String keyword, Integer categoryId, Integer status, Integer type, Integer branchId) {
        Condition condition = DSL.trueCondition();

        condition = condition.and(BranchFilter.branchEq(PRODUCT_V1.BRANCH_ID, CommonUtils.NVL(branchId)));

        if (!StringUtils.isBlank(keyword)) {
            condition = condition.and(DSL.lower(PRODUCT_V1.NAME).like("%" + keyword.trim().toLowerCase() + "%"));
        }

        if (categoryId != null && categoryId > 0) {
            condition = condition.and(PRODUCT_V1.CATEGORY_ID.eq(categoryId));
        }

        if (status != null && status >= 0) {
            condition = condition.and(PRODUCT_V1.STATUS.eq(status));
        }

        if (type != null && type > 0) {
            condition = condition.and(PRODUCT_V1.TYPE.eq(type));
        }

        return condition;
    }

    public List<ProductV1> getProductByCriteria(String keyword, Integer categoryId, Integer status, Integer type, Integer branchId, Pageable pageable) {
        var c = CATEGORY_ITEM.as("c");
        var u = UNIT.as("u");

        List<Field<?>> fields = new ArrayList<>(getProductFields());
        fields.add(c.NAME.as("categoryName"));
        fields.add(u.NAME.as("unitName"));

        Condition condition = getWhereCondition(keyword, categoryId, status, type, branchId);

        SelectConditionStep<?> select = dslContext.select(fields)
                .from(PRODUCT_V1)
                .leftJoin(c).on(c.ID.eq(PRODUCT_V1.CATEGORY_ID))
                .leftJoin(u).on(u.ID.eq(PRODUCT_V1.UNIT_ID))
                .where(condition);

        return select.orderBy(PRODUCT_V1.POSITION.asc(), PRODUCT_V1.ID.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getLimit())
                .fetchInto(ProductV1.class);
    }

    public Long countProductByCriteria(String keyword, Integer categoryId, Integer status, Integer type, Integer branchId) {
        Condition condition = getWhereCondition(keyword, categoryId, status, type, branchId);

        return dslContext.selectCount()
                .from(PRODUCT_V1)
                .where(condition)
                .fetchOne(0, Long.class);
    }

    @Override
    public ProductV1 getById(Integer id) {
        var c = CATEGORY_ITEM.as("c");
        var u = UNIT.as("u");

        List<Field<?>> fields = new ArrayList<>(getProductFields());
        fields.add(c.NAME.as("categoryName"));
        fields.add(u.NAME.as("unitName"));

        return dslContext.select(fields)
                .from(PRODUCT_V1)
                .leftJoin(c).on(c.ID.eq(PRODUCT_V1.CATEGORY_ID))
                .leftJoin(u).on(u.ID.eq(PRODUCT_V1.UNIT_ID))
                .where(PRODUCT_V1.ID.eq(id))
                .fetchOptionalInto(ProductV1.class)
                .orElse(new ProductV1());
    }
}
