package vn.aeoc.crm.repository.auth;

import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.Permission;
import vn.aeoc.entity.data.pojo.EBPermissionResource;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static vn.entity.backend.tables.Permission.PERMISSION;
import static vn.entity.backend.tables.Resource.RESOURCE;

@Repository
public class PermissionRepository {
    @Autowired
    private DSLContext dslContext;

    public static List<Field<?>> getPermissionFields() {
        return asList(
                PERMISSION.ID,
                PERMISSION.RESOURCE_ID,
                PERMISSION.ROLE_ID,
                PERMISSION.ACTIONS
        );
    }

    public List<Permission> getPermissionByCriteria(Integer roleId) {
        SelectConditionStep<Record> select = dslContext
                .select(getPermissionFields())
                .from(PERMISSION)
                .where(
                        (CommonUtils.NVL(roleId) > 0 ? PERMISSION.ROLE_ID.eq(roleId) : DSL.trueCondition()));

        return select.fetchInto(Permission.class);
    }

    public Permission getPermissionById(Integer permissionId) {
        return dslContext
                .select()
                .from(PERMISSION)
                .where(PERMISSION.ID.eq(permissionId))
                .fetchOptionalInto(Permission.class)
                .orElseThrow(() -> new AppException("", BAD_REQUEST.value()));
    }

    public Permission insertPermission(Permission permission) {
        // Kiểm tra và chuẩn hóa chuỗi actions
        String actionsJson = permission.getActions();
        if (actionsJson == null || actionsJson.trim().isEmpty()) {
            actionsJson = "[]"; // Giá trị mặc định nếu actions rỗng
        }

        dslContext
                .insertInto(PERMISSION, getPermissionFields())
                .values(
                        null,
                        DSL.value(permission.getResourceId()),
                        DSL.value(permission.getRoleId()),
                        DSL.value(actionsJson, JSONB.class)
                )
                .execute();
        permission.setId(dslContext.lastID().intValue());
        return permission;
    }

    public List<Permission> insertBatchPermission(List<Permission> lstPermission) {
        if (lstPermission == null || lstPermission.isEmpty()) {
            return new ArrayList<>();
        }

            InsertValuesStepN<?> step = dslContext
                    .insertInto(PERMISSION, getPermissionFields());
            for (Permission permission : lstPermission) {
                step = step.values(
                        null,
                        DSL.value(permission.getResourceId()),
                        DSL.value(permission.getRoleId()),
                        DSL.value(CommonUtils.getJSON(permission.getActions()))
                );
            }

            return step.returning().fetch().into(Permission.class);
    }

    public Permission updatePermission(Permission permission) {
        dslContext.update(PERMISSION)
                .set(PERMISSION.RESOURCE_ID, permission.getResourceId())
                .set(PERMISSION.ROLE_ID, permission.getRoleId())
                .set(PERMISSION.ACTIONS, CommonUtils.getJSON(permission.getActions()))
                .where(PERMISSION.ID.eq(permission.getId()))
                .execute();
        return permission;
    }

    public Integer deletePermission(Integer id) {
        return dslContext
                .delete(PERMISSION)
                .where(PERMISSION.ID.eq(id))
                .execute();
    }

    public List<EBPermissionResource> getPermissionResources(Integer roleId, Integer isOperator) {
        List<Field<?>> selectedFields = asList(RESOURCE.CODE, PERMISSION.ACTIONS);



        if (CommonUtils.NVL(isOperator) >= 1) {
            return dslContext
                    .select(RESOURCE.CODE, RESOURCE.ACTIONS)
                    .from(RESOURCE)
                    .orderBy(RESOURCE.ID.asc())
                    .offset(0)
                    .limit(1000)
                    .fetchInto(EBPermissionResource.class);
        }

        SelectConditionStep<Record> select = dslContext
                .select(selectedFields)
                .from(PERMISSION)
                .leftJoin(RESOURCE).on(PERMISSION.RESOURCE_ID.eq(RESOURCE.ID))
                .where(PERMISSION.ROLE_ID.eq(roleId));



        select.orderBy(PERMISSION.RESOURCE_ID.asc()).offset(0).limit(1000);
        return select.fetchInto(EBPermissionResource.class);
    }

    public Long checkPermission(Integer roleId, String uri, String action) {
        SelectConditionStep<?> select = dslContext
                .selectCount()
                .from(PERMISSION)
                .join(RESOURCE).on(PERMISSION.RESOURCE_ID.eq(RESOURCE.ID))
                .where(
                        PERMISSION.ROLE_ID.eq(roleId)
                                .and(DSL.condition(
                                        "JSON_CONTAINS({0}, {1})",
                                        PERMISSION.ACTIONS,
                                        DSL.inline(String.format("[\"%s\"]", action))
                                ))
                                .and(RESOURCE.URI.eq(uri))
                );


        return select.fetchOne(0, Long.class);
    }


    public Long checkPermission(Integer roleId, Integer resourceId) {
        return dslContext
                .selectCount()
                .from(PERMISSION)
                .where(
                        PERMISSION.ROLE_ID.eq(roleId)
                                .and(PERMISSION.RESOURCE_ID.eq(resourceId))
                )
                .fetchOne(0, Long.class);
    }

    public Permission getPermissionToProcess() {
        return dslContext
                .select(getPermissionFields())
                .from(PERMISSION)
                .limit(1)
                .fetchOptionalInto(Permission.class)
                .orElse(null);
    }

    public int deletePermission(Integer resourceId, Integer roleId, Integer id) {
        return dslContext
                .delete(PERMISSION)
                .where(
                        PERMISSION.RESOURCE_ID.eq(resourceId)
                                .and(PERMISSION.ROLE_ID.eq(roleId))
                                .and(PERMISSION.ID.lessThan(id))
                )
                .execute();
    }

    public Permission getPermissionActions(Integer resourceId, Integer roleId) {
        return dslContext
                .select()
                .from(PERMISSION)
                .where(
                        PERMISSION.RESOURCE_ID.eq(resourceId)
                                .and(PERMISSION.ROLE_ID.eq(roleId))
                )
                .limit(1)
                .fetchOptionalInto(Permission.class)
                .orElse(null);
    }
}
