package vn.aeoc.crm.service.product;

import vn.aeoc.base.util.QueryExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.product.ProductWishlistRepository;
import vn.aeoc.entity.data.mysql.product.ProductWishlist;

import java.util.List;

@Slf4j(topic = "PRODUCT-WISHLIST-SERVICE")
@Service
public class ProductWishlistService extends BaseService<
        ProductWishlistRepository,
        ProductWishlist,
        Integer> {

    public ProductWishlistService(ProductWishlistRepository repository, QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
    }

    public List<ProductWishlist> getByCriteria(
            Integer userId
    ) {
        return repository.getByCriteria(userId);
    }

    public void add(Integer userId, Integer productId) {

        repository.insertWishlist(userId, productId);
    }

    public int deleteByCompositeKey(Integer userId, Integer productId) {
        return repository.deleteByUserIdAndProductId(userId, productId);
    }
}
