package vn.aeoc.crm.service.auth;

import vn.aeoc.base.util.QueryExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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


    protected ResourceService(ResourceRepository repository, ModuleRepository moduleRepository,
                              QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
        this.moduleRepository = moduleRepository;
    }

    public Page<Resource> getListResourceByCriteria(String name, Integer active, Integer moduleId, Pageable pageable) {

        return pageQuery(
                pageable,
                () -> repository.getResourceByCriteria(name, active, moduleId, pageable),
                () -> repository.countResourceByCriteria(name, active, moduleId)
        );
    }

    @Override
    public Resource insert(Resource resource) {
        validateModule(resource.getModuleId());
        return repository.insertResource(resource);
    }

    @Override
    public Resource update(Integer id, Resource resource) {
        validateModule(resource.getModuleId());
        resource.setId(id);
        return repository.updateResource(resource);
    }

    private void validateModule(Integer moduleId) {
        if (moduleId != null && !moduleRepository.existsById(moduleId)) {
            throw new AppException("Module không tồn tại", BAD_REQUEST.value());
        }
    }


    public int updateResourceStatus(Integer id, Integer active) {
        return repository.updateResourceStatus(id, active);
    }
}
