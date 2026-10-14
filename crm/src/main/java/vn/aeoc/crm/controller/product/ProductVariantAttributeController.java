package vn.aeoc.crm.controller.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.product.ProductVariantAttributeService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.product.ProductVariantAttribute;

import java.security.Principal;

@RestController
public class ProductVariantAttributeController extends BaseController {

    private static final String URI = "/product-variant-attribute/";

    private final ProductVariantAttributeService productVariantAttributeService;

    public ProductVariantAttributeController(ProductVariantAttributeService productVariantAttributeService, UserService userService) {
        super(userService);
        this.productVariantAttributeService = productVariantAttributeService;
    }

    @GetMapping("/product-variant-attribute/list")
    public ResponseEntity<ApiResponse<Page<ProductVariantAttribute>>> list(
            @RequestParam(required = false) Integer variantId,
            @RequestParam(required = false) Integer attributeValueId,
            Pageable pageable,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = productVariantAttributeService.getByCriteria(
                variantId, attributeValueId, pageable
        );
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/product-variant-attribute/update")
    public ResponseEntity<ApiResponse<ProductVariantAttribute>> update(
            @RequestBody ProductVariantAttribute request,
            Principal principal
    ) {
        checkPermission(principal, URI, "ADD");
        ProductVariantAttribute result = productVariantAttributeService.insert(request);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/product-variant-attribute/delete")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @RequestParam Integer variantId,
            @RequestParam Integer attributeValueId,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = productVariantAttributeService.deleteByCompositeKey(variantId, attributeValueId);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
