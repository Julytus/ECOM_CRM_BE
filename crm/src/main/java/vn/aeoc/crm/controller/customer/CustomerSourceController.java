package vn.aeoc.crm.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.customer.CustomerSourceService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.customer.CustomerSource;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;

@RestController
public class CustomerSourceController extends BaseController {

    private static final String URI = "/customerSource/";

    private final CustomerSourceService service;

    public CustomerSourceController(CustomerSourceService service, UserService userService) {
        super(userService);
        this.service = service;
    }

    @GetMapping("/customerSource/list")
    public ResponseEntity<ApiResponse<Page<CustomerSource>>> list(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status,
            Pageable pageable,
            Principal principal) {

        User login = checkPermission(principal, URI, "VIEW");
        var result = service.getByCriteria(keyword, status, BranchFilter.listScopeBranchId(login), pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/customerSource/update")
    public ResponseEntity<ApiResponse<CustomerSource>> upsert(
            @RequestBody CustomerSource source,
            Principal principal) {

        String action = (CommonUtils.NVL(source.getId()) <= 0) ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);
        if (CommonUtils.NVL(source.getBranchId()) <= 0 && CommonUtils.NVL(login.getBranchId()) > 0) {
            source.setBranchId(login.getBranchId());
        }

        CustomerSource result = (CommonUtils.NVL(source.getId()) <= 0)
                ? service.insert(source)
                : service.update(source.getId(), source);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/customerSource/update-status")
    public ResponseEntity<ApiResponse<Integer>> updateStatus(
            @RequestParam(name = "id") Integer id,
            @RequestParam(name = "status") Integer status,
            Principal principal) {

        checkPermission(principal, URI, "UPDATE");
        var result = service.updateStatus(id, status);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/customerSource/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable(name = "id") Integer id,
            Principal principal) {

        checkPermission(principal, URI, "DELETE");
        var result = service.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
