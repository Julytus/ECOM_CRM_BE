package vn.aeoc.crm.controller.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.request.ProductListRequest;
import vn.aeoc.crm.data.response.ProductDetailResponse;
import vn.aeoc.crm.service.product.ProductService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.product.Product;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;

@RestController
public class ProductController extends BaseController {

    private static final String URI = "/product/";

    @Autowired
    private ProductService productService;

    @GetMapping("/product/list")
    public ResponseEntity<ApiResponse<Page<Product>>> list(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer categoryId,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = productService.getByCriteria(
                id, name, status, categoryId, BranchFilter.listScopeBranchId(login), pageable
        );
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/product/list-detail")
    public ResponseEntity<ApiResponse<Page<ProductDetailResponse>>> getDetailByCriteria(
            @ModelAttribute ProductListRequest request, Pageable pageable) {

        Page<ProductDetailResponse> result = productService.getProductDetailList(request, pageable);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/product/detail/{id}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getDetail(
            @PathVariable(name = "id") Integer id) {

        var result = productService.getDetailById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/product/get")
    public ResponseEntity<ApiResponse<Product>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
//        checkPermission(principal, URI, "VIEW");
        var result = productService.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/product/update")
    public ResponseEntity<ApiResponse<Product>> update(
            @RequestBody Product request,
            Principal principal
    ) {
        String action = CommonUtils.NVL(request.getId()) <= 0 ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);

        if (CommonUtils.NVL(request.getBranchId()) <= 0
                && CommonUtils.NVL(login.getBranchId()) > 0) {
            request.setBranchId(login.getBranchId());
        }

        Product result = CommonUtils.NVL(request.getId()) <= 0
                ? productService.insert(request)
                : productService.update(request.getId(), request);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/product/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = productService.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
