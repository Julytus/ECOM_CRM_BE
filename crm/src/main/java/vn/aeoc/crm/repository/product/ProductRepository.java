package vn.aeoc.crm.repository.product;

import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.data.request.ProductListRequest;
import vn.aeoc.crm.data.response.ProductDetailRow;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.product.Product;
import vn.entity.backend.tables.records.ProductRecord;

import java.util.List;

import static vn.entity.backend.tables.AttributeValue.ATTRIBUTE_VALUE;
import static vn.entity.backend.tables.CategoryItem.CATEGORY_ITEM;
import static vn.entity.backend.tables.Product.PRODUCT;
import static vn.entity.backend.tables.ProductAttribute.PRODUCT_ATTRIBUTE;
import static vn.entity.backend.tables.ProductVariant.PRODUCT_VARIANT;

@Repository
public class ProductRepository extends AbsMysqlRepository<ProductRecord, Product, Integer> {
    @Override
    protected TableImpl<ProductRecord> getTable() {
        return PRODUCT;
    }

    private Condition getWhereCondition(ProductListRequest r) {

        Condition condition = DSL.trueCondition();

        if (StringUtils.isNotBlank(r.getKeyword())) {
            condition = condition.and(
                    DSL.lower(PRODUCT.NAME).like("%" + r.getKeyword().trim().toLowerCase() + "%")
            );
        }

        if (r.getCategoryId() != null && r.getCategoryId() > 0) {
            condition = condition.and(PRODUCT.CATEGORY_ID.eq(r.getCategoryId()));
        }

        if (r.getIds() != null && !r.getIds().isEmpty()) {
            condition = condition.and(PRODUCT.ID.in(r.getIds()));
        }

        if (r.getAvgRatingFrom() != null) {
            condition = condition.and(PRODUCT.AVG_RATING.ge(r.getAvgRatingFrom()));
        }

        if (r.getAvgRatingTo() != null) {
            condition = condition.and(PRODUCT.AVG_RATING.le(r.getAvgRatingTo()));
        }

        if (r.getActive() != null) {
            condition = condition.and(PRODUCT.STATUS.eq(r.getActive()));
        }

        return condition;
    }

    private Condition getWhereCondition(Integer id, String name, Integer status,
                                        Integer categoryId, Integer branchId) {

        Condition condition = DSL.trueCondition();

        condition = condition.and(BranchFilter.branchEq(PRODUCT.BRANCH_ID, CommonUtils.NVL(branchId)));

        if (CommonUtils.NVL(id) > 0) {
            condition = condition.and(PRODUCT.ID.eq(id));
        }

        if (CommonUtils.NVL(categoryId) > 0) {
            condition = condition.and(PRODUCT.CATEGORY_ID.eq(categoryId));
        }

        if (!CommonUtils.NVL(name).isEmpty()) {
            condition = condition.and(
                    PRODUCT.NAME.likeIgnoreCase("%" + name.trim() + "%")
            );
        }

        if (CommonUtils.NVL(status) > 0) {
            condition = condition.and(PRODUCT.STATUS.eq(status));
        }

        return condition;
    }

    private List<Field<?>> getJoinFields() {
        return mergeFields(
                CATEGORY_ITEM.NAME.as("category_name")
        );
    }

    private Table<?> getJoinTable() {
        return PRODUCT.leftJoin(CATEGORY_ITEM).on(PRODUCT.CATEGORY_ID.eq(CATEGORY_ITEM.ID));
    }

    public List<Product> getByCriteria(
            Integer id,
            String name,
            Integer status,
            Integer categoryId,
            Integer branchId,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(id, name, status, categoryId, branchId);
        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                condition,
                PRODUCT.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            Integer id,
            String name,
            Integer status,
            Integer categoryId,
            Integer branchId
    ) {
        Condition condition = getWhereCondition(id, name, status, categoryId, branchId);
        return getCountWithCriteria(condition);
    }

    public List<Product> getByCriteria(
            ProductListRequest request, Pageable pageable) {

        List<Field<?>> fields = mergeFields(
                DSL.coalesce(
                        DSL.min(PRODUCT_VARIANT.PRICE),
                        DSL.inline(0.0)).as("min_price"),
                DSL.coalesce(DSL.max(PRODUCT_VARIANT.PRICE),
                        DSL.inline(0.0)).as("max_price"),
                DSL.coalesce(DSL.min(PRODUCT_VARIANT.ORIGINAL_PRICE),
                        DSL.inline(0.0)).as("original_min_price"),
                DSL.coalesce(DSL.max(PRODUCT_VARIANT.ORIGINAL_PRICE),
                        DSL.inline(0.0)).as("original_max_price"),
                CATEGORY_ITEM.NAME.as("category_name")
        );

        return dslContext.select(fields)
                .from(PRODUCT)
                .leftJoin(PRODUCT_VARIANT).on(PRODUCT_VARIANT.PRODUCT_ID.eq(PRODUCT.ID))
                .leftJoin(CATEGORY_ITEM).on(CATEGORY_ITEM.ID.eq(PRODUCT.CATEGORY_ID))
                .where(getWhereCondition(request))
                .groupBy(PRODUCT.ID)
                .orderBy(getSort(request))
                .offset(pageable.getOffset())
                .limit(pageable.getLimit())
                .fetchInto(Product.class);
    }

    private SortField<?> getSort(ProductListRequest r) {

        String sortBy = r.getSortBy();
        String sortDir = r.getSortDir();

        boolean asc = "asc".equalsIgnoreCase(sortDir);

        if ("minPrice".equalsIgnoreCase(sortBy)) {
            return asc ? DSL.min(PRODUCT_VARIANT.PRICE).asc() : DSL.min(PRODUCT_VARIANT.PRICE).desc();
        }

        if ("maxPrice".equalsIgnoreCase(sortBy)) {
            return asc ? DSL.max(PRODUCT_VARIANT.PRICE).asc() : DSL.max(PRODUCT_VARIANT.PRICE).desc();
        }

        if ("name".equalsIgnoreCase(sortBy)) {
            return asc ? PRODUCT.NAME.asc() : PRODUCT.NAME.desc();
        }

        if ("avgRating".equalsIgnoreCase(sortBy)) {
            return asc ? PRODUCT.AVG_RATING.asc() : PRODUCT.AVG_RATING.desc();
        }

        // default
        return PRODUCT.ID.desc();
    }

    public long countByCriteria(ProductListRequest request) {

        return dslContext.selectCount().from(
                dslContext.select(PRODUCT.ID).from(PRODUCT).leftJoin(PRODUCT_VARIANT).on(PRODUCT_VARIANT.PRODUCT_ID.eq(PRODUCT.ID)).where(getWhereCondition(request)).groupBy(PRODUCT.ID)
        ).fetchOne(0, long.class);
    }


    public List<ProductDetailRow> fetchProductDetailByProductIds(
            List<Integer> productIds) {

        return dslContext.select(
                PRODUCT.ID.as("product_id"), PRODUCT.NAME.as("product_name"), PRODUCT.DESCRIPTION.as("product_description"),
                PRODUCT_ATTRIBUTE.ID.as("attribute_id"), PRODUCT_ATTRIBUTE.FIELD_NAME.as("attribute_code"), PRODUCT_ATTRIBUTE.NAME.as("attribute_name"),
                ATTRIBUTE_VALUE.ID.as("value_id"), ATTRIBUTE_VALUE.VALUE.as("value")
        ).from(PRODUCT).join(PRODUCT_ATTRIBUTE).on(PRODUCT_ATTRIBUTE.PRODUCT_ID.eq(PRODUCT.ID)).join(ATTRIBUTE_VALUE).on(ATTRIBUTE_VALUE.ATTRIBUTE_ID.eq(PRODUCT_ATTRIBUTE.ID)).where(PRODUCT.ID.in(productIds)).orderBy(
                PRODUCT.ID.asc(), PRODUCT_ATTRIBUTE.ID.asc(), ATTRIBUTE_VALUE.ID.asc()
        ).fetchInto(ProductDetailRow.class);
    }

    public List<ProductDetailRow> fetchProductDetailByProductId(
            Integer productId) {

        SelectConditionStep<?> select = dslContext.select(
                PRODUCT.ID.as("product_id"), PRODUCT.NAME.as("product_name"), PRODUCT.DESCRIPTION.as("product_description"),
                PRODUCT_ATTRIBUTE.ID.as("attribute_id"), PRODUCT_ATTRIBUTE.FIELD_NAME.as("attribute_code"), PRODUCT_ATTRIBUTE.NAME.as("attribute_name"),
                ATTRIBUTE_VALUE.ID.as("value_id"), ATTRIBUTE_VALUE.VALUE.as("value")
        ).from(PRODUCT).join(PRODUCT_ATTRIBUTE).on(PRODUCT_ATTRIBUTE.PRODUCT_ID.eq(PRODUCT.ID)).join(ATTRIBUTE_VALUE).on(ATTRIBUTE_VALUE.ATTRIBUTE_ID.eq(PRODUCT_ATTRIBUTE.ID)).where(PRODUCT.ID.eq(productId));

        return select
                .orderBy(
                        PRODUCT.ID.asc(), PRODUCT_ATTRIBUTE.ID.asc(), ATTRIBUTE_VALUE.ID.asc())
                .fetchInto(ProductDetailRow.class);
    }

    @Override
    public Product getById(Integer id) {
        return getByIdWithJoin(getJoinFields(), getJoinTable(), id);
    }
}
