package vn.aeoc.crm.repository.auth;

import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.auth.Role;
import vn.entity.backend.tables.records.RoleRecord;

import java.util.List;

import static vn.entity.backend.tables.Role.ROLE;

@Repository
public class RoleRepository extends AbsMysqlRepository<RoleRecord, Role, Integer> {
    public RoleRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<RoleRecord> getTable() {
        return ROLE;
    }

    /**
     * Xây dựng điều kiện where động theo các tham số lọc
     */
    private Condition getWhereCondition(Integer id, String name, Integer isDefault, Integer isOperator) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(id) > 0) {
            condition = condition.and(ROLE.ID.eq(id));
        }
        if (!CommonUtils.NVL(name).isEmpty()) {
            condition = condition.and(ROLE.NAME.likeIgnoreCase("%" + name.trim() + "%"));
        }
        if (CommonUtils.NVL(isDefault) > 0) {
            condition = condition.and(ROLE.IS_DEFAULT.eq(isDefault));
        }
        if (CommonUtils.NVL(isOperator) > 0) {
            condition = condition.and(ROLE.IS_OPERATOR.eq(isOperator));
        }
        return condition;
    }

    public List<Role> getRoleByCriteria(Integer id, String name, Integer isDefault, Integer isOperator, Pageable pageable) {
        Condition condition = getWhereCondition(id, name, isDefault, isOperator);
        return getListByCriteria(condition, ROLE.IS_OPERATOR.desc(), pageable);
    }

    public Long countRoleByCriteria(Integer id, String name, Integer isDefault, Integer isOperator) {
        Condition condition = getWhereCondition(id, name, isDefault, isOperator);
        return getCountWithCriteria(condition);
    }

    public Role update(Role role) {
        if (role.getIsDefault() != null && role.getIsDefault() == 1) {
            resetDefaultRole();
        }

        return super.update(role.getId(), role);
    }

    /**
     * Reset is_default = 0 cho toàn bộ role
     */
    private void resetDefaultRole() {
        dslContext.update(ROLE)
                .set(ROLE.IS_DEFAULT, 0)
                .execute();
    }

    public Role getDefaultRole() {
        return getOne(ROLE.IS_DEFAULT.eq(1));
    }

}
