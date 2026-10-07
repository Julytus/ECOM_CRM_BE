package vn.aeoc.crm.controller.sale;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.constant.RagSyncConstant;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.other.RagService;
import vn.aeoc.crm.service.sale.VoucherService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.User;
import vn.aeoc.entity.data.mysql.Voucher;

import java.security.Principal;

@RestController
@AllArgsConstructor
public class VoucherController extends BaseController {

    private static final String URI = "/voucher/";
    private VoucherService voucherService;
    @Autowired
    private RagService ragService;

    @GetMapping("/voucher/list")
    public ResponseEntity<ApiResponse<Page<Voucher>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer branchId,
            Pageable pageable,
            Principal principal
    ) {
        User login = checkPermission(principal, URI, "VIEW");
        return Mapper.map(voucherService.getList(keyword, status, null, pageable), ApiResponse::okEntity);
    }

    @GetMapping("/voucher/get")
    public ResponseEntity<ApiResponse<Voucher>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(voucherService.getById(id), ApiResponse::okEntity);
    }

    @GetMapping("/voucher/getByCode")
    public ResponseEntity<ApiResponse<Voucher>> getByCode(
            @RequestParam String code,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        return Mapper.map(voucherService.getByCode(code), ApiResponse::okEntity);
    }

    @PostMapping("/voucher/update")
    public ResponseEntity<ApiResponse<Voucher>> upsert(
            @RequestBody Voucher v,
            Principal principal
    ) {
        String action = CommonUtils.NVL(v.getId()) <= 0 ? "ADD" : "UPDATE";
        checkPermission(principal, URI, action);

        Voucher result = CommonUtils.NVL(v.getId()) <= 0
                ? voucherService.insert(v)
                : voucherService.update(v.getId(), v);

        ragService.upsert(result.getId(), RagSyncConstant.VOUCHER);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/voucher/applyVoucher")
    public ResponseEntity<ApiResponse<Double>> applyVoucher(
            @RequestParam Integer invoiceId,
            @RequestParam String voucherCode,
            Principal principal
    ) {
        checkPermission(principal, URI, "UPDATE");

        Double result = voucherService.applyVoucher(invoiceId, voucherCode);

        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/voucher/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = voucherService.delete(id);
        if(result.equals(1))
            ragService.delete(id, RagSyncConstant.VOUCHER);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
