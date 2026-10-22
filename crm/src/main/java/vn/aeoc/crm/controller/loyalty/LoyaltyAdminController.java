package vn.aeoc.crm.controller.loyalty;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.request.CustomerAccountLinkRequest;
import vn.aeoc.crm.data.request.LoyaltyAdjustRequest;
import vn.aeoc.crm.data.request.LoyaltyConfigRequest;
import vn.aeoc.crm.data.request.LoyaltyTierRequest;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.loyalty.LoyaltyAdminService;
import vn.aeoc.crm.util.SecurityUtils;
import vn.aeoc.entity.data.mysql.sale.LoyaltyAccount;
import vn.aeoc.entity.data.mysql.sale.LoyaltyConfig;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTier;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTransaction;

import java.security.Principal;
import java.util.List;

@RestController
public class LoyaltyAdminController extends BaseController {
    private static final String URI = "/loyalty/";
    private final LoyaltyAdminService service;

    public LoyaltyAdminController(LoyaltyAdminService service, UserService userService) {
        super(userService);
        this.service = service;
    }

    @GetMapping("/loyalty/tier/list")
    public ResponseEntity<ApiResponse<List<LoyaltyTier>>> tiers(Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return ResponseEntity.ok(ApiResponse.ok(service.listTiers()));
    }

    @PostMapping("/loyalty/tier/save")
    public ResponseEntity<ApiResponse<LoyaltyTier>> saveTier(@RequestBody LoyaltyTierRequest request, Principal principal) {
        checkPermission(principal, URI, request != null && request.id() != null ? "UPDATE" : "ADD");
        return ResponseEntity.ok(ApiResponse.ok(service.saveTier(request)));
    }

    @DeleteMapping("/loyalty/tier/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> deleteTier(@PathVariable Integer id, Principal principal) {
        checkPermission(principal, URI, "DELETE");
        return ResponseEntity.ok(ApiResponse.ok(service.deleteTier(id)));
    }

    @GetMapping("/loyalty/config")
    public ResponseEntity<ApiResponse<LoyaltyConfig>> config(Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return ResponseEntity.ok(ApiResponse.ok(service.getConfig()));
    }

    @PostMapping("/loyalty/config/update")
    public ResponseEntity<ApiResponse<LoyaltyConfig>> updateConfig(@RequestBody LoyaltyConfigRequest request, Principal principal) {
        checkPermission(principal, URI, "UPDATE");
        if (request == null) throw new AppException("earn_rate là bắt buộc", ErrorCode.BAD_REQUEST.code);
        return ResponseEntity.ok(ApiResponse.ok(service.updateConfig(request.earnRate())));
    }

    @PostMapping("/loyalty/adjust")
    public ResponseEntity<ApiResponse<Boolean>> adjust(@RequestBody LoyaltyAdjustRequest request, Principal principal) {
        checkPermission(principal, URI, "UPDATE");
        if (request == null || request.customerId() == null || request.points() == null || request.rankDelta() == null
                || request.reason() == null || request.reason().isBlank()) {
            throw new AppException("customerId, points, rankDelta và reason là bắt buộc", ErrorCode.BAD_REQUEST.code);
        }
        return ResponseEntity.ok(ApiResponse.ok(service.adjust(request.customerId(), request.points(),
                request.rankDelta(), request.reason(), SecurityUtils.userId(principal))));
    }

    @GetMapping("/loyalty/account")
    public ResponseEntity<ApiResponse<LoyaltyAccount>> account(@RequestParam Integer customerId, Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return ResponseEntity.ok(ApiResponse.ok(service.getAccount(customerId)));
    }

    @GetMapping("/loyalty/transactions")
    public ResponseEntity<ApiResponse<Page<LoyaltyTransaction>>> transactions(@RequestParam Integer customerId,
            @ParameterObject Pageable pageable, Principal principal) {
        checkPermission(principal, URI, "VIEW");
        return ResponseEntity.ok(ApiResponse.ok(service.getTransactions(customerId, pageable)));
    }

    @PostMapping("/customer/linkAccount")
    public ResponseEntity<ApiResponse<Integer>> linkAccount(@RequestBody CustomerAccountLinkRequest request, Principal principal) {
        checkPermission(principal, URI, "UPDATE");
        if (request == null) throw new AppException("customerId và accountUserId là bắt buộc", ErrorCode.BAD_REQUEST.code);
        return ResponseEntity.ok(ApiResponse.ok(service.linkAccount(request.customerId(), request.accountUserId())));
    }
}
