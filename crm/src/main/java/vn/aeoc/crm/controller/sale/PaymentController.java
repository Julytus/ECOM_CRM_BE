package vn.aeoc.crm.controller.sale;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.service.sale.PaymentService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.Payment;

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
}