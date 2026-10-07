package vn.aeoc.crm.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.auth.RoleRepository;
import vn.aeoc.entity.data.mysql.Role;

@Slf4j(topic = "ROLE-SERVICE")
@Service
public class RoleService extends BaseService<
        RoleRepository,
        Role,
        Integer> {

    protected RoleService(RoleRepository repository) {
        super(repository);
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
        return repository.update(role);
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
        return repository.getById(roleId);
    }

    public boolean isOperatorRole(Integer roleId) {
        Role role = getRoleById(roleId);
        return role != null && Integer.valueOf(1).equals(role.getIsOperator());
    }
}
