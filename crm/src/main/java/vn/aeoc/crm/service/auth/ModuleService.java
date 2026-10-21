package vn.aeoc.crm.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.repository.auth.ModuleRepository;
import vn.aeoc.crm.repository.auth.ResourceRepository;
import vn.aeoc.entity.data.mysql.auth.Module;
import vn.aeoc.entity.data.mysql.auth.Resource;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j(topic = "MODULE-SERVICE")
@Service
public class ModuleService extends BaseService<ModuleRepository, Module, Integer> {
    private final ResourceRepository resourceRepository;

    public ModuleService(ModuleRepository repository, ResourceRepository resourceRepository,
                         QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
        this.resourceRepository = resourceRepository;
    }

    public Page<Module> getListModuleByCriteria(String keyword, Integer active, Pageable pageable) {
        return pageQuery(pageable,
                () -> repository.getModuleByCriteria(keyword, active, pageable),
                () -> repository.countModuleByCriteria(keyword, active));
    }

    public Module upsert(Module module) {
        if (repository.existsCodeExceptId(module.getCode(), module.getId())) {
            throw new AppException("Mã module đã tồn tại", BAD_REQUEST.value());
        }
        if (module.getId() == null || module.getId() <= 0) {
            return repository.insertReturning(module);
        }
        return repository.update(module.getId(), module);
    }

    public Integer updateModuleStatus(Integer id, Integer active) {
        return repository.updateModuleStatus(id, active);
    }

    public Integer deleteModule(Integer id) {
        if (resourceRepository.countResourcesByModuleId(id) > 0) {
            throw new AppException("Không thể xóa module đang có resource", BAD_REQUEST.value());
        }
        return repository.delete(id);
    }

    public List<Resource> getResources(Integer moduleId) {
        if (!repository.existsById(moduleId)) {
            throw new AppException("Module không tồn tại", BAD_REQUEST.value());
        }
        return resourceRepository.getResourcesByModuleId(moduleId);
    }

    @Transactional
    public int assignResources(Integer moduleId, List<Integer> resourceIds) {
        List<Integer> ids = normalizeIds(resourceIds);
        if (moduleId == null || moduleId <= 0 || !repository.existsById(moduleId)) {
            throw new AppException("Module không tồn tại", BAD_REQUEST.value());
        }
        if (ids.isEmpty() || resourceRepository.countResourcesByIds(ids) != ids.size()) {
            throw new AppException("Một hoặc nhiều resource không tồn tại", BAD_REQUEST.value());
        }
        return resourceRepository.assignResources(moduleId, ids);
    }

    @Transactional
    public int unassignResources(List<Integer> resourceIds) {
        List<Integer> ids = normalizeIds(resourceIds);
        if (ids.isEmpty() || resourceRepository.countResourcesByIds(ids) != ids.size()) {
            throw new AppException("Một hoặc nhiều resource không tồn tại", BAD_REQUEST.value());
        }
        return resourceRepository.unassignResources(ids);
    }

    private List<Integer> normalizeIds(List<Integer> resourceIds) {
        if (resourceIds == null) return List.of();
        if (resourceIds.stream().anyMatch(id -> id == null || id <= 0)) return List.of();
        return resourceIds.stream().distinct().toList();
    }
}
