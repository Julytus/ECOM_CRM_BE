package vn.aeoc.crm.controller.sale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.sale.UnitService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.sale.Unit;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;

@RestController
public class UnitController extends BaseController {

    private static final String URI = "/product/";

    private final UnitService service;

    public UnitController(UnitService service, UserService userService) {
        super(userService);
        this.service = service;
    }

    @GetMapping("/unit/list")
    public ResponseEntity<ApiResponse<Page<Unit>>> listUnits(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = service.getListUnitByCriteria(keyword, status, BranchFilter.listScopeBranchId(login), pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/unit/get")
    public ResponseEntity<ApiResponse<Unit>> getUnit(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = service.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/unit/update")
    public ResponseEntity<ApiResponse<Unit>> upsertUnit(
            @RequestBody Unit unit,
            Principal principal
    ) {
        String action = (CommonUtils.NVL(unit.getId()) <= 0) ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);
        if (CommonUtils.NVL(unit.getBranchId()) <= 0 && CommonUtils.NVL(login.getBranchId()) > 0) {
            unit.setBranchId(login.getBranchId());
        }

        Unit result = (CommonUtils.NVL(unit.getId()) <= 0)
                ? service.insert(unit)
                : service.update(unit.getId(), unit);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/unit/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> deleteUnit(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = service.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/unit/update-status")
    public ResponseEntity<ApiResponse<Integer>> updateStatus(
            @RequestParam Integer id,
            @RequestParam Integer status,
            Principal principal
    ) {
        checkPermission(principal, URI, "UPDATE");
        var result = service.updateUnitStatus(id, status);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
