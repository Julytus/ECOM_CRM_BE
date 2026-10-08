package vn.aeoc.crm.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.constant.StageConstant;
import vn.aeoc.crm.constant.StatusConstant;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.request.BulkAssignOpportunityRequest;
import vn.aeoc.crm.data.request.MoveOpportunityStageRequest;
import vn.aeoc.crm.data.response.OpportunityKanbanResponse;
import vn.aeoc.crm.data.response.StageTransitionMetaResponse;
import vn.aeoc.crm.service.other.NotificationService;
import vn.aeoc.crm.service.customer.OpportunityService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.Notification;
import vn.aeoc.entity.data.mysql.customer.Opportunity;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;
import java.util.List;

@RestController
public class OpportunityController extends BaseController {

    private static final String URI = "/opportunity/";

    @Autowired
    private OpportunityService service;

    @Autowired
    private NotificationService notificationService;

    /**
     * Danh sách cơ hội
     */
    @GetMapping("/opportunity/list")
    public ResponseEntity<ApiResponse<Page<Opportunity>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) List<Integer> priorities,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean unassignedOnly,
            Pageable pageable,
            Principal principal
    ) {

        User login = checkPermission(principal, URI, "VIEW");

        var result = service.getByCriteria(
                keyword,
                customerId,
                userId,
                stage,
                priorities,
                status,
                unassignedOnly,
                BranchFilter.listScopeBranchId(login),
                pageable
        );

        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Chi tiết cơ hội
     */
    @GetMapping("/opportunity/get")
    public ResponseEntity<ApiResponse<Opportunity>> get(
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
     * Thêm / cập nhật cơ hội
     */
    @PostMapping("/opportunity/update")
    public ResponseEntity<ApiResponse<Opportunity>> update(
            @RequestBody Opportunity opportunity,
            Principal principal
    ) {
        String action = CommonUtils.NVL(opportunity.getId()) <= 0
                ? "ADD"
                : "UPDATE";

        User login = checkPermission(principal, URI, action);

        Opportunity result;

        if (CommonUtils.NVL(opportunity.getId()) <= 0) {
            opportunity.setStage(StageConstant.LEAD);
            opportunity.setCreatorId(login.getId());
            opportunity.setBranchId(login.getBranchId());
            opportunity.setStatus(StatusConstant.ACTIVE);
            result = service.insert(opportunity);
        } else {
            result = service.update(opportunity.getId(), opportunity);
        }

        return Mapper.map(result, ApiResponse::okEntity);
    }

    /**
     * Xóa cơ hội
     */
    @DeleteMapping("/opportunity/delete/{id}")
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

    @PostMapping("/opportunity/update-stage")
    public ResponseEntity<ApiResponse<Integer>> updateStage(
            @RequestParam(name = "id") Integer id,
            @RequestParam(name = "stage") Integer stage,
            Principal principal
    ) {

        checkPermission(principal, URI, "UPDATE");

        return Mapper.map(
                service.updateStage(id, stage),
                ApiResponse::okEntity
        );
    }

    @PostMapping("/opportunity/lost")
    public ResponseEntity<ApiResponse<Integer>> lost(
            @RequestParam(name = "id") Integer id,
            @RequestParam(name = "reason") String reason,
            Principal principal
    ) {

        checkPermission(principal, URI, "UPDATE");

        return Mapper.map(
                service.markLost(id, reason),
                ApiResponse::okEntity
        );
    }

    @PostMapping("/opportunity/won")
    public ResponseEntity<ApiResponse<Integer>> won(
            @RequestParam(name = "id") Integer id,
            Principal principal
    ) {

        checkPermission(principal, URI, "UPDATE");

        return Mapper.map(
                service.markWon(id),
                ApiResponse::okEntity
        );
    }

    @GetMapping("/opportunity/kanban")
    public ResponseEntity<ApiResponse<OpportunityKanbanResponse>> kanban(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) List<Integer> stages,
            @RequestParam(required = false) List<Integer> priorities,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "12") Integer limit,
            Principal principal
    ) {

        User login = checkPermission(principal, URI, "VIEW");

        OpportunityKanbanResponse result = service.getKanban(
                keyword,
                customerId,
                userId,
                stage,
                stages,
                priorities,
                status,
                page,
                limit,
                BranchFilter.listScopeBranchId(login)
        );

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/opportunity/stage-transition-meta")
    public ResponseEntity<ApiResponse<StageTransitionMetaResponse>> stageTransitionMeta(
            Principal principal
    ) {

        checkPermission(principal, URI, "VIEW");

        return Mapper.map(
                service.getStageTransitionMeta(),
                ApiResponse::okEntity
        );
    }

    @PostMapping("/opportunity/move-stage/{id}")
    public ResponseEntity<ApiResponse<Integer>> moveStage(
            @PathVariable Integer id,
            @RequestBody MoveOpportunityStageRequest request,
            Principal principal
    ) {

        checkPermission(principal, URI, "UPDATE");

        return Mapper.map(
                service.moveStage(id, request),
                ApiResponse::okEntity
        );
    }

    @PostMapping("/opportunity/distribute")
    public ResponseEntity<ApiResponse<Integer>> distribute(
            @RequestBody BulkAssignOpportunityRequest request,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "UPDATE");
        Integer result = service.distributeOpportunities(
                request.getOpportunityIds(),
                request.getUserIds(),
                BranchFilter.listScopeBranchId(login)
        );
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/opportunity/assign/{id}")
    public ResponseEntity<ApiResponse<Integer>> assign(
            @PathVariable Integer id,
            @RequestParam Integer userId,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "UPDATE");
        Integer branchId = BranchFilter.listScopeBranchId(login);
        Integer result = service.assignOpportunity(id, userId, branchId);
        if (result != null && result > 0) {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setBranchId(branchId);
            notification.setType(1);
            notification.setTitle("Điều phối cơ hội");
            notification.setContent("Bạn được điều phối một cơ hội mới.");
            notification.setIsRead(0);
            notificationService.saveAndNotify(notification);
        }
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/opportunity/assign-auto/{id}")
    public ResponseEntity<ApiResponse<Integer>> assignAuto(
            @PathVariable Integer id,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "UPDATE");
        Integer branchId = BranchFilter.listScopeBranchId(login);
        Integer result = service.autoAssignOpportunity(id, branchId);
        if (result != null && result > 0) {
            Opportunity opp = service.getById(id);
            if (opp != null && opp.getUserId() != null) {
                Notification notification = new Notification();
                notification.setUserId(opp.getUserId());
                notification.setBranchId(branchId);
                notification.setType(1);
                notification.setTitle("Điều phối cơ hội");
                notification.setContent("Bạn được điều phối một cơ hội mới.");
                notification.setIsRead(0);
                notificationService.saveAndNotify(notification);
            }
        }
        return Mapper.map(result, ApiResponse::okEntity);
    }

}