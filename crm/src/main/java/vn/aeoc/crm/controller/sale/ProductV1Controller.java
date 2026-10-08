package vn.aeoc.crm.controller.sale;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.constant.RagSyncConstant;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.other.RagService;
import vn.aeoc.crm.service.sale.ProductV1Service;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.sale.ProductV1;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;

@RestController
public class ProductV1Controller extends BaseController {

    private static final String URI = "/product/";

    @Autowired
    private ProductV1Service productV1Service;
    @Autowired
    private RagService ragService;

    @GetMapping("/productv1/list")
    public ResponseEntity<ApiResponse<Page<ProductV1>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer type,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = productV1Service.getListProductByCriteria(
                keyword, categoryId, status, type, BranchFilter.listScopeBranchId(login), pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/productv1/get")
    public ResponseEntity<ApiResponse<ProductV1>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = productV1Service.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/productv1/update")
    public ResponseEntity<ApiResponse<ProductV1>> update(
            @RequestBody @Valid ProductV1 productV1,
            Principal principal
    ) {
        String action = (CommonUtils.NVL(productV1.getId()) <= 0) ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);
        if (CommonUtils.NVL(productV1.getBranchId()) <= 0 && CommonUtils.NVL(login.getBranchId()) > 0) {
            productV1.setBranchId(login.getBranchId());
        }

        ProductV1 result = (CommonUtils.NVL(productV1.getId()) <= 0)
                ? productV1Service.insert(productV1)
                : productV1Service.update(productV1.getId(), productV1);

        ragService.upsert(result.getId(), RagSyncConstant.PRODUCT_V1, result.getBranchId());
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/productv1/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var product = productV1Service.getById(id);
        var result = productV1Service.delete(id);
        if(result.equals(1))
            ragService.delete(id, RagSyncConstant.PRODUCT_V1, product.getBranchId());
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
