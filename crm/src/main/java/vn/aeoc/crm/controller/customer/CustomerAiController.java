package vn.aeoc.crm.controller.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.customer.CustomerStrategyService;
import vn.aeoc.crm.util.Mapper;

import java.security.Principal;

@RestController
@RequestMapping("/customer/ai")
@RequiredArgsConstructor
public class CustomerAiController extends BaseController {

    private final CustomerStrategyService customerStrategyService;

    private static final String URI = "/campaignSnapshot/";

    /**
     * Lấy AI context (profile + rag)
     */
    @GetMapping("/{id}/context")
    public ResponseEntity<ApiResponse<Object>> getAiContext(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");

        var result = customerStrategyService.buildAiContext(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
