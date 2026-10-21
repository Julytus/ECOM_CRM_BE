package vn.aeoc.crm.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.config.security.principal.impl.SimplePrincipal;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.repository.auth.UserRepository;
import vn.aeoc.crm.service.RestService;
import vn.aeoc.entity.data.mysql.auth.Role;
import vn.aeoc.entity.data.mysql.auth.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServicePermissionTest {
    private UserRepository userRepository;
    private PermissionService permissionService;
    private RoleService roleService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        permissionService = mock(PermissionService.class);
        roleService = mock(RoleService.class);
        userService = new UserService(userRepository, permissionService, mock(JWTService.class),
                mock(JavaMailSender.class), mock(RestService.class), roleService);
        User user = new User();
        user.setId(20);
        user.setRoleId(8);
        when(userRepository.getUserById(20)).thenReturn(user);
    }

    @Test
    void operatorPassesWithoutPermissionLookup() {
        Role role = new Role();
        role.setId(8);
        role.setIsOperator(1);
        when(roleService.getRoleById(8)).thenReturn(role);

        User allowed = userService.checkPermission(20, "/module/", "UPDATE");

        assertEquals(20, allowed.getId());
        verifyNoInteractions(permissionService);
    }

    @Test
    void regularRoleWithoutPermissionGetsBadRequest() {
        Role role = new Role();
        role.setId(8);
        role.setIsOperator(0);
        when(roleService.getRoleById(8)).thenReturn(role);
        when(permissionService.checkPermission(8, "/module/", "UPDATE")).thenReturn(0L);
        BaseController controller = new BaseController(userService) { };

        AppException exception = assertThrows(AppException.class,
                () -> controller.checkPermission(SimplePrincipal.builder().id(20).build(), "/module/", "UPDATE"));

        assertEquals(400, exception.getStatusCode());
    }
}
