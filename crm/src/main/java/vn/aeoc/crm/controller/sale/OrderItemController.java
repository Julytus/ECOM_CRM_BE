package vn.aeoc.crm.controller.sale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.service.sale.OrderItemService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.entity.data.mysql.sale.OrderItem;

import java.security.Principal;

@RestController
public class OrderItemController extends BaseController {
    private static final String URI = "/order-item/";

    @Autowired
    private OrderItemService orderItemService;

    @GetMapping("/order-item/list")
    public ResponseEntity<ApiResponse<Page<OrderItem>>> list(
            @RequestParam(required = false) Integer orderId,
            Pageable pageable,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = orderItemService.getByCriteria(orderId, pageable);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/order-item/get")
    public ResponseEntity<ApiResponse<OrderItem>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "VIEW");
        var result = orderItemService.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/order-item/update")
    public ResponseEntity<ApiResponse<OrderItem>> update(
            @RequestBody OrderItem request,
            Principal principal
    ) {
        String action = CommonUtils.NVL(request.getId()) <= 0 ? "ADD" : "UPDATE";
        checkPermission(principal, URI, action);

        OrderItem result = CommonUtils.NVL(request.getId()) <= 0
                ? orderItemService.insert(request)
                : orderItemService.update(request.getId(), request);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @DeleteMapping("/order-item/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
        checkPermission(principal, URI, "DELETE");
        var result = orderItemService.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }
}