package vn.aeoc.crm.controller.sale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.report.RevenueHourChart;
import vn.aeoc.crm.data.report.RevenueToday;
import vn.aeoc.crm.data.response.InvoiceResponse;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.sale.InvoiceService;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.auth.User;

import java.security.Principal;
import java.util.List;

@RestController
public class InvoiceController extends BaseController {

    private static final String URI = "/invoice/";

    private final InvoiceService service;

    public InvoiceController(InvoiceService service, UserService userService) {
        super(userService);
        this.service = service;
    }

    @GetMapping("/invoice/list")
    public ResponseEntity<ApiResponse<Page<Invoice>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) List<Integer> status,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = service.getListByCriteria(
                keyword, customerId, status, BranchFilter.listScopeBranchId(login), pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/invoice/get")
    public ResponseEntity<ApiResponse<Invoice>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = service.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/invoice/getDetail")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getDetail(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = service.getDetailById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    //Lấy tổng doanh thu trong ngày hôm nay, dùng cho báo cáo doanh thu hôm nay
    @GetMapping("/invoice/getRevenueToday")
    public ResponseEntity<ApiResponse<RevenueToday>> getRevenueToday(
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        var result = service.getRevenueToday(BranchFilter.listScopeBranchId(login));
        return Mapper.map(result, ApiResponse::okEntity);
    }

    //Biểu đồ doanh thu theo giờ trong ngày hôm nay, dùng cho báo cáo doanh thu hôm nay
    @GetMapping("/invoice/getRevenueByHourToday")
    public ResponseEntity<ApiResponse<List<RevenueHourChart>>> getRevenueByHourToday(
            Principal principal
    ) {
        User login = checkPermission(principal, "/report/", "VIEW");
        var result = service.getRevenueByHourToday(BranchFilter.listScopeBranchId(login));
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/invoice/updateStatus")
    public ResponseEntity<ApiResponse<Integer>> upsert(
            @RequestParam Integer invoiceId,
            @RequestParam Integer status,
            Principal principal
    ) {
        String action = "UPDATE";
        checkPermission(principal, URI, action);

        Integer result = service.updateStatus(invoiceId, status);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    //Tạo hóa đơn thanh toán thành công từ hóa đơn nháp,
    //đồng thời cập nhật trạng thái hóa đơn nháp thành đã thanh toán
    /*
    Input:
     invoice: thông tin hóa đơn
     voucherId
     */
    @PostMapping("/invoice/create")
    public ResponseEntity<ApiResponse<Invoice>> create(
            @RequestBody Invoice invoice,
            Principal principal
    ) {
        String action = "UPDATE";
        checkPermission(principal, URI, action);

        if (invoice.getPaymentType() == InvoiceConstant.PAYMENT_CASH) invoice.setStatus(InvoiceConstant.STATUS_DONE);
        if (invoice.getPaymentType() == InvoiceConstant.PAYMENT_TRANSFER) invoice.setStatus(InvoiceConstant.STATUS_UNPAID);
        if (invoice.getPaymentType() == InvoiceConstant.PAYMENT_TRANSFER && invoice.getFee() == 0)
            invoice.setStatus(InvoiceConstant.STATUS_DONE);
        return Mapper.map(service.create(invoice), ApiResponse::okEntity);
    }

    @PostMapping("/invoice/cancel/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = service.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/invoice/draft/create")
    public ResponseEntity<ApiResponse<Invoice>> createDraft(
            @RequestParam(required = false) Integer customerId,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "ADD");

        Invoice result = service.createDraftInvoice(
                customerId,
                login.getId(),
                login.getBranchId()
        );

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/invoice/draft/update")
    public ResponseEntity<ApiResponse<Invoice>> createDraft(
            @RequestBody Invoice invoice,
            Principal principal
    ) {
        Invoice result = service.updateInvoice(invoice);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/invoice/draft/delete")
    public ResponseEntity<ApiResponse<Integer>> deleteDraft(
            @RequestParam Integer invoiceId,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");

        Integer result = service.delete(invoiceId);
        return Mapper.map(result, ApiResponse::okEntity);
    }


    //Tính toán lại hóa đơn nháp, dùng khi thêm/sửa/xóa sản phẩm trên hóa đơn nháp
    @PostMapping("/invoice/recalculate")
    public ResponseEntity<ApiResponse<Invoice>> recalculate(
            @RequestParam Integer invoiceId,
            @RequestParam(required = false) Long version,
            Principal principal
    ) {
        checkPermission(principal, URI, "UPDATE");

        Invoice result = service.recalculateDraftAmount(invoiceId, version);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
