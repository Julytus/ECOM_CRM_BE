package vn.aeoc.crm.controller.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.product.ProductReviewService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.product.ProductReview;

import java.security.Principal;

@RestController
public class ProductReviewController extends BaseController {

    private static final String URI = "/product-review/";

    private final ProductReviewService productReviewService;

    public ProductReviewController(ProductReviewService productReviewService, UserService userService) {
        super(userService);
        this.productReviewService = productReviewService;
    }

    @GetMapping("/product-review/list")
    public ResponseEntity<ApiResponse<Page<ProductReview>>> list(
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) Integer productVariantId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer orderId,
            Pageable pageable,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = productReviewService.getByCriteria(
                productId, productVariantId, userId, rating, orderId, pageable
        );
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/product-review/get")
    public ResponseEntity<ApiResponse<ProductReview>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = productReviewService.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/product-review/update")
    public ResponseEntity<ApiResponse<ProductReview>> update(
            @RequestBody ProductReview request,
            Principal principal
    ) {
        String action = CommonUtils.NVL(request.getId()) <= 0 ? "ADD" : "UPDATE";
        checkPermission(principal, URI, action);

        ProductReview result = CommonUtils.NVL(request.getId()) <= 0
                ? productReviewService.insert(request)
                : productReviewService.update(request.getId(), request);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/product-review/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = productReviewService.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
