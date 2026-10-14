package vn.aeoc.crm.repository.product;

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
import vn.aeoc.crm.data.response.ProductVariantRow;
import vn.aeoc.crm.data.response.VariantAttributeRow;
import vn.aeoc.entity.data.mysql.product.ProductVariant;
import vn.entity.backend.tables.records.ProductVariantRecord;

import java.util.List;

import static vn.entity.backend.tables.AttributeValue.ATTRIBUTE_VALUE;
import static vn.entity.backend.tables.Product.PRODUCT;
import static vn.entity.backend.tables.ProductAttribute.PRODUCT_ATTRIBUTE;
import static vn.entity.backend.tables.ProductVariant.PRODUCT_VARIANT;
import static vn.entity.backend.tables.ProductVariantAttribute.PRODUCT_VARIANT_ATTRIBUTE;

@Repository
public class ProductVariantRepository
        extends AbsMysqlRepository<ProductVariantRecord, ProductVariant, Integer> {
    public ProductVariantRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<ProductVariantRecord> getTable() {
        return PRODUCT_VARIANT;
    }

    private Condition getWhereCondition(
            Integer productId,
            String sku,
            Integer minStock,
            Integer maxStock
    ) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(productId) > 0) {
            condition = condition.and(PRODUCT_VARIANT.PRODUCT_ID.eq(productId));
        }

        if (!StringUtils.isBlank(sku)) {
            String kw = "%" + sku.trim().toLowerCase() + "%";
            condition = condition.and(DSL.lower(PRODUCT_VARIANT.SKU).like(kw));
        }

        if (CommonUtils.NVL(minStock) >= 0) {
            condition = condition.and(PRODUCT_VARIANT.STOCK.greaterOrEqual(minStock));
        }

        if (CommonUtils.NVL(maxStock) >= 0) {
            condition = condition.and(PRODUCT_VARIANT.STOCK.lessOrEqual(maxStock));
        }

        return condition;
    }

    private List<Field<?>> getJoinFields() {
        return mergeFields(
                PRODUCT.NAME.as("product_name")
        );
    }

    private Table<?> getJoinTable() {
        return PRODUCT_VARIANT.leftJoin(PRODUCT).on(PRODUCT_VARIANT.PRODUCT_ID.eq(PRODUCT.ID));
    }

    public List<ProductVariant> getByCriteria(
            Integer productId,
            String sku,
            Integer minStock,
            Integer maxStock,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(productId, sku, minStock, maxStock);
        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                condition,
                PRODUCT_VARIANT.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            Integer productId,
            String sku,
            Integer minStock,
            Integer maxStock
    ) {
        Condition condition = getWhereCondition(productId, sku, minStock, maxStock);
        return getCountWithCriteria(condition);
    }

    @Override
    public ProductVariant getById(Integer id) {
        return getByIdWithJoin(getJoinFields(), getJoinTable(), id);
    }

    public List<ProductVariantRow> fetchVariantDetailByProductIds(List<Integer> productIds) {

        return dslContext.select(
                        PRODUCT_VARIANT.ID,
                        PRODUCT_VARIANT.SKU.as("sku"),
                        PRODUCT_VARIANT.IMAGE.as("image"),
                        PRODUCT_VARIANT.PRICE.as("price"),
                        PRODUCT_VARIANT.ORIGINAL_PRICE.as("original_price"),
                        PRODUCT_VARIANT.STOCK.as("stock"),
                        PRODUCT_VARIANT.PRODUCT_ID.as("product_id"),
                        PRODUCT.NAME.as("product_name"),
                        PRODUCT_VARIANT.CREATED_AT.as("created_at"),
                        PRODUCT_ATTRIBUTE.FIELD_NAME.as("field_name"),
                        ATTRIBUTE_VALUE.VALUE.as("value")
                )
                .from(PRODUCT_VARIANT)
                .leftJoin(PRODUCT).on(PRODUCT.ID.eq(PRODUCT_VARIANT.PRODUCT_ID))
                .join(PRODUCT_VARIANT_ATTRIBUTE)
                .on(PRODUCT_VARIANT_ATTRIBUTE.VARIANT_ID.eq(PRODUCT_VARIANT.ID))
                .join(ATTRIBUTE_VALUE)
                .on(ATTRIBUTE_VALUE.ID.eq(PRODUCT_VARIANT_ATTRIBUTE.ATTRIBUTE_VALUE_ID))
                .join(PRODUCT_ATTRIBUTE)
                .on(PRODUCT_ATTRIBUTE.ID.eq(ATTRIBUTE_VALUE.ATTRIBUTE_ID))
                .where(PRODUCT_VARIANT.PRODUCT_ID.in(productIds))
                .orderBy(
                        PRODUCT_VARIANT.PRODUCT_ID.asc(),
                        PRODUCT_VARIANT.ID.asc()
                )
                .fetchInto(ProductVariantRow.class);
    }

    public List<ProductVariantRow> fetchVariantDetailByProductId(Integer productId) {

        return dslContext.select(
                        PRODUCT_VARIANT.ID.as("id"),
                        PRODUCT_VARIANT.SKU.as("sku"),
                        PRODUCT_VARIANT.IMAGE.as("image"),
                        PRODUCT_VARIANT.PRICE.as("price"),
                        PRODUCT_VARIANT.ORIGINAL_PRICE.as("original_price"),
                        PRODUCT_VARIANT.STOCK.as("stock"),
                        PRODUCT_VARIANT.PRODUCT_ID.as("product_id"),
                        PRODUCT.NAME.as("product_name"),
                        PRODUCT_VARIANT.CREATED_AT.as("created_at"),
                        PRODUCT_ATTRIBUTE.FIELD_NAME.as("field_name"),
                        ATTRIBUTE_VALUE.VALUE.as("value")
                )
                .from(PRODUCT_VARIANT)
                .leftJoin(PRODUCT).on(PRODUCT.ID.eq(PRODUCT_VARIANT.PRODUCT_ID))
                .join(PRODUCT_VARIANT_ATTRIBUTE)
                .on(PRODUCT_VARIANT_ATTRIBUTE.VARIANT_ID.eq(PRODUCT_VARIANT.ID))
                .join(ATTRIBUTE_VALUE)
                .on(ATTRIBUTE_VALUE.ID.eq(PRODUCT_VARIANT_ATTRIBUTE.ATTRIBUTE_VALUE_ID))
                .join(PRODUCT_ATTRIBUTE)
                .on(PRODUCT_ATTRIBUTE.ID.eq(ATTRIBUTE_VALUE.ATTRIBUTE_ID))
                .where(PRODUCT_VARIANT.PRODUCT_ID.eq(productId))
                .orderBy(PRODUCT_VARIANT.ID.asc())
                .fetchInto(ProductVariantRow.class);
    }

    public List<ProductVariant> getByIds(List<Integer> ids) {

        return dslContext.select(
                        PRODUCT_VARIANT.ID,
                        PRODUCT_VARIANT.PRODUCT_ID,
                        PRODUCT.NAME.as("product_name"),   // JOIN thêm
                        PRODUCT_VARIANT.SKU,
                        PRODUCT_VARIANT.IMAGE,
                        PRODUCT_VARIANT.PRICE,
                        PRODUCT_VARIANT.ORIGINAL_PRICE,
                        PRODUCT_VARIANT.STOCK,
                        PRODUCT_VARIANT.CREATED_AT
                )
                .from(PRODUCT_VARIANT)
                .join(PRODUCT)
                .on(PRODUCT.ID.eq(PRODUCT_VARIANT.PRODUCT_ID))
                .where(PRODUCT_VARIANT.ID.in(ids))
                .fetchInto(ProductVariant.class);
    }

    public List<VariantAttributeRow> fetchVariantAttributesByVariantIds(
            List<Integer> variantIds) {

        return dslContext.select(
                        PRODUCT_VARIANT_ATTRIBUTE.VARIANT_ID.as("variant_id"),
                        PRODUCT_ATTRIBUTE.FIELD_NAME.as("field_name"),
                        ATTRIBUTE_VALUE.VALUE.as("value")
                )
                .from(PRODUCT_VARIANT_ATTRIBUTE)
                .join(ATTRIBUTE_VALUE)
                .on(ATTRIBUTE_VALUE.ID.eq(PRODUCT_VARIANT_ATTRIBUTE.ATTRIBUTE_VALUE_ID))
                .join(PRODUCT_ATTRIBUTE)
                .on(PRODUCT_ATTRIBUTE.ID.eq(ATTRIBUTE_VALUE.ATTRIBUTE_ID))
                .where(PRODUCT_VARIANT_ATTRIBUTE.VARIANT_ID.in(variantIds))
                .fetchInto(VariantAttributeRow.class);
    }
}
