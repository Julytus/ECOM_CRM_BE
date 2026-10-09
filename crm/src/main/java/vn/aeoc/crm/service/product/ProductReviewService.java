package vn.aeoc.crm.service.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.product.ProductReviewRepository;
import vn.aeoc.entity.data.mysql.product.ProductReview;

@Slf4j(topic = "PRODUCT-REVIEW-SERVICE")
@Service
public class ProductReviewService extends BaseService<
        ProductReviewRepository,
        ProductReview,
        Integer> {

    public ProductReviewService(ProductReviewRepository repository) {
        super(repository);
    }

    public Page<ProductReview> getByCriteria(
            Integer productId,
            Integer productVariantId,
            Integer userId,
            Integer rating,
            Integer orderId,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(productId, productVariantId, userId, rating, orderId, pageable),
                () -> repository.countByCriteria(productId, productVariantId, userId, rating, orderId)
        );
    }
}
