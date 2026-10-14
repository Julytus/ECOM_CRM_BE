package vn.aeoc.crm.controller.sale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.sale.BoughtServiceItemService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.sale.BoughtService;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;
import java.util.List;

@RestController
public class BoughtServiceController extends BaseController {

    private static final String URI = "/sale/";

    private final BoughtServiceItemService service;

    public BoughtServiceController(BoughtServiceItemService service, UserService userService) {
        super(userService);
        this.service = service;
    }

    /**
     * Danh sách dịch vụ trong hóa đơn
     */
    @GetMapping("/boughtService/list")
    public ResponseEntity<ApiResponse<Page<BoughtService>>> list(
            @RequestParam(required = false) Integer invoiceId,
            @RequestParam(required = false) Integer status,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = service.getByCriteria(invoiceId, status, BranchFilter.listScopeBranchId(login), pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Chi tiết dịch vụ đã mua
     */
    @GetMapping("/boughtService/get")
    public ResponseEntity<ApiResponse<BoughtService>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getById(id), ApiResponse::okEntity);
    }

    /**
     * Thêm / cập nhật dịch vụ vào hóa đơn
     */
    @PostMapping("/boughtService/update")
    public ResponseEntity<ApiResponse<BoughtService>> update(
            @RequestBody BoughtService boughtService,
            Principal principal
    ) {
        String action = CommonUtils.NVL(boughtService.getId()) <= 0 ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);
        if (CommonUtils.NVL(login.getBranchId()) > 0) {
            boughtService.setBranchId(login.getBranchId());
        }

        BoughtService result = CommonUtils.NVL(boughtService.getId()) <= 0
                ? service.insert(boughtService)
                : service.update(boughtService.getId(), boughtService);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/boughtService/batch-upsert")
    public ResponseEntity<ApiResponse<List<BoughtService>>> upsertBatch(
            @RequestBody List<BoughtService> services,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "UPDATE");
        if (services != null && CommonUtils.NVL(login.getBranchId()) > 0) {
            Integer bid = login.getBranchId();
            services.forEach(p -> p.setBranchId(bid));
        }

        List<BoughtService> result = service.upsertBatch(services);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Xóa dịch vụ khỏi hóa đơn
     */
    @DeleteMapping("/boughtService/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        return Mapper.map(service.delete(id), ApiResponse::okEntity);
    }

}
