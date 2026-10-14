package vn.aeoc.crm.repository.product;

import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.product.ProductReview;
import vn.entity.backend.tables.records.ProductReviewRecord;

import java.util.List;

import static vn.entity.backend.tables.ProductReview.PRODUCT_REVIEW;

@Repository
public class ProductReviewRepository
        extends AbsMysqlRepository<ProductReviewRecord, ProductReview, Integer> {
    public ProductReviewRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<ProductReviewRecord> getTable() {
        return PRODUCT_REVIEW;
    }

    private Condition getWhereCondition(
            Integer productId,
            Integer productVariantId,
            Integer userId,
            Integer rating,
            Integer orderId
    ) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(productId) > 0) {
            condition = condition.and(PRODUCT_REVIEW.PRODUCT_ID.eq(productId));
        }

        if (CommonUtils.NVL(productVariantId) > 0) {
            condition = condition.and(PRODUCT_REVIEW.PRODUCT_VARIANT_ID.eq(productVariantId));
        }

        if (CommonUtils.NVL(userId) > 0) {
            condition = condition.and(PRODUCT_REVIEW.USER_ID.eq(userId));
        }

        if (CommonUtils.NVL(rating) > 0) {
            condition = condition.and(PRODUCT_REVIEW.RATING.eq(rating));
        }

        if (CommonUtils.NVL(orderId) > 0) {
            condition = condition.and(PRODUCT_REVIEW.ORDER_ID.eq(orderId));
        }

        return condition;
    }

    public List<ProductReview> getByCriteria(
            Integer productId,
            Integer productVariantId,
            Integer userId,
            Integer rating,
            Integer orderId,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(productId, productVariantId, userId, rating, orderId);
        return getListByCriteriaWithJoin(
                mergeFields(),
                PRODUCT_REVIEW,
                condition,
                PRODUCT_REVIEW.CREATED_AT.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            Integer productId,
            Integer productVariantId,
            Integer userId,
            Integer rating,
            Integer orderId
    ) {
        Condition condition = getWhereCondition(productId, productVariantId, userId, rating, orderId);
        return getCountWithCriteria(condition);
    }
}
