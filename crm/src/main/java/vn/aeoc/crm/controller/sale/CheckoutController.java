package vn.aeoc.crm.controller.sale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.data.request.CheckoutRequest;
import vn.aeoc.crm.data.response.CheckoutPreviewResponse;
import vn.aeoc.crm.data.response.CheckoutResponse;
import vn.aeoc.crm.service.sale.CheckoutService;
import vn.aeoc.crm.util.SecurityUtils;

import java.security.Principal;

@RestController
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/preview")
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> preview(@RequestBody CheckoutRequest request, Principal principal) {
        Integer userId = SecurityUtils.userId(principal);
        return ResponseEntity.ok(ApiResponse.ok(checkoutService.preview(request, userId)));
    }

    @PostMapping("/checkout/submit")
    public ResponseEntity<ApiResponse<CheckoutResponse>> submit(@RequestBody CheckoutRequest request, Principal principal) {
        Integer userId = SecurityUtils.userId(principal);
        return ResponseEntity.ok(ApiResponse.ok(checkoutService.submit(request, userId)));
    }
}
