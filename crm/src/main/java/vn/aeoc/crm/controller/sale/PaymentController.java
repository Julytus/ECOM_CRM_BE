package vn.aeoc.crm.controller.sale;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.service.sale.PaymentService;
import vn.aeoc.crm.util.SecurityUtils;
import vn.aeoc.crm.data.response.PaymentStatusResponse;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.sale.Payment;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@AllArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Payment>> createPayment(
            @RequestParam Integer invoiceId
    ) {
        Payment result = paymentService.createPayment(invoiceId);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String requestBody
    ) {
        String result = paymentService.handleWebhook(requestBody);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> status(
            @RequestParam String orderCode, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.syncFromPayos(orderCode, SecurityUtils.userId(principal))));
    }

    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<Map<String, String>>> retry(
            @RequestParam Integer invoiceId, Principal principal) {
        PaymentService.PayosLink link = paymentService.retryPayment(invoiceId, SecurityUtils.userId(principal));
        return ResponseEntity.ok(ApiResponse.ok(Map.of("checkoutUrl", link.checkoutUrl(),
                "qrCode", link.qrCode() == null ? "" : link.qrCode())));
    }
}
