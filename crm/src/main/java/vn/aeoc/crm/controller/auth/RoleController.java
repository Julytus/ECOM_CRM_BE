package vn.aeoc.crm.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.RoleService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.auth.Role;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class RoleController extends BaseController {
    private static final String URI = "/role/";
    private final RoleService roleService;

    @GetMapping("/role/list")
    public ResponseEntity<ApiResponse<Page<Role>>> getAllRoles(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer isDefault,
            @RequestParam(required = false) Integer isOperator,
            Pageable pageable,
            Principal principal) {

        checkPermission(principal, URI, "VIEW");
        var result = roleService.getListRoleByCriteria(id, keyword, isDefault, isOperator, pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/role/update")
    public ResponseEntity<ApiResponse<Role>> upsertRole(@RequestBody Role role, Principal principal) {
        String action = (CommonUtils.NVL(role.getId()) <= 0) ? "ADD" : "UPDATE";
        checkPermission(principal, URI, action);

        Role result;
        if (CommonUtils.NVL(role.getId()) <= 0) {
            result = roleService.insert(role);
        } else {
            result = roleService.update(role);
        }

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/role/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> deleteRole(@PathVariable Integer id, Principal principal) {
        checkPermission(principal, URI, "DELETE");
        var result = roleService.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
