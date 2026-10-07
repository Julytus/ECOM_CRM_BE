package vn.aeoc.crm.repository.other;

import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.Branch;
import vn.entity.backend.tables.records.BranchRecord;

import java.util.List;

import static vn.entity.backend.tables.Branch.BRANCH;

@Repository
public class BranchRepository extends AbsMysqlRepository<BranchRecord, Branch, Integer> {

    @Override
    protected TableImpl<BranchRecord> getTable() {
        return BRANCH;
    }

    /* ==============================
            CONDITION
       ============================== */

    private Condition getWhereCondition(Integer id, String name, Integer status) {

        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(id) > 0) {
            condition = condition.and(BRANCH.ID.eq(id));
        }

        if (!CommonUtils.NVL(name).isEmpty()) {
            condition = condition.and(
                    BRANCH.NAME.likeIgnoreCase("%" + name.trim() + "%")
            );
        }

        if (CommonUtils.NVL(status) > 0) {
            condition = condition.and(BRANCH.STATUS.eq(status));
        }

        return condition;
    }

    /* ==============================
            QUERY
       ============================== */

    public List<Branch> getByCriteria(
            Integer id,
            String name,
            Integer status,
            Pageable pageable
    ) {

        return getListByCriteria(
                getWhereCondition(id, name, status),
                BRANCH.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(Integer id, String name, Integer status) {

        return getCountWithCriteria(
                getWhereCondition(id, name, status)
        );
    }

    /* ==============================
            COMMAND
       ============================== */

    public Integer updateStatus(Integer id, Integer status) {

        return updateField(id, BRANCH.STATUS, status);
    }

}