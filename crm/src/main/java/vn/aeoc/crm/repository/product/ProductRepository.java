package vn.aeoc.crm.repository.product;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.product.Product;
import vn.entity.backend.tables.records.ProductRecord;

import java.util.List;

import static vn.entity.backend.tables.CategoryItem.CATEGORY_ITEM;
import static vn.entity.backend.tables.Product.PRODUCT;

@Repository
public class ProductRepository extends AbsMysqlRepository<ProductRecord, Product, Integer> {
    @Override
    protected TableImpl<ProductRecord> getTable() {
        return PRODUCT;
    }

    private Condition getWhereCondition(Integer id, String name, Integer status,
                                        Integer categoryId, Integer branchId) {

        Condition condition = DSL.trueCondition();

        condition = condition.and(BranchFilter.branchEq(PRODUCT.BRANCH_ID, CommonUtils.NVL(branchId)));

        if (CommonUtils.NVL(id) > 0) {
            condition = condition.and(PRODUCT.ID.eq(id));
        }

        if (CommonUtils.NVL(categoryId) > 0) {
            condition = condition.and(PRODUCT.CATEGORY_ID.eq(categoryId));
        }

        if (!CommonUtils.NVL(name).isEmpty()) {
            condition = condition.and(
                    PRODUCT.NAME.likeIgnoreCase("%" + name.trim() + "%")
            );
        }

        if (CommonUtils.NVL(status) > 0) {
            condition = condition.and(PRODUCT.STATUS.eq(status));
        }

        return condition;
    }

    private List<Field<?>> getJoinFields() {
        return mergeFields(
                CATEGORY_ITEM.NAME.as("category_name")
        );
    }

    private Table<?> getJoinTable() {
        return PRODUCT.leftJoin(CATEGORY_ITEM).on(PRODUCT.CATEGORY_ID.eq(CATEGORY_ITEM.ID));
    }

    public List<Product> getByCriteria(
            Integer id,
            String name,
            Integer status,
            Integer categoryId,
            Integer branchId,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(id, name, status, categoryId, branchId);
        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                condition,
                PRODUCT.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            Integer id,
            String name,
            Integer status,
            Integer categoryId,
            Integer branchId
    ) {
        Condition condition = getWhereCondition(id, name, status, categoryId, branchId);
        return getCountWithCriteria(condition);
    }

    @Override
    public Product getById(Integer id) {
        return getByIdWithJoin(getJoinFields(), getJoinTable(), id);
    }
}
