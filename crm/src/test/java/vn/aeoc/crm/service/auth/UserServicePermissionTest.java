package vn.aeoc.crm.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.repository.auth.PermissionRepository;
import vn.aeoc.crm.repository.auth.ResourceRepository;
import vn.aeoc.crm.repository.auth.RoleRepository;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.config.security.principal.impl.SimplePrincipal;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.repository.auth.UserRepository;
import vn.aeoc.crm.service.RestService;
import vn.aeoc.crm.service.cache.CacheKeyFactory;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;
import vn.aeoc.entity.data.mysql.auth.Role;
import vn.aeoc.entity.data.mysql.auth.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

class UserServicePermissionTest {
    private UserRepository userRepository;
    private PermissionService permissionService;
    private RoleService roleService;
    private UserService userService;
    private RoleRepository roleRepository;
    private PermissionRepository permissionRepository;
    private ThreeLayerCacheTemplate cacheTemplate;
    private final Map<String, Object> cache = new HashMap<>();

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        permissionRepository = mock(PermissionRepository.class);
        cacheTemplate = mock(ThreeLayerCacheTemplate.class);
        cache.clear();
        when(cacheTemplate.getOrLoad(anyString(), any(Duration.class), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if (!cache.containsKey(key)) {
                Supplier<?> loader = invocation.getArgument(2);
                Object loaded = loader.get();
                if (loaded != null) cache.put(key, loaded);
            }
            return cache.get(key);
        });
        doAnswer(invocation -> cache.remove(invocation.getArgument(0)))
                .when(cacheTemplate).evict(anyString());
        doAnswer(invocation -> {
            for (String key : (java.util.List<String>) invocation.getArgument(0)) cache.remove(key);
            return null;
        }).when(cacheTemplate).evictAll(any());
        roleService = new RoleService(roleRepository, mock(QueryExecutor.class), cacheTemplate);
        permissionService = new PermissionService(permissionRepository, mock(ResourceRepository.class),
                roleRepository, new com.google.gson.Gson(), cacheTemplate);
        userService = new UserService(userRepository, permissionService, mock(JWTService.class),
                mock(JavaMailSender.class), mock(RestService.class), roleService,
                mock(vn.aeoc.crm.repository.customer.CustomerRepository.class), cacheTemplate);
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
        when(roleRepository.getById(8)).thenReturn(role);

        User allowed = userService.checkPermission(20, "/module/", "UPDATE");

        assertEquals(20, allowed.getId());
        verifyNoInteractions(permissionRepository);
    }

    @Test
    void regularRoleWithoutPermissionGetsBadRequest() {
        Role role = new Role();
        role.setId(8);
        role.setIsOperator(0);
        when(roleRepository.getById(8)).thenReturn(role);
        when(permissionRepository.checkPermission(8, "/module/", "UPDATE")).thenReturn(0L);
        BaseController controller = new BaseController(userService) { };

        AppException exception = assertThrows(AppException.class,
                () -> controller.checkPermission(SimplePrincipal.builder().id(20).build(), "/module/", "UPDATE"));

        assertEquals(400, exception.getStatusCode());
    }

    @Test
    void repeatedPermissionCheckLoadsEachAuthRecordOnce() {
        Role role = new Role();
        role.setId(8);
        role.setIsOperator(0);
        when(roleRepository.getById(8)).thenReturn(role);
        when(permissionRepository.checkPermission(8, "/module/", "UPDATE")).thenReturn(1L);

        userService.checkPermission(20, "/module/", "UPDATE");
        userService.checkPermission(20, "/module/", "UPDATE");

        verify(userRepository, times(1)).getUserById(20);
        verify(roleRepository, times(1)).getById(8);
        verify(permissionRepository, times(1)).checkPermission(8, "/module/", "UPDATE");
    }

    @Test
    void updateUserEvictsCachedAuthorizationUser() {
        User updated = new User();
        updated.setId(20);
        updated.setRoleId(9);
        User updateRequest = new User();
        updateRequest.setId(20);
        updateRequest.setRoleId(9);
        when(userRepository.updateUser(updateRequest)).thenReturn(updated);

        userService.getUserById(20);
        userService.updateUser(updateRequest);
        when(userRepository.getUserById(20)).thenReturn(updated);
        User reloaded = userService.getUserById(20);

        assertEquals(9, reloaded.getRoleId());
        verify(cacheTemplate).evict(CacheKeyFactory.authUser(20));
        verify(userRepository, times(2)).getUserById(20);
    }

    @Test
    void permissionDeleteEvictsCachedDecision() {
        when(permissionRepository.checkPermission(8, "/module/", "UPDATE")).thenReturn(1L, 0L);
        when(permissionRepository.deletePermission(41)).thenReturn(1);

        assertEquals(1L, permissionService.checkPermission(8, "/module/", "UPDATE"));
        permissionService.deletePermission(41);
        assertEquals(0L, permissionService.checkPermission(8, "/module/", "UPDATE"));

        verify(permissionRepository, times(2)).checkPermission(8, "/module/", "UPDATE");
        verify(cacheTemplate).evictAll(java.util.List.of(CacheKeyFactory.authPermission(8, "/module/", "UPDATE")));
    }

    @Test
    void lockedUserIsReloadedAfterStatusUpdate() {
        User locked = new User();
        locked.setId(20);
        locked.setRoleId(8);
        locked.setActive(0);
        when(userRepository.updateUserStatus(20, 0)).thenReturn(1);
        when(userRepository.getUserById(20)).thenReturn(locked);

        userService.getUserById(20);
        userService.updateUserStatus(20, 0);

        assertEquals(0, userService.getUserById(20).getActive());
        verify(cacheTemplate).evict(CacheKeyFactory.authUser(20));
    }

    @Test
    void deletedUserIsReloadedAfterDelete() {
        User existing = new User();
        existing.setId(20);
        existing.setRoleId(8);
        when(userRepository.getUserById(20)).thenReturn(existing, new User());
        when(userRepository.deleteUser(20)).thenReturn(1);

        userService.getUserById(20);
        userService.deleteUser(20);

        assertNull(userService.getUserById(20).getId());
        verify(cacheTemplate).evict(CacheKeyFactory.authUser(20));
    }

    @Test
    void redisFailureFallsBackToDirectUserLookup() {
        User user = new User();
        user.setId(20);
        when(userRepository.getUserById(20)).thenReturn(user);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(cacheTemplate).getOrLoad(eq(CacheKeyFactory.authUser(20)), eq(Duration.ofSeconds(30)), any());

        assertEquals(20, userService.getUserById(20).getId());

        verify(userRepository, times(1)).getUserById(20);
    }
}
