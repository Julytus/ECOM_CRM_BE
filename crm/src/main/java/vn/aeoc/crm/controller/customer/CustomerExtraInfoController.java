package vn.aeoc.crm.controller.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.customer.CustomerExtraInfoService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.CustomerExtraInfo;

import java.security.Principal;
import java.util.List;

@RestController
public class CustomerExtraInfoController extends BaseController {

    private static final String URI = "/customerAttribute/";

    @Autowired
    private CustomerExtraInfoService service;

    /**
     * Lấy danh sách thông tin bổ sung của 1 khách hàng
     */
    @GetMapping("/customerExtraInfo/list")
    public ResponseEntity<ApiResponse<List<CustomerExtraInfo>>> listCustomerExtraInfo(
            @RequestParam(defaultValue = "0") Integer customerId,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = service.getCustomerExtraInfoByCustomerId(customerId);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}
