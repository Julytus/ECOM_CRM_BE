package vn.aeoc.crm.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.customer.CustomerAttributeService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.customer.CustomerAttribute;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;

@RestController
public class CustomerAttributeController extends BaseController {

    private static final String URI = "/customerAttribute/";

    private final CustomerAttributeService service;

    public CustomerAttributeController(CustomerAttributeService service, UserService userService) {
        super(userService);
        this.service = service;
    }

    /**
     * Danh sách Customer Attribute (tìm kiếm & phân trang)
     */
    @GetMapping("/customerAttribute/list")
    public ResponseEntity<ApiResponse<Page<CustomerAttribute>>> listCustomerAttributes(
            @RequestParam(required = false) Integer isParent,
            @RequestParam(required = false) Integer isTitle,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dataType,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = service.getByCriteria(
                isParent, isTitle, keyword, dataType, BranchFilter.listScopeBranchId(login), pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Lấy chi tiết Customer Attribute
     */
    @GetMapping("/customerAttribute/get")
    public ResponseEntity<ApiResponse<CustomerAttribute>> getCustomerAttribute(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = service.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Thêm hoặc cập nhật Customer Attribute (Upsert)
     */
    @PostMapping("/customerAttribute/update")
    public ResponseEntity<ApiResponse<CustomerAttribute>> upsertCustomerAttribute(
            @RequestBody CustomerAttribute attr,
            Principal principal
    ) {
        String action = (CommonUtils.NVL(attr.getId()) <= 0) ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);
        if (CommonUtils.NVL(attr.getBranchId()) <= 0 && CommonUtils.NVL(login.getBranchId()) > 0) {
            attr.setBranchId(login.getBranchId());
        }

        CustomerAttribute result = (CommonUtils.NVL(attr.getId()) <= 0)
                ? service.insert(attr)
                : service.update(attr.getId(), attr);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Xóa Customer Attribute
     */
    @DeleteMapping("/customerAttribute/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> deleteCustomerAttribute(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = service.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}