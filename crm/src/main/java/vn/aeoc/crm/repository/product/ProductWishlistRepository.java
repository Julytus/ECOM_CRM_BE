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
            Integer userId
    ) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(userId) >= 0) {
            condition = condition.and(PRODUCT_WISHLIST.USER_ID.eq(userId));
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
            Integer userId
    ) {
        Condition condition = getWhereCondition(userId);
        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                condition,
                PRODUCT_WISHLIST.CREATED_AT.desc()
        );
    }

    public Long countByCriteria(
            Integer userId
    ) {
        Condition condition = getWhereCondition(userId);
        return getCountWithCriteria(condition);
    }

    public Integer deleteByUserIdAndProductId(Integer userId, Integer productId) {
        return delete(
                PRODUCT_WISHLIST.USER_ID.eq(userId)
                        .and(PRODUCT_WISHLIST.PRODUCT_ID.eq(productId))
        );
    }

    public void insertWishlist(Integer userId, Integer productId) {
        dslContext.insertInto(PRODUCT_WISHLIST)
                .set(PRODUCT_WISHLIST.USER_ID, userId)
                .set(PRODUCT_WISHLIST.PRODUCT_ID, productId)
                .execute();
    }

}
