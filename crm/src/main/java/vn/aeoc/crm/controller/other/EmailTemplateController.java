package vn.aeoc.crm.controller.other;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.other.EmailTemplateService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.EmailTemplate;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/email-templates")
public class EmailTemplateController extends BaseController {

    private final EmailTemplateService service;
    private static final String URI = "/email-templates/";

    /* ==============================
            QUERY
       ============================== */

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmailTemplate>>> getByCriteria(
            Principal principal,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String name,
            Pageable pageable
    ) {
        User login = checkPermission(principal, URI, "VIEW");

        return Mapper.map(
                service.getByCriteria(id, name, BranchFilter.listScopeBranchId(login), pageable),
                ApiResponse::okEntity
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailTemplate>> getById(
            Principal principal,
            @PathVariable Integer id
    ) {
        checkPermission(principal, URI, "VIEW");

        return Mapper.map(
                service.getById(id),
                ApiResponse::okEntity
        );
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<EmailTemplate>>> getAll(
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");

        return Mapper.map(
                service.getAll(BranchFilter.listScopeBranchId(login)),
                ApiResponse::okEntity
        );
    }

    /* ==============================
            COMMAND
       ============================== */

    @PostMapping
    public ResponseEntity<ApiResponse<EmailTemplate>> create(
            Principal principal,
            @RequestBody EmailTemplate request
    ) {
        User login = checkPermission(principal, URI, "ADD");
        if (CommonUtils.NVL(request.getBranchId()) <= 0 && CommonUtils.NVL(login.getBranchId()) > 0) {
            request.setBranchId(login.getBranchId());
        }

        return Mapper.map(
                service.insert(request),
                ApiResponse::okEntity
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailTemplate>> update(
            Principal principal,
            @PathVariable Integer id,
            @RequestBody EmailTemplate request
    ) {
        checkPermission(principal, URI, "UPDATE");

        return Mapper.map(
                service.update(id, request),
                ApiResponse::okEntity
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            Principal principal,
            @PathVariable Integer id
    ) {
        checkPermission(principal, URI, "DELETE");

        return Mapper.map(
                service.delete(id),
                ApiResponse::okEntity
        );
    }
}