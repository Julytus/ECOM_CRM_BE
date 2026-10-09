package vn.aeoc.crm.repository.product;

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
import vn.aeoc.entity.data.mysql.product.ProductVariant;
import vn.entity.backend.tables.records.ProductVariantRecord;

import java.util.List;

import static vn.entity.backend.tables.Product.PRODUCT;
import static vn.entity.backend.tables.ProductVariant.PRODUCT_VARIANT;

@Repository
public class ProductVariantRepository
        extends AbsMysqlRepository<ProductVariantRecord, ProductVariant, Integer> {

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
}
