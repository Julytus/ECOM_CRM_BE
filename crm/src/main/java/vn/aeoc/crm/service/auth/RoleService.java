package vn.aeoc.crm.service.auth;

import vn.aeoc.base.util.QueryExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.auth.RoleRepository;
import vn.aeoc.crm.service.cache.CacheKeyFactory;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;
import vn.aeoc.entity.data.mysql.auth.Role;

import java.time.Duration;

@Slf4j(topic = "ROLE-SERVICE")
@Service
public class RoleService extends BaseService<
        RoleRepository,
        Role,
        Integer> {

    private static final Duration AUTH_CACHE_TTL = Duration.ofSeconds(30);
    private final ThreeLayerCacheTemplate cacheTemplate;

    protected RoleService(RoleRepository repository, QueryExecutor queryExecutor,
                          ThreeLayerCacheTemplate cacheTemplate) {
        super(repository, queryExecutor);
        this.cacheTemplate = cacheTemplate;
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
        try {
            cacheTemplate.evict(CacheKeyFactory.authRole(role.getId()));
        } catch (RuntimeException cacheFailure) {
            log.warn("Could not evict auth role cache for {}", role.getId(), cacheFailure);
        }
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
        try {
            Role cached = cacheTemplate.getOrLoad(cacheKey, AUTH_CACHE_TTL,
                    () -> copyRole(repository.getById(roleId)));
            return copyRole(cached);
        } catch (RuntimeException cacheFailure) {
            log.warn("Auth role cache unavailable, loading role {} directly", roleId, cacheFailure);
            return copyRole(repository.getById(roleId));
        }
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
