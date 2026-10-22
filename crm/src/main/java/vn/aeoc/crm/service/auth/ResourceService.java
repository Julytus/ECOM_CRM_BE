package vn.aeoc.crm.service.auth;

import vn.aeoc.base.util.QueryExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.auth.ResourceRepository;
import vn.aeoc.crm.repository.auth.ModuleRepository;
import vn.aeoc.entity.data.mysql.auth.Resource;
import vn.aeoc.base.exception.AppException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j(topic = "RESOURCE-SERVICE")
@Service
public class ResourceService  extends BaseService<
        ResourceRepository,
        Resource,
        Integer> {

    private final ModuleRepository moduleRepository;
    private final PermissionService permissionService;


    @Autowired
    protected ResourceService(ResourceRepository repository, ModuleRepository moduleRepository,
                              QueryExecutor queryExecutor, PermissionService permissionService) {
        super(repository, queryExecutor);
        this.moduleRepository = moduleRepository;
        this.permissionService = permissionService;
    }

    protected ResourceService(ResourceRepository repository, ModuleRepository moduleRepository,
                              QueryExecutor queryExecutor) {
        this(repository, moduleRepository, queryExecutor, null);
    }

    private void evictPermissionCache() {
        if (permissionService != null) permissionService.evictCheckPermissionCache();
    }

    public Page<Resource> getListResourceByCriteria(String name, Integer active, Integer moduleId,
                                                    Boolean unassigned, Pageable pageable) {

        return pageQuery(
                pageable,
                () -> repository.getResourceByCriteria(name, active, moduleId, unassigned, pageable),
                () -> repository.countResourceByCriteria(name, active, moduleId, unassigned)
        );
    }

    @Override
    public Resource insert(Resource resource) {
        validateModule(resource.getModuleId());
        Resource inserted = repository.insertResource(resource);
        evictPermissionCache();
        return inserted;
    }

    @Override
    public Resource update(Integer id, Resource resource) {
        validateModule(resource.getModuleId());
        resource.setId(id);
        Resource updated = repository.updateResource(resource);
        evictPermissionCache();
        return updated;
    }

    private void validateModule(Integer moduleId) {
        if (moduleId != null && !moduleRepository.existsById(moduleId)) {
            throw new AppException("Module không tồn tại", BAD_REQUEST.value());
        }
    }


    public int updateResourceStatus(Integer id, Integer active) {
        int updated = repository.updateResourceStatus(id, active);
        evictPermissionCache();
        return updated;
    }
}
