package vn.aeoc.crm.controller.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.auth.ResourceService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.auth.Resource;

import java.security.Principal;

@RestController
public class ResourceController extends BaseController {
    private static final String URI = "/resource/";

    @Autowired
    private ResourceService resourceService;

    /**
     * Danh sách Resource (tìm kiếm & phân trang)
     */
    @GetMapping("/resource/list")
    public ResponseEntity<ApiResponse<Page<Resource>>> getAllResources(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer active,
            Pageable pageable,
            Principal principal) {

        checkPermission(principal, URI, "VIEW");
        var result = resourceService.getListResourceByCriteria(keyword, active, pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Lấy chi tiết Resource
     */
    @GetMapping("/resource/getById")
    public ResponseEntity<ApiResponse<Resource>> getResourceDetail(
            @RequestParam Integer id,
            Principal principal) {

        checkPermission(principal, URI, "VIEW");
        var result = resourceService.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Thêm hoặc cập nhật Resource (Upsert)
     */
    @PostMapping("/resource/update")
    public ResponseEntity<ApiResponse<Resource>> upsertResource(
            @RequestBody Resource resource,
            Principal principal) {

        String action = (CommonUtils.NVL(resource.getId()) <= 0) ? "ADD" : "UPDATE";
        checkPermission(principal, URI, action);

        Resource result = (CommonUtils.NVL(resource.getId()) <= 0)
                ? resourceService.insert(resource)
                : resourceService.update(resource.getId(), resource);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Xóa Resource
     */
    @DeleteMapping("/resource/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> deleteResource(
            @PathVariable Integer id,
            Principal principal) {

        checkPermission(principal, URI, "DELETE");
        var result = resourceService.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Cập nhật trạng thái Resource (active = 0/1)
     */
    @PostMapping("/resource/update-status")
    public ResponseEntity<ApiResponse<Integer>> updateResourceStatus(
            @RequestParam Integer id,
            @RequestParam Integer active,
            Principal principal) {

        checkPermission(principal, URI, "UPDATE");

        var result = resourceService.updateResourceStatus(id, active);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
