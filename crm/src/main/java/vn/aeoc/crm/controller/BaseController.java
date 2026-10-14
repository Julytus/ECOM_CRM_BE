package vn.aeoc.crm.controller;

import org.springframework.stereotype.Service;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.util.SecurityUtils;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static vn.aeoc.crm.util.SecurityUtils.userId;

@Service
public class BaseController {
    protected final UserService userService;

    protected BaseController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Kiểm tra quyền
     *
     * @param principal
     * @param URI
     * @param action
     * @return
     */
    public User checkPermission(Principal principal, String URI, String action) {
        if (SecurityUtils.isApiKeyPrincipal(principal)) {
            return SecurityUtils.buildApiKeyOperator();
        }

        Integer loginId = userId(principal);
        User user = userService.checkPermission(loginId, URI, action);
        checkUserPermission(URI, action, user);
        return user;
    }

    private static User checkUserPermission(String URI, String action, User user) {
        if (user.getId() == null) {
            throw new AppException(
                    String.format("Bạn không có quyền thực hiện thao tác này! Path: %s, Action: %s", URI, action),
                    BAD_REQUEST.value());
        }
        return user;
    }

    private String getMessageError(Map<String, String> mapParam, String message) {
        for (Map.Entry<String, String> entry : mapParam.entrySet()) {
            message = message.replaceAll(String.format(":%s:", entry.getKey()), entry.getValue());
        }

        return message;
    }
}
