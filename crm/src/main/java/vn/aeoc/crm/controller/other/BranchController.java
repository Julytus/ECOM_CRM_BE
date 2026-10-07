package vn.aeoc.crm.controller.other;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.other.BranchService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.Branch;

import java.security.Principal;

@RestController
public class BranchController extends BaseController {

    private static final String URI = "/branch/";

    @Autowired
    private BranchService service;

    /**
     * Danh sách chi nhánh
     */
    @GetMapping("/branch/list")
    public ResponseEntity<ApiResponse<Page<Branch>>> list(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            Pageable pageable,
            Principal principal
    ) {

        checkPermission(principal, URI, "VIEW");

        var result = service.getByCriteria(
                id,
                keyword,
                status,
                pageable
        );

        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Chi tiết chi nhánh
     */
    @GetMapping("/branch/get")
    public ResponseEntity<ApiResponse<Branch>> get(
            @RequestParam Integer id,
            Principal principal
    ) {

        checkPermission(principal, URI, "VIEW");

        return Mapper.map(
                service.getById(id),
                ApiResponse::okEntity
        );
    }

    /**
     * Thêm / cập nhật chi nhánh
     */
    @PostMapping("/branch/update")
    public ResponseEntity<ApiResponse<Branch>> update(
            @RequestBody Branch branch,
            Principal principal
    ) {

        String action = CommonUtils.NVL(branch.getId()) <= 0
                ? "ADD"
                : "UPDATE";

        checkPermission(principal, URI, action);

        Branch result = CommonUtils.NVL(branch.getId()) <= 0
                ? service.insert(branch)
                : service.update(branch.getId(), branch);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Cập nhật trạng thái chi nhánh
     */
    @PostMapping("/branch/update-status")
    public ResponseEntity<ApiResponse<Integer>> updateStatus(
            @RequestParam Integer id,
            @RequestParam Integer status,
            Principal principal
    ) {

        checkPermission(principal, URI, "UPDATE");

        return Mapper.map(
                service.updateStatus(id, status),
                ApiResponse::okEntity
        );
    }

    /**
     * Xóa chi nhánh
     */
    @DeleteMapping("/branch/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {

        checkPermission(principal, URI, "DELETE");

        return Mapper.map(
                service.delete(id),
                ApiResponse::okEntity
        );
    }

}