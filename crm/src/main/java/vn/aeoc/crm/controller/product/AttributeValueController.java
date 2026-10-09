package vn.aeoc.crm.controller.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.product.AttributeValueService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.product.AttributeValue;

import java.security.Principal;

@RestController
public class AttributeValueController extends BaseController {

    private static final String URI = "/attribute-value/";

    @Autowired
    private AttributeValueService attributeValueService;

    @GetMapping("/attribute-value/list")
    public ResponseEntity<ApiResponse<Page<AttributeValue>>> list(
            @RequestParam(required = false) Integer attributeId,
            @RequestParam(required = false) String value,
            Pageable pageable,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = attributeValueService.getByCriteria(
                attributeId, value, pageable
        );
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/attribute-value/get")
    public ResponseEntity<ApiResponse<AttributeValue>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = attributeValueService.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/attribute-value/update")
    public ResponseEntity<ApiResponse<AttributeValue>> update(
            @RequestBody AttributeValue request,
            Principal principal
    ) {
        String action = CommonUtils.NVL(request.getId()) <= 0 ? "ADD" : "UPDATE";
        checkPermission(principal, URI, action);

        AttributeValue result = CommonUtils.NVL(request.getId()) <= 0
                ? attributeValueService.insert(request)
                : attributeValueService.update(request.getId(), request);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/attribute-value/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = attributeValueService.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
