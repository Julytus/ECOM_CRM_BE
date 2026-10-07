package vn.aeoc.crm.repository.other;

import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.EmailTemplate;
import vn.entity.backend.tables.records.EmailTemplatesRecord;

import java.util.List;

import static vn.entity.backend.tables.EmailTemplates.EMAIL_TEMPLATES;

@Repository
public class EmailTemplateRepository
        extends AbsMysqlRepository<EmailTemplatesRecord, EmailTemplate, Integer> {

    @Override
    protected TableImpl<EmailTemplatesRecord> getTable() {
        return EMAIL_TEMPLATES;
    }

    /* ==============================
            CONDITION
       ============================== */

    private Condition getWhereCondition(Integer id, String name, Integer branchId) {

        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(EMAIL_TEMPLATES.BRANCH_ID.eq(branchId));
        }

        if (CommonUtils.NVL(id) > 0) {
            condition = condition.and(EMAIL_TEMPLATES.ID.eq(id));
        }

        if (!CommonUtils.NVL(name).isEmpty()) {
            condition = condition.and(
                    EMAIL_TEMPLATES.NAME.likeIgnoreCase("%" + name.trim() + "%")
            );
        }

        return condition;
    }

    /* ==============================
            QUERY
       ============================== */

    public List<EmailTemplate> getByCriteria(
            Integer id,
            String name,
            Integer branchId,
            Pageable pageable
    ) {
        return getListByCriteria(
                getWhereCondition(id, name, branchId),
                EMAIL_TEMPLATES.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(Integer id, String name, Integer branchId) {
        return getCountWithCriteria(
                getWhereCondition(id, name, branchId)
        );
    }

    public List<EmailTemplate> getAll(Integer branchId) {
        return getListByCriteria(
                getWhereCondition(0, "", branchId),
                EMAIL_TEMPLATES.ID.desc()
        );
    }

    public EmailTemplate getById(Integer id) {
        return super.getById(id);
    }

    /* ==============================
            COMMAND
       ============================== */

    public EmailTemplate insertReturning(EmailTemplate entity) {
        return super.insertReturning(entity);
    }

    public EmailTemplate update(Integer id, EmailTemplate entity) {
        return super.update(id, entity);
    }

    public Integer delete(Integer id) {
        return super.delete(id);
    }
}