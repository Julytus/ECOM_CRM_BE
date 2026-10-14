package vn.aeoc.crm.controller.other;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.report.*;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.sale.ReportService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/report")
public class ReportController extends BaseController {

    private static final String URI = "/report/";

    private final ReportService service;

    public ReportController(ReportService service, UserService userService) {
        super(userService);
        this.service = service;
    }

    /**
     * Biểu đồ cột: khách hàng theo tháng
     */
    @GetMapping("/customer/by-month")
    public ResponseEntity<ApiResponse<List<CustomerMonthlyReport>>> reportByMonth(
            @RequestParam(name = "year") Integer year,
            Principal principal) {

        User login = checkPermission(principal, URI, "VIEW");
        return ResponseEntity.ok(ApiResponse.ok(service.getCustomerByMonth(year, BranchFilter.listScopeBranchId(login))));
    }

    /**
     * Biểu đồ tròn: khách hàng theo nguồn
     */
    @GetMapping("/customer/by-source")
    public ResponseEntity<ApiResponse<List<CustomerSourceReport>>> reportBySource(
            @RequestParam(name = "year") Integer year,
            Principal principal) {

        User login = checkPermission(principal, URI, "VIEW");
        return ResponseEntity.ok(ApiResponse.ok(service.getCustomerBySource(year, BranchFilter.listScopeBranchId(login))));
    }

    /**
     * Tổng doanh thu trong tháng
     */
    @GetMapping("/invoice/monthly-revenue")
    public ResponseEntity<ApiResponse<InvoiceMonthlyRevenueReport>> monthlyRevenue(
            @RequestParam(name = "year") Integer year,
            @RequestParam(name = "month") Integer month,
            Principal principal) {

        User login = checkPermission(principal, URI, "VIEW");
        return ResponseEntity.ok(ApiResponse.ok(
                service.getMonthlyRevenue(year, month, BranchFilter.listScopeBranchId(login))
        ));
    }

    /**
     * Tần suất mua bán trong tháng
     */
    @GetMapping("/invoice/frequency")
    public ResponseEntity<ApiResponse<Page<InvoiceFrequencyReport>>> frequency(
            @RequestParam(name = "year") Integer year,
            @RequestParam(name = "month") Integer month,
            Principal principal,
            Pageable pageable) {

        User login = checkPermission(principal, URI, "VIEW");
        return ResponseEntity.ok(ApiResponse.ok(
                service.getInvoiceFrequency(year, month, BranchFilter.listScopeBranchId(login), pageable)
        ));
    }

    @GetMapping("/callHistory/avg-interest")
    public ResponseEntity<ApiResponse<Page<CustomerInterestAvgReport>>> avgInterest(
            @RequestParam(name = "keyword", required = false) String keyword,
            Pageable pageable,
            Principal principal) {

        User login = checkPermission(principal, URI, "VIEW");
        var result = service
                .getAvgInterestByCustomer(keyword, BranchFilter.listScopeBranchId(login), pageable);

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/callHistory/interest-bar")
    public ResponseEntity<ApiResponse<List<CustomerInterestMonthlyBarReport>>> interestBar(
            @RequestParam(name = "year") Integer year,
            @RequestParam(name = "minAvgInterest") Double minAvgInterest,
            Principal principal) {

        User login = checkPermission(principal, URI, "VIEW");

        return ResponseEntity.ok(ApiResponse.ok(
                service
                        .getMonthlyInterestBar(year, minAvgInterest, BranchFilter.listScopeBranchId(login))
        ));
    }
}
