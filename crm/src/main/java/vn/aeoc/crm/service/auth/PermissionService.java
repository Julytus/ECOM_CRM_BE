package vn.aeoc.crm.service.auth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import vn.aeoc.crm.data.pojo.PermissionInfo;
import vn.aeoc.crm.data.pojo.ResourceInfo;
import vn.aeoc.crm.repository.auth.PermissionRepository;
import vn.aeoc.crm.repository.auth.ResourceRepository;
import vn.aeoc.crm.repository.auth.RoleRepository;
import vn.aeoc.entity.data.mysql.Permission;
import vn.aeoc.entity.data.mysql.Resource;
import vn.aeoc.entity.data.mysql.Role;
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

    public PermissionService(
            PermissionRepository permissionRepository,
            ResourceRepository resourceRepository,
            RoleRepository roleRepository,
            Gson gson
    ) {
        this.permissionRepository = permissionRepository;
        this.resourceRepository = resourceRepository;
        this.roleRepository = roleRepository;
        this.gson = gson;
    }

    public List<Permission> getPermissionByCriteria(Integer objectId) {
        return permissionRepository.getPermissionByCriteria(objectId);
    }

    public Permission getPermissionById(Integer permissionId) {
        return permissionRepository.getPermissionById(permissionId);
    }

    public Permission insertPermission(Permission permission) {
        return permissionRepository.insertPermission(permission);
    }

    public List<Permission> insertBatchPermission(List<Permission> lstPermission) {
        return permissionRepository.insertBatchPermission(lstPermission);
    }

    public Permission updatePermission(Permission permission) {
        return permissionRepository.updatePermission(permission);
    }

    public Integer deletePermission(Integer id) {
        return permissionRepository.deletePermission(id);
    }

    public List<EBPermissionResource> getPermissionResources(Integer roleId) {
        Role role = roleRepository.getById(roleId);
        if (role == null) {
            return List.of();
        }
        return permissionRepository.getPermissionResources(roleId, role.getIsOperator());
    }

    public long checkPermission(Integer roleId, String uri, String action) {
        return permissionRepository.checkPermission(roleId, uri, action);
    }

    /**
     * Thêm hoặc cập nhật lại quyền
     */
    public Permission updatePermission(Permission permission, String action) {
        Permission permissionDb = permissionRepository.getPermissionActions(
                permission.getResourceId(), permission.getRoleId());

        if (permissionDb == null) {
            if ("ADD".equals(action)) {
                return permissionRepository.insertPermission(permission);
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
        return permissionRepository.updatePermission(permissionDb);
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
