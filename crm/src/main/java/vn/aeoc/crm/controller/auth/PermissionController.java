package vn.aeoc.crm.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.pojo.ResourceInfo;
import vn.aeoc.crm.service.auth.PermissionService;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.crm.util.SecurityUtils;
import vn.aeoc.entity.data.mysql.auth.Permission;
import vn.aeoc.entity.data.mysql.auth.User;
import vn.aeoc.entity.data.pojo.EBPermissionResource;

import java.security.Principal;
import java.util.List;

@RestController
public class PermissionController extends BaseController {
    private static final String URI = "/permission/";

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService, UserService userService) {
        super(userService);
        this.permissionService = permissionService;
    }

    /**
     * Danh sách permission theo roleId (hoặc objectId)
     */
    @GetMapping("/permission/list")
    public ResponseEntity<ApiResponse<List<Permission>>> listPermissions(
            @RequestParam(required = false) Integer objectId,
            Pageable pageable,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = permissionService.getPermissionByCriteria(objectId);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Thêm hoặc cập nhật permission (upsert)
     */
    @PostMapping("/permission/update")
    public ResponseEntity<ApiResponse<Permission>> upsertPermission(
            @RequestBody Permission permission,
            Principal principal
    ) {
        String action = (CommonUtils.NVL(permission.getId()) <= 0) ? "ADD" : "UPDATE";
        checkPermission(principal, URI, action);

        Permission result;
        if (CommonUtils.NVL(permission.getId()) <= 0) {
            result = permissionService.insertPermission(permission);
        } else {
            result = permissionService.updatePermission(permission);
        }
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Thêm mới quyền cho role (ADD action)
     */
    @PostMapping("/permission/add")
    public ResponseEntity<ApiResponse<Permission>> addPermission(
            @RequestBody Permission permission,
            Principal principal
    ) {
        checkPermission(principal, URI, "ADD");
        var result = permissionService.updatePermission(permission, "ADD");
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Gỡ bỏ quyền khỏi role (DELETE action)
     */
    @PostMapping("/permission/remove")
    public ResponseEntity<ApiResponse<Permission>> removePermission(
            @RequestBody Permission permission,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = permissionService.updatePermission(permission, "DELETE");
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Thêm nhiều permission cùng lúc (batch insert)
     */
    @PostMapping("/permission/batch-insert")
    public ResponseEntity<ApiResponse<List<Permission>>> insertBatchPermission(
            @RequestBody List<Permission> permissions,
            Principal principal
    ) {
        checkPermission(principal, URI, "ADD");
        var result = permissionService.insertBatchPermission(permissions);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Xóa permission theo ID
     */
    @DeleteMapping("/permission/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> deletePermission(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = permissionService.deletePermission(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }


    /**
     * Lấy danh sách resource + actions mà roleId có quyền
     */
    @GetMapping("/permission/resource")
    public ResponseEntity<ApiResponse<List<EBPermissionResource>>> getPermissionResources(
            Principal principal
    ) {
        User user = userService.getUserById(SecurityUtils.userId(principal));
        var result = permissionService.getPermissionResources(user.getRoleId());
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Lấy thông tin trả về dùng để phân quyền
     *
     * @param roleId
     * @param name         Tên tài nguyên (resource)
     * @param principal
     * @return
     */
    @GetMapping("/permission/info")
    public ResponseEntity<ApiResponse<List<ResourceInfo>>> getPermissionInfo(
            @RequestParam Integer roleId,
            @RequestParam(required = false) String name,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = permissionService.getResourceInfo(roleId, name);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
