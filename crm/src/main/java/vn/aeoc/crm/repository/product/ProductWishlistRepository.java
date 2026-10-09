package vn.aeoc.crm.repository.product;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.product.ProductWishlist;
import vn.entity.backend.tables.records.ProductWishlistRecord;

import java.util.List;

import static vn.entity.backend.tables.Product.PRODUCT;
import static vn.entity.backend.tables.ProductWishlist.PRODUCT_WISHLIST;

@Repository
public class ProductWishlistRepository
        extends AbsMysqlRepository<ProductWishlistRecord, ProductWishlist, Integer> {

    @Override
    protected TableImpl<ProductWishlistRecord> getTable() {
        return PRODUCT_WISHLIST;
    }

    private Condition getWhereCondition(
            Integer userId,
            Integer productId
    ) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(userId) > 0) {
            condition = condition.and(PRODUCT_WISHLIST.USER_ID.eq(userId));
        }

        if (CommonUtils.NVL(productId) > 0) {
            condition = condition.and(PRODUCT_WISHLIST.PRODUCT_ID.eq(productId));
        }

        return condition;
    }

    private List<Field<?>> getJoinFields() {
        return mergeFields(
                PRODUCT.NAME.as("product_name")
        );
    }

    private Table<?> getJoinTable() {
        return PRODUCT_WISHLIST.leftJoin(PRODUCT).on(PRODUCT_WISHLIST.PRODUCT_ID.eq(PRODUCT.ID));
    }

    public List<ProductWishlist> getByCriteria(
            Integer userId,
            Integer productId,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(userId, productId);
        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                condition,
                PRODUCT_WISHLIST.CREATED_AT.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            Integer userId,
            Integer productId
    ) {
        Condition condition = getWhereCondition(userId, productId);
        return getCountWithCriteria(condition);
    }

    public int deleteByUserIdAndProductId(Integer userId, Integer productId) {
        return dslContext.deleteFrom(PRODUCT_WISHLIST)
                .where(PRODUCT_WISHLIST.USER_ID.eq(userId))
                .and(PRODUCT_WISHLIST.PRODUCT_ID.eq(productId))
                .execute();
    }
}
