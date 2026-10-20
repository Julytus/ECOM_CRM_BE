package vn.aeoc.crm.repository.auth;

import org.jooq.DSLContext;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.SelectOffsetStep;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.auth.Resource;
import vn.entity.backend.tables.records.ResourceRecord;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static vn.entity.backend.tables.Resource.RESOURCE;

@Repository
public class ResourceRepository extends AbsMysqlRepository<ResourceRecord, Resource, Integer> {
    public ResourceRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<ResourceRecord> getTable() {
        return RESOURCE;
    }

    private Condition getWhereCondition(String name, Integer active) {
        Condition condition = DSL.trueCondition();
        if (!StringUtils.isBlank(name)) {
            condition = condition.and(DSL.lower(RESOURCE.NAME)
                    .like(DSL.val("%" + name.trim().toLowerCase() + "%")));
        }

        if (CommonUtils.NVL(active) > 0) {
            condition = condition.and(RESOURCE.ACTIVE.eq(active));
        }

        return condition;
    }

    /**
     * Lấy theo điều kiện
     */
    public List<Resource> getResourceByCriteria(String name,
                                                Integer active, Pageable pageable) {
        return getListByCriteria(getWhereCondition(name, active), RESOURCE.ID.desc(), pageable);
    }

    /**
     * Đếm số resource theo điều kiện
     */
    public long countResourceByCriteria(String name, Integer active) {
        return getCountWithCriteria(getWhereCondition(name, active));

    }

    public Integer updateResourceStatus(Integer id, Integer active) {
        return updateField(id, RESOURCE.ACTIVE, active);
    }

    /**
     * Lấy ra toàn bộ để tính toán phân quyền
     */
    public List<Resource> getAllResources(String name) {
        SelectOffsetStep<Record> select = dslContext
                .select(getFiled())
                .from(RESOURCE)
                .where(
                        (StringUtils.isBlank(name) ? DSL.trueCondition() : DSL.lower(RESOURCE.NAME)
                                .like(DSL.val("%" + name.trim().toLowerCase() + "%")))
                )
                .limit(1000);

        return select.fetchInto(Resource.class);
    }

    public Resource getResourceById(Integer resourceId) {
        return dslContext
                .select()
                .from(RESOURCE)
                .where(RESOURCE.ID.eq(resourceId))
                .fetchOptionalInto(Resource.class)
                .orElseThrow(() -> new AppException("Resource not found", BAD_REQUEST.value()));
    }

    public Resource insertResource(Resource resource) {
        dslContext
                .insertInto(RESOURCE, getFields())
                .values(
                        null,
                        DSL.value(resource.getName()),
                        DSL.value(resource.getDescription()),
                        DSL.value(resource.getCode()),
                        DSL.value(resource.getUri()),
                        DSL.value(CommonUtils.getJSON(resource.getActions())),
                        DSL.value(CommonUtils.NVL(resource.getActive()) > 0 ? resource.getActive() : 1)
                )
                .execute();
        resource.setId(dslContext.lastID().intValue());
        return resource;
    }

    public Resource updateResource(Resource resource) {
        dslContext.update(RESOURCE)
                .set(RESOURCE.NAME, resource.getName())
                .set(RESOURCE.DESCRIPTION, resource.getDescription())
                .set(RESOURCE.CODE, resource.getCode())
                .set(RESOURCE.URI, resource.getUri())
                .set(RESOURCE.ACTIONS, CommonUtils.getJSON(resource.getActions()))
                .where(RESOURCE.ID.eq(resource.getId()))
                .execute();
        return resource;
    }

    public Integer deleteResource(Integer id) {
        return dslContext
                .delete(RESOURCE)
                .where(RESOURCE.ID.eq(id))
                .execute();
    }
}
