package vn.aeoc.crm.controller.product;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.request.ProductAggregateRequest;
import vn.aeoc.crm.data.response.ProductAggregateResult;
import vn.aeoc.crm.service.product.ProductAggregateService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;

@RestController
public class ProductAggregateController extends BaseController {
    private static final String URI = "/product/";
    @Autowired private ProductAggregateService productAggregateService;

    @PostMapping("/product/create-full")
    public ResponseEntity<ApiResponse<ProductAggregateResult>> createFull(
            @Valid @RequestBody ProductAggregateRequest request, Principal principal) {
        User login = checkPermission(principal, URI, "ADD");
        request.setId(null);
        return Mapper.map(productAggregateService.save(request, login.getBranchId()), ApiResponse::okEntity);
    }

    @PutMapping("/product/update-full/{id}")
    public ResponseEntity<ApiResponse<ProductAggregateResult>> updateFull(
            @PathVariable Integer id,
            @Valid @RequestBody ProductAggregateRequest request,
            Principal principal) {
        User login = checkPermission(principal, URI, "UPDATE");
        request.setId(id);
        return Mapper.map(productAggregateService.save(request, login.getBranchId()), ApiResponse::okEntity);
    }
}
