package vn.aeoc.crm.controller.loyalty;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.data.request.RedeemRequest;
import vn.aeoc.crm.data.response.LoyaltyAccountResponse;
import vn.aeoc.crm.data.response.MyVoucherResponse;
import vn.aeoc.crm.data.response.RedeemableVoucherResponse;
import vn.aeoc.crm.repository.customer.CustomerRepository;
import vn.aeoc.crm.service.loyalty.LoyaltyQueryService;
import vn.aeoc.crm.service.loyalty.LoyaltyRedeemService;
import vn.aeoc.crm.util.SecurityUtils;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTransaction;
import vn.aeoc.base.config.extention.paging.Page;

import java.security.Principal;
import java.util.List;

@RestController
public class LoyaltyController {
    private final CustomerRepository customers;
    private final LoyaltyQueryService queries;
    private final LoyaltyRedeemService redeemer;

    public LoyaltyController(CustomerRepository customers, LoyaltyQueryService queries, LoyaltyRedeemService redeemer) {
        this.customers = customers;
        this.queries = queries;
        this.redeemer = redeemer;
    }

    @GetMapping("/loyalty/my")
    public ResponseEntity<ApiResponse<LoyaltyAccountResponse>> my(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(queries.getMyAccount(customerId(principal))));
    }

    @GetMapping("/loyalty/my/transactions")
    public ResponseEntity<ApiResponse<Page<LoyaltyTransaction>>> transactions(
            @ParameterObject Pageable pageable, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(queries.getMyTransactions(customerId(principal), pageable)));
    }

    @GetMapping("/loyalty/redeemable")
    public ResponseEntity<ApiResponse<List<RedeemableVoucherResponse>>> redeemable(Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(queries.listRedeemable(customerId(principal))));
    }

    @PostMapping("/loyalty/redeem")
    public ResponseEntity<ApiResponse<Integer>> redeem(@RequestBody RedeemRequest request, Principal principal) {
        if (request == null || request.voucherId() == null) throw new AppException("voucherId là bắt buộc", ErrorCode.BAD_REQUEST.code);
        return ResponseEntity.ok(ApiResponse.ok(redeemer.redeem(customerId(principal), request.voucherId())));
    }

    @GetMapping("/loyalty/my/vouchers")
    public ResponseEntity<ApiResponse<Page<MyVoucherResponse>>> myVouchers(
            @RequestParam(required = false) String status,
            @ParameterObject Pageable pageable, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(queries.getMyVouchers(customerId(principal), status, pageable)));
    }

    private Integer customerId(Principal principal) {
        Integer userId = SecurityUtils.userId(principal);
        Integer customerId = userId > 0 ? customers.findCustomerIdByAccountUserId(userId) : null;
        if (customerId == null || customerId <= 0) {
            throw new AppException("Tài khoản chưa được liên kết với khách hàng", ErrorCode.BAD_REQUEST.code);
        }
        return customerId;
    }
}
