package vn.aeoc.crm.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.auth.ResourceRepository;
import vn.aeoc.entity.data.mysql.Resource;

@Slf4j(topic = "RESOURCE-SERVICE")
@Service
public class ResourceService  extends BaseService<
        ResourceRepository,
        Resource,
        Integer> {


    protected ResourceService(ResourceRepository repository) {
        super(repository);
    }

    public Page<Resource> getListResourceByCriteria(String name, Integer active, Pageable pageable) {

        return pageQuery(
                pageable,
                () -> repository.getResourceByCriteria(name, active, pageable),
                () -> repository.countResourceByCriteria(name, active)
        );
    }


    public int updateResourceStatus(Integer id, Integer active) {
        return repository.updateResourceStatus(id, active);
    }
}
