package vn.aeoc.crm.controller.auth;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.controller.auth.dto.AssignResourcesRequest;
import vn.aeoc.crm.controller.auth.dto.ResourceIdsRequest;
import vn.aeoc.crm.service.auth.ModuleService;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.auth.Module;
import vn.aeoc.entity.data.mysql.auth.Resource;

import java.security.Principal;
import java.util.List;

@RestController
public class ModuleController extends BaseController {
    private static final String URI = "/module/";
    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService, UserService userService) {
        super(userService);
        this.moduleService = moduleService;
    }

    @GetMapping("/module/list")
    public ResponseEntity<ApiResponse<Page<Module>>> getAllModules(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer active,
            @ParameterObject Pageable pageable,
            Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(moduleService.getListModuleByCriteria(keyword, active, pageable), ApiResponse::okEntity);
    }

    @GetMapping("/module/getById")
    public ResponseEntity<ApiResponse<Module>> getModuleById(@RequestParam Integer id, Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(moduleService.getById(id), ApiResponse::okEntity);
    }

    @PostMapping("/module/update")
    public ResponseEntity<ApiResponse<Module>> upsertModule(@RequestBody Module module, Principal principal) {
        String action = CommonUtils.NVL(module.getId()) <= 0 ? "ADD" : "UPDATE";
        checkPermission(principal, URI, action);
        return Mapper.map(moduleService.upsert(module), ApiResponse::okEntity);
    }

    @DeleteMapping("/module/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> deleteModule(@PathVariable Integer id, Principal principal) {
        checkPermission(principal, URI, "DELETE");
        return Mapper.map(moduleService.deleteModule(id), ApiResponse::okEntity);
    }

    @PostMapping("/module/update-status")
    public ResponseEntity<ApiResponse<Integer>> updateModuleStatus(@RequestParam Integer id,
                                                                    @RequestParam Integer active,
                                                                    Principal principal) {
        checkPermission(principal, URI, "UPDATE");
        return Mapper.map(moduleService.updateModuleStatus(id, active), ApiResponse::okEntity);
    }

    @PostMapping("/module/assign-resources")
    public ResponseEntity<ApiResponse<Integer>> assignResources(@RequestBody AssignResourcesRequest request,
                                                                 Principal principal) {
        checkPermission(principal, URI, "UPDATE");
        return Mapper.map(moduleService.assignResources(request.getModuleId(), request.getResourceIds()),
                ApiResponse::okEntity);
    }

    @PostMapping("/module/unassign-resources")
    public ResponseEntity<ApiResponse<Integer>> unassignResources(@RequestBody ResourceIdsRequest request,
                                                                   Principal principal) {
        checkPermission(principal, URI, "UPDATE");
        return Mapper.map(moduleService.unassignResources(request.getResourceIds()), ApiResponse::okEntity);
    }

    @GetMapping("/module/resources")
    public ResponseEntity<ApiResponse<List<Resource>>> getModuleResources(@RequestParam Integer moduleId,
                                                                           Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(moduleService.getResources(moduleId), ApiResponse::okEntity);
    }
}
