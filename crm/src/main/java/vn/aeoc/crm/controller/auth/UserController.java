package vn.aeoc.crm.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Cursor;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.crm.util.SecurityUtils;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;
import java.util.List;

@RestController
public class UserController extends BaseController {
    private static String URI = "/user/";

    public UserController(UserService userService) {
        super(userService);
    }

    /**
     * Danh sách user (có tìm kiếm, phân trang)
     */
    @GetMapping("/user/list")
    public ResponseEntity<ApiResponse<Page<User>>> getAllUsers(
            @RequestParam(required = false) List<Integer> lstUserId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer excludeUserId,
            Pageable pageable,
            Principal principal) {
        User login = checkPermission(principal, URI, "VIEW");
        Integer branchId = BranchFilter.listScopeBranchId(login);
        var result = userService.getListUserByCriteria(lstUserId, branchId, keyword, pageable, excludeUserId);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Lấy thông tin chi tiết user theo ID
     */
    @GetMapping("/user/getById")
    public ResponseEntity<ApiResponse<User>> getUserDetail(
            @RequestParam Integer id,
            @RequestParam(required = false, defaultValue = "false") Boolean hasSensitiveInfo,
            Principal principal) {
        checkPermission(principal, URI, "VIEW");
        var result = userService.getUserById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Thêm hoặc cập nhật user (upsert)
     */
    @PostMapping("/user/update")
    public ResponseEntity<ApiResponse<User>> upsertUser(
            @RequestBody User user,
            Principal principal) {

        String action = (user.getId() == null || user.getId() <= 0) ? "ADD" : "UPDATE";
        checkPermission(principal, URI, action);

        User result;
        if (user.getId() == null || user.getId() <= 0) {
            result = userService.insertUser(user);
        } else {
            result = userService.updateUser(user);
        }

        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Cập nhật mật khẩu người dùng
     */
    @PostMapping("/user/update-password")
    public ResponseEntity<ApiResponse<User>> updatePassword(
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            Principal principal) {

        Integer loginId = SecurityUtils.userId(principal);
        var result = userService.updateUserPassword(loginId, oldPassword, newPassword);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Xóa người dùng
     */
    @DeleteMapping("/user/delete")
    public ResponseEntity<ApiResponse<Integer>> deleteUser(
            @RequestParam Integer id,
            Principal principal) {

        checkPermission(principal, URI, "DELETE");
        var result = userService.deleteUser(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/user/info")
    public ResponseEntity<ApiResponse<User>> getMyInfo(
            Principal principal
    ) {
        Integer loginId = SecurityUtils.userId(principal);
        var result = userService.getUserById(loginId);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/user/info")
    public ResponseEntity<ApiResponse<User>> updateMyInfo(
            Principal principal,
            @RequestBody User user
    ) {
        Integer loginId = SecurityUtils.userId(principal);
        user.setId(loginId);
        var result = userService.updateInfo(user);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/user/update-status")
    public ResponseEntity<ApiResponse<Integer>> updateUserStatus(
            @RequestParam Integer id,
            @RequestParam Integer active,
            Principal principal) {

        checkPermission(principal, "/user/", "UPDATE");

        var result = userService.updateUserStatus(id, active);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/user/list-cursor")
    public ResponseEntity<ApiResponse<Cursor<User>>> listUserCursor(
            @RequestParam(required = false) Integer beforeUserId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Integer excludeUserId,
            Principal principal) {
        checkPermission(principal, "/user/", "VIEW");
        var result = userService.getUserCursor(beforeUserId, keyword, limit, excludeUserId);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
