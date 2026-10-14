package vn.aeoc.crm.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.customer.CallHistoryService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.customer.CallHistory;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;

@RestController
public class CallHistoryController extends BaseController {

    private static final String URI = "/callHistory/";

    private final CallHistoryService service;

    public CallHistoryController(CallHistoryService service, UserService userService) {
        super(userService);
        this.service = service;
    }

    @GetMapping("/callHistory/list")
    public ResponseEntity<ApiResponse<Page<CallHistory>>> list(
            @RequestParam(name = "userId", required = false) Integer userId,
            @RequestParam(name = "customerId", required = false) Integer customerId,
            @RequestParam(name = "callType", required = false) Integer callType,
            @RequestParam(name = "status", required = false) Integer status,
            Pageable pageable,
            Principal principal) {

        User login = checkPermission(principal, URI, "VIEW");
        var result = service.getByCriteria(
                userId, customerId, callType, status, BranchFilter.listScopeBranchId(login), pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/callHistory/update")
    public ResponseEntity<ApiResponse<CallHistory>> upsert(
            @RequestBody CallHistory callHistory,
            Principal principal) {

        String action = (CommonUtils.NVL(callHistory.getId()) <= 0) ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);
        if (CommonUtils.NVL(login.getBranchId()) > 0) {
            callHistory.setBranchId(login.getBranchId());
        }

        CallHistory result = (CommonUtils.NVL(callHistory.getId()) <= 0)
                ? service.insert(callHistory)
                : service.update(callHistory.getId(), callHistory);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/callHistory/update-status")
    public ResponseEntity<ApiResponse<Integer>> updateStatus(
            @RequestParam(name = "id") Integer id,
            @RequestParam(name = "status") Integer status,
            Principal principal) {

        checkPermission(principal, URI, "UPDATE");
        var result = service.updateStatus(id, status);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/callHistory/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable(name = "id") Integer id,
            Principal principal) {

        checkPermission(principal, URI, "DELETE");
        var result = service.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
