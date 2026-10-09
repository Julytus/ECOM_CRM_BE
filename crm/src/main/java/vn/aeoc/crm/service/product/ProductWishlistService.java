package vn.aeoc.crm.service.product;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.product.ProductWishlistRepository;
import vn.aeoc.entity.data.mysql.product.ProductWishlist;

@Slf4j(topic = "PRODUCT-WISHLIST-SERVICE")
@Service
public class ProductWishlistService extends BaseService<
        ProductWishlistRepository,
        ProductWishlist,
        Integer> {

    public ProductWishlistService(ProductWishlistRepository repository) {
        super(repository);
    }

    public Page<ProductWishlist> getByCriteria(
            Integer userId,
            Integer productId,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(userId, productId, pageable),
                () -> repository.countByCriteria(userId, productId)
        );
    }

    public int deleteByCompositeKey(Integer userId, Integer productId) {
        return repository.deleteByUserIdAndProductId(userId, productId);
    }
}
