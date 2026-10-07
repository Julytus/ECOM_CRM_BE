package vn.aeoc.crm.controller.sale;

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
import vn.aeoc.crm.service.sale.ServiceItemService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.ServiceItem;
import vn.aeoc.entity.data.mysql.User;

import java.security.Principal;

@RestController
public class ServiceItemController extends BaseController {

    private static final String URI = "/service/";

    @Autowired
    private ServiceItemService serviceItemService;
    @Autowired
    private RagService ragService;

    @GetMapping("/service/list")
    public ResponseEntity<ApiResponse<Page<ServiceItem>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer isCombo,
            @RequestParam(required = false) Integer featured,
            @RequestParam(required = false) Integer status,
            Pageable pageable,
            Principal principal) {

        User login = checkPermission(principal, URI, "VIEW");
        var result = serviceItemService.getByCriteria(
                keyword, categoryId, isCombo, featured, status, BranchFilter.listScopeBranchId(login), pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/service/get")
    public ResponseEntity<ApiResponse<ServiceItem>> get(
            @RequestParam Integer id,
            Principal principal) {

        checkPermission(principal, URI, "VIEW");
        var result = serviceItemService.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/service/update")
    public ResponseEntity<ApiResponse<ServiceItem>> update(
            @RequestBody ServiceItem service,
            Principal principal) {

        String action = (CommonUtils.NVL(service.getId()) <= 0) ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);
        if (CommonUtils.NVL(service.getBranchId()) <= 0 && CommonUtils.NVL(login.getBranchId()) > 0) {
            service.setBranchId(login.getBranchId());
        }

        ServiceItem result = (CommonUtils.NVL(service.getId()) <= 0)
                ? serviceItemService.insert(service)
                : serviceItemService.update(service.getId(), service);

        ragService.upsert(result.getId(), RagSyncConstant.SERVICE, result.getBranchId());
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/service/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal) {

        checkPermission(principal, URI, "DELETE");
        var service = serviceItemService.getById(id);
        var result = serviceItemService.delete(id);
        if(result.equals(1))
            ragService.delete(id, RagSyncConstant.SERVICE, service.getBranchId());
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
