package vn.aeoc.crm.controller.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.product.ProductWishlistService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.product.ProductWishlist;

import java.security.Principal;

@RestController
public class ProductWishlistController extends BaseController {

    private static final String URI = "/product-wishlist/";

    @Autowired
    private ProductWishlistService productWishlistService;

    @GetMapping("/product-wishlist/list")
    public ResponseEntity<ApiResponse<Page<ProductWishlist>>> list(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer productId,
            Pageable pageable,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = productWishlistService.getByCriteria(
                userId, productId, pageable
        );
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/product-wishlist/update")
    public ResponseEntity<ApiResponse<ProductWishlist>> update(
            @RequestBody ProductWishlist request,
            Principal principal
    ) {
        checkPermission(principal, URI, "ADD");
        ProductWishlist result = productWishlistService.insert(request);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/product-wishlist/delete")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @RequestParam Integer userId,
            @RequestParam Integer productId,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = productWishlistService.deleteByCompositeKey(userId, productId);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
