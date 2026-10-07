package vn.aeoc.crm.controller.other;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.other.NotificationService;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.crm.util.SecurityUtils;
import vn.aeoc.entity.data.mysql.Notification;
import vn.aeoc.entity.data.mysql.User;

import java.security.Principal;

@RestController
public class NotificationController extends BaseController {
    @Autowired
    private NotificationService service;

    @Autowired
    private UserService userService;

    @GetMapping("/notification/list")
    public ResponseEntity<ApiResponse<Page<Notification>>> list(
            @RequestParam(required = false) Integer isRead,
            Pageable pageable,
            Principal principal
    ) {
        Integer loginId = SecurityUtils.userId(principal);
        User login = userService.getUserById(loginId);
        var result = service.getByCriteria(
                loginId,
                isRead,
                BranchFilter.listScopeBranchId(login),
                pageable
        );

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/notification/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(
            Principal principal
    ) {
        Integer loginId = SecurityUtils.userId(principal);
        User login = userService.getUserById(loginId);
        Integer branchId = BranchFilter.listScopeBranchId(login);

        return Mapper.map(
                service.getUnreadCount(loginId, branchId),
                ApiResponse::okEntity
        );
    }

    @GetMapping("/notification/get")
    public ResponseEntity<ApiResponse<Notification>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        return Mapper.map(
                service.getById(id),
                ApiResponse::okEntity
        );
    }

    @PostMapping("/notification/update")
    public ResponseEntity<ApiResponse<Notification>> update(
            @RequestBody Notification notification,
            Principal principal
    ) {
        Notification result = service.saveAndNotify(notification);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/notification/mark-read")
    public ResponseEntity<ApiResponse<Integer>> updateStatus(
            @RequestParam Integer id,
            Principal principal
    ) {

        Integer loginId = SecurityUtils.userId(principal);
        User login = userService.getUserById(loginId);
        Integer branchId = BranchFilter.listScopeBranchId(login);

        return Mapper.map(
                service.markRead(id, loginId, branchId),
                ApiResponse::okEntity
        );
    }

    @PostMapping("/notification/mark-all-read")
    public ResponseEntity<ApiResponse<Integer>> updateStatus(
            Principal principal
    ) {
        Integer loginId = SecurityUtils.userId(principal);
        User login = userService.getUserById(loginId);
        Integer branchId = BranchFilter.listScopeBranchId(login);

        return Mapper.map(
                service.markAllRead(loginId, branchId),
                ApiResponse::okEntity
        );
    }

    @DeleteMapping("/notification/delete")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @RequestParam Integer id,
            Principal principal
    ) {
        Integer loginId = SecurityUtils.userId(principal);
        User login = userService.getUserById(loginId);
        Integer branchId = BranchFilter.listScopeBranchId(login);

        return Mapper.map(
                service.deleteNotification(id, loginId, branchId),
                ApiResponse::okEntity
        );
    }

}