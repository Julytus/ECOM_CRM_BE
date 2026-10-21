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

    private Condition getWhereCondition(String name, Integer active, Integer moduleId, Boolean unassigned) {
        Condition condition = DSL.trueCondition();
        if (!StringUtils.isBlank(name)) {
            condition = condition.and(DSL.lower(RESOURCE.NAME)
                    .like(DSL.val("%" + name.trim().toLowerCase() + "%")));
        }

        if (CommonUtils.NVL(active) > 0) {
            condition = condition.and(RESOURCE.ACTIVE.eq(active));
        }

        // When both filters are supplied, unassigned takes precedence over moduleId.
        if (Boolean.TRUE.equals(unassigned)) {
            condition = condition.and(RESOURCE.MODULE_ID.isNull());
        } else if (moduleId != null) {
            condition = condition.and(RESOURCE.MODULE_ID.eq(moduleId));
        }

        return condition;
    }

    /**
     * Lấy theo điều kiện
     */
    public List<Resource> getResourceByCriteria(String name,
                                                Integer active, Integer moduleId, Boolean unassigned, Pageable pageable) {
        return getListByCriteria(getWhereCondition(name, active, moduleId, unassigned), RESOURCE.ID.desc(), pageable);
    }

    /**
     * Đếm số resource theo điều kiện
     */
    public long countResourceByCriteria(String name, Integer active, Integer moduleId, Boolean unassigned) {
        return getCountWithCriteria(getWhereCondition(name, active, moduleId, unassigned));

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
                        DSL.value(CommonUtils.NVL(resource.getActive()) > 0 ? resource.getActive() : 1),
                        DSL.value(resource.getModuleId())
                )
                .execute();
        resource.setId(dslContext.lastID().intValue());
        return resource;
    }

    public Resource updateResource(Resource resource) {
        var update = dslContext.update(RESOURCE)
                .set(RESOURCE.NAME, resource.getName())
                .set(RESOURCE.DESCRIPTION, resource.getDescription())
                .set(RESOURCE.CODE, resource.getCode())
                .set(RESOURCE.URI, resource.getUri())
                .set(RESOURCE.ACTIONS, CommonUtils.getJSON(resource.getActions()));
        if (resource.getActive() != null) {
            update = update.set(RESOURCE.ACTIVE, resource.getActive());
        }
        if (resource.getModuleId() != null) {
            update = update.set(RESOURCE.MODULE_ID, resource.getModuleId());
        }
        update.where(RESOURCE.ID.eq(resource.getId())).execute();
        return resource;
    }

    public Integer deleteResource(Integer id) {
        return dslContext
                .delete(RESOURCE)
                .where(RESOURCE.ID.eq(id))
                .execute();
    }

    public List<Resource> getResourcesByModuleId(Integer moduleId) {
        return dslContext.select(getFiled())
                .from(RESOURCE)
                .where(RESOURCE.MODULE_ID.eq(moduleId))
                .orderBy(RESOURCE.ID.desc())
                .fetchInto(Resource.class);
    }

    public long countResourcesByModuleId(Integer moduleId) {
        return dslContext.selectCount().from(RESOURCE)
                .where(RESOURCE.MODULE_ID.eq(moduleId)).fetchOne(0, long.class);
    }

    public long countResourcesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        return dslContext.selectCount().from(RESOURCE)
                .where(RESOURCE.ID.in(ids)).fetchOne(0, long.class);
    }

    public int assignResources(Integer moduleId, List<Integer> ids) {
        return dslContext.update(RESOURCE).set(RESOURCE.MODULE_ID, moduleId)
                .where(RESOURCE.ID.in(ids)).execute();
    }

    public int unassignResources(List<Integer> ids) {
        return dslContext.update(RESOURCE).setNull(RESOURCE.MODULE_ID)
                .where(RESOURCE.ID.in(ids)).execute();
    }
}
