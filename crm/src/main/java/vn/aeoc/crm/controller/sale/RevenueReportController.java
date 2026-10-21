package vn.aeoc.crm.controller.sale;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.report.RevenueBucket;
import vn.aeoc.crm.data.report.RevenuePaymentMethod;
import vn.aeoc.crm.data.report.RevenueSummary;
import vn.aeoc.crm.data.report.TopProductStatistic;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.sale.RevenueReportService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
public class RevenueReportController extends BaseController {
    private static final String URI = "/report/";
    private final RevenueReportService service;

    public RevenueReportController(RevenueReportService service, UserService userService) {
        super(userService);
        this.service = service;
    }

    @GetMapping("/report/revenue/summary")
    public ResponseEntity<ApiResponse<RevenueSummary>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) {
        var login = checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getSummary(from, to, BranchFilter.listScopeBranchId(login)), ApiResponse::okEntity);
    }

    @GetMapping("/report/revenue/series")
    public ResponseEntity<ApiResponse<List<RevenueBucket>>> series(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String groupBy,
            Principal principal) {
        var login = checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getSeries(from, to, BranchFilter.listScopeBranchId(login), groupBy), ApiResponse::okEntity);
    }

    @GetMapping("/report/revenue/by-payment-method")
    public ResponseEntity<ApiResponse<List<RevenuePaymentMethod>>> byPaymentMethod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) {
        var login = checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getByPaymentMethod(from, to, BranchFilter.listScopeBranchId(login)), ApiResponse::okEntity);
    }

    @GetMapping("/report/revenue/top-products")
    public ResponseEntity<ApiResponse<List<TopProductStatistic>>> topProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") Integer limit,
            Principal principal) {
        var login = checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getTopProducts(from, to, BranchFilter.listScopeBranchId(login), limit), ApiResponse::okEntity);
    }
}
