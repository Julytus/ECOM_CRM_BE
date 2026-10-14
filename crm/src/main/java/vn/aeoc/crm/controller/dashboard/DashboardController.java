package vn.aeoc.crm.controller.dashboard;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.report.*;
import vn.aeoc.crm.service.dashboard.DashboardService;
import vn.aeoc.crm.util.Mapper;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
public class DashboardController extends BaseController {
    private static final String URI = "/dashboard/";
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard/revenue")
    public ResponseEntity<ApiResponse<List<RevenueStatistic>>> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "DAY") String groupBy,
            Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getRevenue(fromDate, toDate, groupBy), ApiResponse::okEntity);
    }

    @GetMapping("/dashboard/order-status")
    public ResponseEntity<ApiResponse<List<OrderStatusStatistic>>> orderStatus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getOrderStatus(fromDate, toDate), ApiResponse::okEntity);
    }

    @GetMapping("/dashboard/top-products")
    public ResponseEntity<ApiResponse<List<TopProductStatistic>>> topProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "QUANTITY") String sortBy,
            Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getTopProducts(fromDate, toDate, limit, sortBy), ApiResponse::okEntity);
    }

    @GetMapping("/dashboard/aov")
    public ResponseEntity<ApiResponse<List<AovStatistic>>> aov(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "DAY") String groupBy,
            Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getAov(fromDate, toDate, groupBy), ApiResponse::okEntity);
    }

    @GetMapping("/dashboard/inventory")
    public ResponseEntity<ApiResponse<InventoryStatistic>> inventory(
            @RequestParam(defaultValue = "5") Integer threshold,
            @RequestParam(defaultValue = "20") Integer limit,
            Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(service.getInventory(threshold, limit), ApiResponse::okEntity);
    }
}