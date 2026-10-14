package vn.aeoc.crm.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.customer.CustomerService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.customer.Customer;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;

@RestController
public class CustomerController extends BaseController {

    private static final String URI = "/customer/";

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService, UserService userService) {
        super(userService);
        this.customerService = customerService;
    }

    /**
     * Danh sách khách hàng (tìm kiếm & phân trang)
     */
    @GetMapping("/customer/extra/list")
    public ResponseEntity<ApiResponse<Page<Customer>>> listCustomersExtraInfo(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String segmentCode,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        Integer branchId = BranchFilter.listScopeBranchId(login);

        var result = customerService.getLstExtraInfoByCriteria(keyword, branchId, segmentCode, pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/customer/list")
    public ResponseEntity<ApiResponse<Page<Customer>>> listCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String segmentCode,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        Integer branchId = BranchFilter.listScopeBranchId(login);

        var result = customerService.getByCriteria(keyword, branchId, segmentCode, pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Lấy chi tiết khách hàng
     */
    @GetMapping("/customer/get")
    public ResponseEntity<ApiResponse<Customer>> getCustomerDetail(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = customerService.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Thêm hoặc cập nhật khách hàng (Upsert)
     */
    @PostMapping("/customer/update")
    public ResponseEntity<ApiResponse<Customer>> upsertCustomer(
            @RequestBody Customer customer,
            Principal principal
    ) {
        String action = (CommonUtils.NVL(customer.getId()) <= 0) ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);
        customer.setCreatorId(login.getId());
        customer.setBranchId(login.getBranchId());
        Customer result = (CommonUtils.NVL(customer.getId()) <= 0)
                ? customerService.insertCustomer(customer)
                : customerService.updateCustomer(customer);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Xóa khách hàng
     */
    @DeleteMapping("/customer/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> deleteCustomer(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = customerService.deleteCustomer(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
