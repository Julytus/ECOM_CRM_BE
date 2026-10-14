package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.sale.Unit;
import vn.entity.backend.tables.records.UnitRecord;

import java.util.List;

import static vn.entity.backend.tables.Unit.UNIT;

@Repository
public class UnitRepository extends AbsMysqlRepository<UnitRecord, Unit, Integer> {
    public UnitRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<UnitRecord> getTable() {
        return UNIT;
    }

    private Condition getWhereCondition(String name, Integer status, Integer branchId) {
        Condition condition = DSL.trueCondition();

        condition = condition.and(BranchFilter.branchEq(UNIT.BRANCH_ID, CommonUtils.NVL(branchId)));

        if (StringUtils.isNotBlank(name)) {
            condition = condition.and(DSL.lower(UNIT.NAME)
                    .like("%" + name.trim().toLowerCase() + "%"));
        }

        if (CommonUtils.NVL(status) >  0) {
            condition = condition.and(UNIT.STATUS.eq(status));
        }

        return condition;
    }

    public List<Unit> getUnitByCriteria(String name, Integer status, Integer branchId, Pageable pageable) {
        Condition condition = getWhereCondition(name, status, branchId);
        SelectConditionStep<?> select =  dslContext.select(getFields())
                .from(UNIT)
                .where(condition);
        return select
                .orderBy(UNIT.POSITION.asc(), UNIT.ID.asc())
                .limit(pageable.getLimit())
                .offset(pageable.getOffset())
                .fetchInto(Unit.class);
    }

    public Long countUnitByCriteria(String name, Integer status, Integer branchId) {
        Condition condition = getWhereCondition(name, status, branchId);

        return dslContext.selectCount()
                .from(UNIT)
                .where(condition)
                .fetchOne(0, Long.class);
    }

    public Integer updateUnitStatus(Integer id, Integer status) {
        return dslContext.update(UNIT)
                .set(UNIT.STATUS, status)
                .where(UNIT.ID.eq(id))
                .execute();
    }


}
