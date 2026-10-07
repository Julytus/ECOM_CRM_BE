package vn.aeoc.crm.service.other;

import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.crm.repository.other.EmailTemplateRepository;
import vn.aeoc.entity.data.mysql.EmailTemplate;

import java.util.List;

@Service
public class EmailTemplateService
        extends BaseService<EmailTemplateRepository, EmailTemplate, Integer> {

    public EmailTemplateService(EmailTemplateRepository repository) {
        super(repository);
    }

    /* ==============================
            QUERY
       ============================== */

    public Page<EmailTemplate> getByCriteria(
            Integer id,
            String name,
            Integer branchId,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(id, name, branchId, pageable),
                () -> repository.countByCriteria(id, name, branchId)
        );
    }

    public List<EmailTemplate> getAll(Integer branchId) {
        return repository.getAll(branchId);
    }
}