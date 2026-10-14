package vn.aeoc.crm.controller.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.response.ProductVariantResponse;
import vn.aeoc.crm.service.product.ProductVariantService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.product.ProductVariant;

import java.security.Principal;
import java.util.List;

@RestController
public class ProductVariantController extends BaseController {

    private static final String URI = "/product-variant/";

    @Autowired
    private ProductVariantService productVariantService;

    @GetMapping("/product-variant/list")
    public ResponseEntity<ApiResponse<Page<ProductVariant>>> list(
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Integer maxStock,
            Pageable pageable,
            Principal principal
    ) {
//        checkPermission(principal, URI, "VIEW");
        var result = productVariantService.getByCriteria(
                productId, sku, minStock, maxStock, pageable
        );
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/product-variant/get")
    public ResponseEntity<ApiResponse<ProductVariant>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
//        checkPermission(principal, URI, "VIEW");
        var result = productVariantService.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/product-variant/update")
    public ResponseEntity<ApiResponse<ProductVariant>> update(
            @RequestBody ProductVariant request,
            Principal principal
    ) {
        String action = CommonUtils.NVL(request.getId()) <= 0 ? "ADD" : "UPDATE";
        checkPermission(principal, URI, action);

        ProductVariant result = CommonUtils.NVL(request.getId()) <= 0
                ? productVariantService.insert(request)
                : productVariantService.update(request.getId(), request);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/product-variant/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = productVariantService.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/productVariant/detailByIds")
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getLstDetail(
            @ModelAttribute(name = "ids") List<Integer> ids) {

        var result = productVariantService.getDetailByIds(ids);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
