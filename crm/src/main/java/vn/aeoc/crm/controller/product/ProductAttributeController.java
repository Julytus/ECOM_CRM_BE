package vn.aeoc.crm.controller.product;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.product.ProductAttributeService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.product.ProductAttribute;

import java.security.Principal;

@RestController
public class ProductAttributeController extends BaseController {

    private static final String URI = "/product-attribute/";

    private final ProductAttributeService productAttributeService;

    public ProductAttributeController(ProductAttributeService productAttributeService, UserService userService) {
        super(userService);
        this.productAttributeService = productAttributeService;
    }

    @GetMapping("/product-attribute/list")
    public ResponseEntity<ApiResponse<Page<ProductAttribute>>> list(
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer productId,
            Pageable pageable,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = productAttributeService.getByCriteria(
                fieldName, name, productId, pageable
        );
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/product-attribute/get")
    public ResponseEntity<ApiResponse<ProductAttribute>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = productAttributeService.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/product-attribute/update")
    public ResponseEntity<ApiResponse<ProductAttribute>> update(
            @RequestBody ProductAttribute request,
            Principal principal
    ) {
        String action = CommonUtils.NVL(request.getId()) <= 0 ? "ADD" : "UPDATE";
        checkPermission(principal, URI, action);

        ProductAttribute result = CommonUtils.NVL(request.getId()) <= 0
                ? productAttributeService.insert(request)
                : productAttributeService.update(request.getId(), request);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/product-attribute/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = productAttributeService.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
