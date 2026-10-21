package vn.aeoc.crm.repository.auth;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.entity.data.mysql.auth.Module;
import vn.entity.backend.tables.records.ModuleRecord;

import java.util.List;

import static vn.entity.backend.tables.Module.MODULE;

@Repository
public class ModuleRepository extends AbsMysqlRepository<ModuleRecord, Module, Integer> {
    public ModuleRepository(DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    protected TableImpl<ModuleRecord> getTable() {
        return MODULE;
    }

    private Condition getWhereCondition(String keyword, Integer active) {
        Condition condition = DSL.trueCondition();
        if (StringUtils.isNotBlank(keyword)) {
            String query = "%" + keyword.trim().toLowerCase() + "%";
            condition = condition.and(DSL.lower(MODULE.NAME).like(query)
                    .or(DSL.lower(MODULE.CODE).like(query)));
        }
        if (active != null) {
            condition = condition.and(MODULE.ACTIVE.eq(active));
        }
        return condition;
    }

    public List<Module> getModuleByCriteria(String keyword, Integer active, Pageable pageable) {
        return getListByCriteria(getWhereCondition(keyword, active), MODULE.ID.desc(), pageable);
    }

    public long countModuleByCriteria(String keyword, Integer active) {
        return getCountWithCriteria(getWhereCondition(keyword, active));
    }

    public boolean existsCodeExceptId(String code, Integer id) {
        Condition condition = MODULE.CODE.eq(code);
        if (id != null) condition = condition.and(MODULE.ID.ne(id));
        return exists(condition);
    }

    public Integer updateModuleStatus(Integer id, Integer active) {
        return updateField(id, MODULE.ACTIVE, active);
    }
}
