package vn.aeoc.crm.controller.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.segment.CustomerSegmentSnapshotRow;
import vn.aeoc.crm.data.segment.SegmentRecomputeProgress;
import vn.aeoc.crm.data.segment.SegmentSummaryItem;
import vn.aeoc.crm.data.segment.SegmentTrendItem;
import vn.aeoc.crm.service.segment.CustomerSegmentService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.User;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CustomerSegmentController extends BaseController {

    private static final String URI = "/customer/";

    private final CustomerSegmentService service;

    /*
     * List customer segment snapshot with optional segment filter.
     */
    @GetMapping("/segment/customer/list")
    public ResponseEntity<ApiResponse<Page<CustomerSegmentSnapshotRow>>> list(
            @RequestParam(required = false) String segmentCode,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        Integer branchId = BranchFilter.listScopeBranchId(login);
        return Mapper.map(service.getSnapshotPage(branchId, segmentCode, pageable), ApiResponse::okEntity);
    }

    /*
     * Get one customer segment snapshot.
     */
    @GetMapping("/segment/customer/get")
    public ResponseEntity<ApiResponse<CustomerSegmentSnapshotRow>> get(
            @RequestParam Integer customerId,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        Integer branchId = BranchFilter.listScopeBranchId(login);
        return Mapper.map(service.getSnapshot(branchId, customerId), ApiResponse::okEntity);
    }

    /*
     * Get summary counts for charting.
     */
    @GetMapping("/segment/summary")
    public ResponseEntity<ApiResponse<List<SegmentSummaryItem>>> summary(
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        Integer branchId = BranchFilter.listScopeBranchId(login);
        return Mapper.map(service.getSummary(branchId), ApiResponse::okEntity);
    }

    /*
     * Get monthly trend of customer segment movements from history table.
     */
    @GetMapping("/segment/trend")
    public ResponseEntity<ApiResponse<List<SegmentTrendItem>>> trend(
            @RequestParam(defaultValue = "6") Integer monthsBack,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        Integer branchId = BranchFilter.listScopeBranchId(login);
        return Mapper.map(service.getTrend(branchId, monthsBack), ApiResponse::okEntity);
    }

    /*
     * Trigger manual recompute for current branch.
     */
    @PostMapping("/segment/recompute")
    public ResponseEntity<ApiResponse<String>> recompute(
            Principal principal
    ) {
        User login = checkPermission(principal, "/segment/", "UPDATE");
        Integer branchId = BranchFilter.listScopeBranchId(login);
        return Mapper.map(service.recomputeAll(branchId, "MANUAL"), ApiResponse::okEntity);
    }

    /*
     * Get realtime progress/log snapshot for one recompute run.
     */
    @GetMapping("/segment/recompute/progress")
    public ResponseEntity<ApiResponse<SegmentRecomputeProgress>> recomputeProgress(
            @RequestParam String runId,
            Principal principal
    ) {
        checkPermission(principal, "/segment/", "VIEW");
        return Mapper.map(service.getRecomputeProgress(runId), ApiResponse::okEntity);
    }

    /*
     * Get latest recompute progress/log for current branch.
     */
    @GetMapping("/segment/recompute/latest")
    public ResponseEntity<ApiResponse<SegmentRecomputeProgress>> recomputeLatest(
            Principal principal
    ) {
        User login = checkPermission(principal, "/segment/", "VIEW");
        Integer branchId = BranchFilter.listScopeBranchId(login);
        return Mapper.map(service.getLatestRecomputeProgress(branchId), ApiResponse::okEntity);
    }
}
