package vn.aeoc.crm.service.sale;

import vn.aeoc.base.util.QueryExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.sale.CategoryItemRepository;
import vn.aeoc.entity.data.mysql.sale.CategoryItem;

import java.util.List;

@Slf4j(topic = "CATEGORY-ITEM-SERVICE")
@Service
public class CategoryItemService extends BaseService<CategoryItemRepository, CategoryItem, Integer> {
    protected CategoryItemService(CategoryItemRepository repository, QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
    }

    public Page<CategoryItem> getListCategoryItem(String keyword, Integer type, Integer active,
                                                  Integer level, Integer branchId, Pageable pageable) {

        return pageQuery(
                pageable,
                () -> repository.getByCriteria(keyword, type, active, level, branchId, pageable),
                () -> repository.countByCriteria(keyword, type, active, level, branchId)
        );
    }

    public List<CategoryItem> getChild(Integer parentId, Integer branchId) {
        return repository.getChild(parentId, branchId);
    }

    public Integer updateStatus(Integer id, Integer active) {
        return repository.updateStatus(id, active);
    }
}
