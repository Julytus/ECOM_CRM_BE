package vn.aeoc.crm.repository.customer;

import org.jooq.Condition;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.CustomerSource;
import vn.entity.backend.tables.records.CustomerSourceRecord;

import java.util.List;

import static org.jooq.impl.DSL.noCondition;
import static vn.entity.backend.tables.CustomerSource.CUSTOMER_SOURCE;

@Repository
public class CustomerSourceRepository
        extends AbsMysqlRepository<CustomerSourceRecord, CustomerSource, Integer> {

    @Override
    protected TableImpl<CustomerSourceRecord> getTable() {
        return CUSTOMER_SOURCE;
    }

    private Condition where(String name, Integer status, Integer branchId) {
        Condition condition = noCondition();

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(CUSTOMER_SOURCE.BRANCH_ID.eq(branchId));
        }

        if (name != null && !name.isBlank()) {
            condition = condition.and(CUSTOMER_SOURCE.NAME.likeIgnoreCase("%" + name + "%"));
        }
        if (status != null) {
            condition = condition.and(CUSTOMER_SOURCE.STATUS.eq(status));
        }
        return condition;
    }

    public List<CustomerSource> getByCriteria(
            String name,
            Integer status,
            Integer branchId,
            Pageable pageable) {

        return getListByCriteria(where(name, status, branchId), CUSTOMER_SOURCE.ID.desc(), pageable);
    }

    public Long countByCriteria(String name, Integer status, Integer branchId) {
        return getCountWithCriteria(where(name, status, branchId));
    }

    public Integer updateStatus(Integer id, Integer status) {
        return updateField(id, CUSTOMER_SOURCE.STATUS, status);

    }
}
