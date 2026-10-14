package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.sale.CategoryItem;
import vn.entity.backend.tables.records.CategoryItemRecord;

import java.util.List;

import static vn.entity.backend.tables.CategoryItem.CATEGORY_ITEM;

@Repository
public class CategoryItemRepository extends AbsMysqlRepository<CategoryItemRecord, CategoryItem, Integer> {
    public CategoryItemRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<CategoryItemRecord> getTable() {
        return CATEGORY_ITEM;
    }

    private Condition whereCondition(String keyword, Integer type, Integer active, Integer level, Integer branchId) {

        Condition condition = DSL.trueCondition();

        condition = condition.and(BranchFilter.branchEq(CATEGORY_ITEM.BRANCH_ID, CommonUtils.NVL(branchId)));

        if (CommonUtils.NVL(type) > 0) {
            condition = condition.and(CATEGORY_ITEM.TYPE.eq(type));
        }

        if (CommonUtils.NVL(active) >= 0) {
            condition = condition.and(CATEGORY_ITEM.ACTIVE.eq(active));
        }

        // level: 0 = cha, 1 = con, 2 = tất cả
        if (CommonUtils.NVL(level) == 0) {
            condition = condition.and(CATEGORY_ITEM.PARENT_ID.eq(0));
        } else if (CommonUtils.NVL(level) == 1) {
            condition = condition.and(CATEGORY_ITEM.PARENT_ID.gt(0));
        }

        if (StringUtils.isNotBlank(keyword)) {
            condition = condition.and(DSL.lower(CATEGORY_ITEM.NAME)
                    .like("%" + keyword.trim().toLowerCase() + "%"));
        }

        return condition;
    }

    public List<CategoryItem> getByCriteria(String keyword, Integer type, Integer active,
                                            Integer level, Integer branchId, Pageable pageable) {
        return getListByCriteria(whereCondition(keyword, type, active, level, branchId), CATEGORY_ITEM.ID.desc(), pageable);
    }

    public Long countByCriteria(String keyword, Integer type, Integer active, Integer level, Integer branchId) {
        return getCountWithCriteria(whereCondition(keyword, type, active, level, branchId));
    }

    public List<CategoryItem> getChild(Integer parentId, Integer branchId) {
        Condition c = CATEGORY_ITEM.PARENT_ID.eq(parentId)
                .and(BranchFilter.branchEq(CATEGORY_ITEM.BRANCH_ID, CommonUtils.NVL(branchId)));
        return dslContext.select(getFields())
                .from(CATEGORY_ITEM)
                .where(c)
                .orderBy(CATEGORY_ITEM.POSITION.asc(), CATEGORY_ITEM.ID.asc())
                .fetchInto(CategoryItem.class);
    }

    public Integer updateStatus(Integer id, Integer active) {
        return updateField(id, CATEGORY_ITEM.ACTIVE, active);
    }
}
