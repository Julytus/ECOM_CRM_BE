package vn.aeoc.crm.service.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import vn.aeoc.crm.data.pojo.PermissionInfo;
import vn.aeoc.crm.data.pojo.ResourceInfo;
import vn.aeoc.crm.repository.auth.PermissionRepository;
import vn.aeoc.crm.repository.auth.ResourceRepository;
import vn.aeoc.crm.repository.auth.RoleRepository;
import vn.aeoc.crm.service.cache.CacheKeyFactory;
import vn.aeoc.entity.data.mysql.auth.Permission;
import vn.aeoc.entity.data.mysql.auth.Resource;
import vn.aeoc.entity.data.mysql.auth.Role;
import vn.aeoc.entity.data.pojo.EBPermissionResource;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final ResourceRepository resourceRepository;
    private final RoleRepository roleRepository;
    private final Gson gson;
    private final Cache<String, Object> authCache;

    public PermissionService(
            PermissionRepository permissionRepository,
            ResourceRepository resourceRepository,
            RoleRepository roleRepository,
            Gson gson,
            @Qualifier("authCache") Cache<String, Object> authCache
    ) {
        this.permissionRepository = permissionRepository;
        this.resourceRepository = resourceRepository;
        this.roleRepository = roleRepository;
        this.gson = gson;
        this.authCache = authCache;
    }

    public List<Permission> getPermissionByCriteria(Integer objectId) {
        return permissionRepository.getPermissionByCriteria(objectId);
    }

    public Permission getPermissionById(Integer permissionId) {
        return permissionRepository.getPermissionById(permissionId);
    }

    public Permission insertPermission(Permission permission) {
        Permission inserted = permissionRepository.insertPermission(permission);
        evictCheckPermissionCache();
        return inserted;
    }

    public List<Permission> insertBatchPermission(List<Permission> lstPermission) {
        List<Permission> inserted = permissionRepository.insertBatchPermission(lstPermission);
        if (lstPermission != null && !lstPermission.isEmpty()) evictCheckPermissionCache();
        return inserted;
    }

    public Permission updatePermission(Permission permission) {
        Permission updated = permissionRepository.updatePermission(permission);
        evictCheckPermissionCache();
        return updated;
    }

    public Integer deletePermission(Integer id) {
        Integer deleted = permissionRepository.deletePermission(id);
        if (deleted != null && deleted > 0) evictCheckPermissionCache();
        return deleted;
    }

    public List<EBPermissionResource> getPermissionResources(Integer roleId) {
        Role role = roleRepository.getById(roleId);
        if (role == null) {
            return List.of();
        }
        return permissionRepository.getPermissionResources(roleId, role.getIsOperator());
    }

    public long checkPermission(Integer roleId, String uri, String action) {
        String cacheKey = CacheKeyFactory.authPermission(roleId, uri, action);
        Long count = (Long) authCache.get(cacheKey,
                key -> permissionRepository.checkPermission(roleId, uri, action));
        return count == null ? 0L : count;
    }

    /** Evict all permission decisions observed by this instance after a resource URI/action change. */
    public void evictCheckPermissionCache() {
        authCache.invalidateAll();
    }

    /**
     * Thêm hoặc cập nhật lại quyền
     */
    public Permission updatePermission(Permission permission, String action) {
        Permission permissionDb = permissionRepository.getPermissionActions(
                permission.getResourceId(), permission.getRoleId());

        if (permissionDb == null) {
            if ("ADD".equals(action)) {
                Permission inserted = permissionRepository.insertPermission(permission);
                evictCheckPermissionCache();
                return inserted;
            }
            return permission;
        }

        Type listType = new TypeToken<ArrayList<String>>() {
        }.getType();

        List<String> lstOldAction = parseActions(permissionDb.getActions(), listType);
        List<String> lstNewAction = parseActions(permission.getActions(), listType);

        Set<String> resultSet = new LinkedHashSet<>(lstOldAction);
        if ("ADD".equals(action)) {
            resultSet.addAll(lstNewAction);
        } else {
            resultSet.removeAll(lstNewAction);
        }

        permissionDb.setActions(gson.toJson(new ArrayList<>(resultSet)));
        Permission updated = permissionRepository.updatePermission(permissionDb);
        evictCheckPermissionCache();
        return updated;
    }

    public List<ResourceInfo> getResourceInfo(Integer roleId, String name) {
        List<Resource> resources = resourceRepository.getAllResources(name);
        List<Permission> permissions = permissionRepository.getPermissionByCriteria(roleId);

        Map<Integer, Permission> permissionByResourceId = permissions.stream()
                .filter(permission -> permission.getResourceId() != null)
                .collect(Collectors.toMap(
                        Permission::getResourceId,
                        permission -> permission,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<ResourceInfo> result = new ArrayList<>(resources.size());
        for (Resource resource : resources) {
            ResourceInfo info = new ResourceInfo(resource);

            Permission matched = permissionByResourceId.get(resource.getId());
            PermissionInfo permInfo = new PermissionInfo();
            permInfo.setRoleId(roleId);
            permInfo.setActions(matched != null && StringUtils.isNotBlank(matched.getActions())
                    ? matched.getActions()
                    : "[]");

            info.setPermission(permInfo);
            result.add(info);
        }

        return result;
    }

    private List<String> parseActions(String actionsJson, Type listType) {
        if (StringUtils.isBlank(actionsJson)) {
            return List.of();
        }

        try {
            List<String> parsed = gson.fromJson(actionsJson, listType);
            return parsed == null ? List.of() : parsed;
        } catch (Exception ex) {
            return List.of();
        }
    }
}
