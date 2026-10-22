package vn.aeoc.crm.service.auth;

import vn.aeoc.base.util.QueryExecutor;

import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Qualifier;
import vn.aeoc.crm.repository.auth.RoleRepository;
import vn.aeoc.crm.service.cache.CacheKeyFactory;
import vn.aeoc.entity.data.mysql.auth.Role;

@Service
public class RoleService extends BaseService<
        RoleRepository,
        Role,
        Integer> {

    private final Cache<String, Object> authCache;

    protected RoleService(RoleRepository repository, QueryExecutor queryExecutor,
                          @Qualifier("authCache") Cache<String, Object> authCache) {
        super(repository, queryExecutor);
        this.authCache = authCache;
    }

    public Page<Role> getListRoleByCriteria(Integer id, String name, Integer isDefault,
                                            Integer isOperator, Pageable pageable) {
        return pageQuery(
                pageable,
                () -> repository.getRoleByCriteria(id, name, isDefault, isOperator, pageable),
                () -> repository.countRoleByCriteria(id, name, isDefault, isOperator)
        );
    }

    public Role update(Role role) {
        Role updated = repository.update(role);
        authCache.invalidate(CacheKeyFactory.authRole(role.getId()));
        return updated;
    }

    public Role getDefaultRole() {
        Role defaultRole = repository.getDefaultRole();
        if (defaultRole == null) {
            throw new AppException(ErrorCode.DEFAULT_ROLE_NOT_FOUND);
        }
        return defaultRole;
    }

    public Role getRoleById(Integer roleId) {
        if (roleId == null || roleId <= 0) {
            return null;
        }
        String cacheKey = CacheKeyFactory.authRole(roleId);
        Role cached = (Role) authCache.get(cacheKey, key -> copyRole(repository.getById(roleId)));
        return copyRole(cached);
    }

    private Role copyRole(Role role) {
        if (role == null) return null;
        Role copy = new Role();
        copy.setId(role.getId());
        copy.setName(role.getName());
        copy.setIsDefault(role.getIsDefault());
        copy.setIsOperator(role.getIsOperator());
        return copy;
    }

    public boolean isOperatorRole(Integer roleId) {
        Role role = getRoleById(roleId);
        return role != null && Integer.valueOf(1).equals(role.getIsOperator());
    }
}
