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
import vn.aeoc.crm.util.SecurityUtils;
import vn.aeoc.entity.data.mysql.product.ProductWishlist;

import java.security.Principal;

@RestController
public class ProductWishlistController extends BaseController {

    private static final String URI = "/wishlist/";

    @Autowired
    private ProductWishlistService productWishlistService;

    @GetMapping("/wishlist")
    public ResponseEntity<ApiResponse<?>> getByUser(
            Principal principal) {

        return ResponseEntity.ok(
                ApiResponse.ok(productWishlistService.getByCriteria(SecurityUtils.userId(principal))));
    }

    @PostMapping("/wishlist/add")
    public ResponseEntity<ApiResponse<String>> update(
            @RequestBody Integer productId,
            Principal principal
    ) {
//        checkPermission(principal, URI, "ADD");
        productWishlistService.add(SecurityUtils.userId(principal), productId);
        return Mapper.map("Đã thêm sản phẩm vào yêu thích", ApiResponse::okEntity);
    }

    @DeleteMapping("/wishlist/remove")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @RequestParam Integer productId,
            Principal principal
    ) {
//        checkPermission(principal, URI, "DELETE");
        var result = productWishlistService.deleteByCompositeKey(SecurityUtils.userId(principal), productId);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
