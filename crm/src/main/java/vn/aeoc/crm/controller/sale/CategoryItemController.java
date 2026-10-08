package vn.aeoc.crm.controller.sale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.sale.CategoryItemService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.sale.CategoryItem;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;
import java.util.List;

@RestController
public class CategoryItemController extends BaseController {

    private static final String URI = "/categoryItem/";

    @Autowired
    private CategoryItemService service;
/**
 * Level: 0 - lấy cha, 1 - lấy con, 2 - lấy tất cả
 *
 */
    @GetMapping("/categoryItem/list")
    public ResponseEntity<ApiResponse<Page<CategoryItem>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "2") Integer level,
            @RequestParam(required = false, defaultValue = "-1") Integer active,
            @RequestParam(required = false) Integer type,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = service.getListCategoryItem(
                keyword, type, active, level, BranchFilter.listScopeBranchId(login), pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/categoryItem/child")
    public ResponseEntity<ApiResponse<List<CategoryItem>>> child(
            @RequestParam Integer parentId,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = service.getChild(parentId, BranchFilter.listScopeBranchId(login));
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/categoryItem/get")
    public ResponseEntity<ApiResponse<CategoryItem>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = service.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/categoryItem/update")
    public ResponseEntity<ApiResponse<CategoryItem>> update(
            @RequestBody CategoryItem item,
            Principal principal
    ) {
        String action = (CommonUtils.NVL(item.getId()) <= 0) ? "ADD" : "UPDATE";
        User login = checkPermission(principal, URI, action);
        if (CommonUtils.NVL(item.getBranchId()) <= 0 && CommonUtils.NVL(login.getBranchId()) > 0) {
            item.setBranchId(login.getBranchId());
        }

        var result = CommonUtils.NVL(item.getId()) <= 0
                ? service.insert(item)
                : service.update(item.getId(), item);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/categoryItem/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = service.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/categoryItem/update-status")
    public ResponseEntity<ApiResponse<Integer>> updateStatus(
            @RequestParam Integer id,
            @RequestParam Integer active,
            Principal principal
    ) {
        checkPermission(principal, URI, "UPDATE");
        var result = service.updateStatus(id, active);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
