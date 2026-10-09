# CRUD Template — Spring Boot + jOOQ

> Template được rút ra từ các file `ServiceItem.java`, `ServiceItemService.java`, `ServiceItemRepository.java` và `ServiceItemController.java`.
> Khi tạo CRUD cho entity mới, thay `ServiceItem` và các tên bảng/field tương ứng. Giữ nguyên conventions của project nếu không có yêu cầu khác.

## 1. Cấu trúc

```text
entity/
  <Entity>.java
service/
  <Entity>Service.java
repository/
  <Entity>Repository.java
controller/
  <Entity>Controller.java
```

## 2. Entity

Quy ước trong mẫu:
- Dùng Lombok `@Data`.
- Implement `Serializable`.
- ID và các field số dùng wrapper type (`Integer`, `Long`).
- Field join có thể đặt trong entity để nhận dữ liệu join, ví dụ `categoryName`.
- `createdTime` dùng `Long`.

```java
package <entity_package>;

import lombok.Data;
import java.io.Serializable;

@Data
public class <Entity> implements Serializable {
    private Integer id;

    // Các field nghiệp vụ
    private String name;
    private String code;

    // Ví dụ field liên kết; không nhất thiết là cột của bảng chính
    private Integer categoryId;
    private String categoryName;

    private Long createdTime;
    private Integer branchId;
    private Integer status;
}
```

## 3. Service

Service kế thừa `BaseService<Repository, Entity, ID>` và dùng `pageQuery(...)` để trả danh sách phân trang cùng tổng số bản ghi.

```java
package <service_package>;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.service.BaseService;
import <repository_package>.<Entity>Repository;
import <entity_package>.<Entity>;

@Slf4j(topic = "<ENTITY>-SERVICE")
@Service
public class <Entity>Service extends BaseService<
        <Entity>Repository,
        <Entity>,
        Integer> {

    public <Entity>Service(<Entity>Repository repository) {
        super(repository);
    }

    public Page<<Entity>> getByCriteria(
            String keyword,
            Integer status,
            Integer branchId,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(keyword, status, branchId, pageable),
                () -> repository.countByCriteria(keyword, status, branchId)
        );
    }
}
```

## 4. Repository

Repository kế thừa `AbsMysqlRepository<Record, Entity, ID>`, khai báo bảng jOOQ qua `getTable()`, tách điều kiện WHERE thành hàm riêng và dùng cùng điều kiện cho list/count để tránh lệch kết quả.

```java
package <repository_package>;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.util.BranchFilter;
import <entity_package>.<Entity>;
import <jooq_record_package>.<Record>;

import java.util.List;

import static <jooq_table_package>.<TABLE_CONSTANT>;

@Repository
public class <Entity>Repository
        extends AbsMysqlRepository<<Record>, <Entity>, Integer> {

    @Override
    protected TableImpl<<Record>> getTable() {
        return <TABLE_CONSTANT>;
    }

    private Condition getWhereCondition(
            String keyword,
            Integer status,
            Integer branchId
    ) {
        Condition condition = DSL.trueCondition();

        condition = condition.and(
                BranchFilter.branchEq(<TABLE_CONSTANT>.BRANCH_ID, CommonUtils.NVL(branchId))
        );

        if (!StringUtils.isBlank(keyword)) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            condition = condition.and(
                    DSL.lower(<TABLE_CONSTANT>.NAME).like(kw)
                            .or(DSL.lower(<TABLE_CONSTANT>.CODE).like(kw))
            );
        }

        if (CommonUtils.NVL(status) > 0) {
            condition = condition.and(<TABLE_CONSTANT>.STATUS.eq(status));
        }

        return condition;
    }

    public List<<Entity>> getByCriteria(
            String keyword,
            Integer status,
            Integer branchId,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(keyword, status, branchId);
        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                condition,
                <TABLE_CONSTANT>.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(
            String keyword,
            Integer status,
            Integer branchId
    ) {
        Condition condition = getWhereCondition(keyword, status, branchId);
        return getCountWithCriteria(condition);
    }

    // Chỉ override khi cần JOIN để lấy dữ liệu bổ sung.
    // Nếu không cần JOIN, dùng phương thức truy vấn phù hợp của AbsMysqlRepository.
    private List<org.jooq.Field<?>> getJoinFields() {
        // Ví dụ: return mergeFields(OTHER_TABLE.NAME.as("otherName"));
        return mergeFields();
    }

    private org.jooq.Table<?> getJoinTable() {
        return <TABLE_CONSTANT>;
    }

    // Override getById(...) nếu cần JOIN hoặc mapping đặc biệt.
}
```

### 4.1. JOIN và getById

Trong mẫu nguồn:
- `getJoinFields()` khai báo field bổ sung, alias theo tên property của entity (ví dụ `category_name` cho `categoryName` ở list query).
- `getJoinTable()` khai báo `LEFT JOIN`.
- `getById(id)` có thể tự dựng query, thêm field alias camelCase (ví dụ `categoryName`) rồi dùng `fetchOptionalInto(Entity.class)`.
- Nếu không cần JOIN, không sao chép phần JOIN chỉ để đủ cấu trúc; dùng implementation mặc định nếu `AbsMysqlRepository` hỗ trợ.

## 5. Controller

Theo mẫu nguồn, controller:
- Kế thừa `BaseController`.
- Dùng `checkPermission(principal, URI, action)` để kiểm tra quyền.
- Dùng `Mapper.map(result, ApiResponse::okEntity)` để đóng gói response.
- Endpoint list hỗ trợ `Pageable`.
- Endpoint update dùng chung cho create/update: ID không hợp lệ thì `ADD`, ngược lại `UPDATE`.
- Sau khi lưu/xóa có thể đồng bộ hệ thống ngoài; chỉ giữ phần này nếu entity thực sự cần.

```java
package <controller_package>;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.<module>.<Entity>Service;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import <entity_package>.<Entity>;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;

@RestController
public class <Entity>Controller extends BaseController {

    private static final String URI = "/<resource>/";

    @Autowired
    private <Entity>Service <entityVariable>Service;

    @GetMapping("/<resource>/list")
    public ResponseEntity<ApiResponse<Page<<Entity>>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = <entityVariable>Service.getByCriteria(
                keyword, status, BranchFilter.listScopeBranchId(login), pageable
        );
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/<resource>/get")
    public ResponseEntity<ApiResponse<<Entity>>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = <entityVariable>Service.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/<resource>/update")
    public ResponseEntity<ApiResponse<<Entity>>> update(
            @RequestBody <Entity> request,
            Principal principal
    ) {
        String action = CommonUtils.NVL(request.getId()) <= 0 ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);

        if (CommonUtils.NVL(request.getBranchId()) <= 0
                && CommonUtils.NVL(login.getBranchId()) > 0) {
            request.setBranchId(login.getBranchId());
        }

        <Entity> result = CommonUtils.NVL(request.getId()) <= 0
                ? <entityVariable>Service.insert(request)
                : <entityVariable>Service.update(request.getId(), request);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/<resource>/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = <entityVariable>Service.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
```

## 6. API convention

| Method | Endpoint | Ý nghĩa | Quyền |
|---|---|---|---|
| GET | `/<resource>/list` | Danh sách có phân trang/lọc | `VIEW` |
| GET | `/<resource>/get?id={id}` | Chi tiết | `VIEW` |
| POST | `/<resource>/update` | Tạo mới hoặc cập nhật | `ADD` / `UPDATE` |
| DELETE | `/<resource>/delete/{id}` | Xóa theo ID | `DELETE` |

## 7. Checklist khi tạo CRUD mới

- [ ] Tạo Entity với field đúng schema; không thêm field join vào bảng nếu không phải cột thật.
- [ ] Tạo Service kế thừa `BaseService` và gọi `super(repository)`.
- [ ] Tạo Repository kế thừa `AbsMysqlRepository`; `getTable()` trả đúng bảng jOOQ.
- [ ] Dùng chung `getWhereCondition(...)` cho list và count.
- [ ] Thêm JOIN chỉ khi cần; alias phải khớp property entity.
- [ ] Dùng wrapper type (`Integer`, `Long`) theo convention của project.
- [ ] Dùng `BranchFilter` nếu dữ liệu cần giới hạn theo branch.
- [ ] Kiểm tra quyền `VIEW`, `ADD`, `UPDATE`, `DELETE` ở controller.
- [ ] Chỉ thêm tích hợp ngoài (RAG, Kafka, v.v.) khi nghiệp vụ cần.
- [ ] Kiểm tra hành vi `BaseService`, `AbsMysqlRepository`, `Pageable`, `Mapper` trước khi áp dụng nguyên mẫu vào module khác.
