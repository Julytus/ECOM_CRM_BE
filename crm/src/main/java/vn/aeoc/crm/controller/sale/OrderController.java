package vn.aeoc.crm.controller.sale;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.config.model.ApiResponse;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.controller.BaseController;
import vn.aeoc.crm.data.constant.OrderStatus;
import vn.aeoc.crm.data.constant.PaymentStatus;
import vn.aeoc.crm.data.request.OrderRequest;
import vn.aeoc.crm.data.response.OrderResponse;
import vn.aeoc.crm.service.auth.UserService;
import vn.aeoc.crm.service.sale.OrderService;
import vn.aeoc.crm.util.Mapper;
import vn.aeoc.crm.util.SecurityUtils;
import vn.aeoc.entity.data.mysql.sale.Order;

import java.security.Principal;

@RestController
public class OrderController extends BaseController {
    private static final String URI = "/order/";

    private final OrderService orderService;

    public OrderController(OrderService orderService, UserService userService) {
        super(userService);
        this.orderService = orderService;
    }

    @GetMapping("/order/list")
    public ResponseEntity<ApiResponse<Page<Order>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String status,
            Pageable pageable,
            Principal principal
    ) {
//        checkPermission(principal, URI, "VIEW");
        var result = orderService.getByCriteria(
                keyword, userId, orderStatus, paymentStatus, status, pageable
        );
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/order/get")
    public ResponseEntity<ApiResponse<Order>> get(
            @RequestParam Integer id,
            Principal principal
    ) {
//        checkPermission(principal, URI, "VIEW");
        var result = orderService.getById(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/order/create")
    public ResponseEntity<ApiResponse<String>> create(
            Principal principal,
            @RequestBody OrderRequest o) {
        Integer loginId = SecurityUtils.userId(principal);
        o.setUserId(loginId);
        var result = orderService.create(o);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/order/update")
    public ResponseEntity<ApiResponse<Order>> update(
            @RequestBody Order request,
            Principal principal
    ) {
        String action = CommonUtils.NVL(request.getId()) <= 0 ? "ADD" : "UPDATE";
//        checkPermission(principal, URI, action);

        Order result = CommonUtils.NVL(request.getId()) <= 0
                ? orderService.insert(request)
                : orderService.update(request.getId(), request);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @GetMapping("/order/detail/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getDetail(
            @PathVariable(name = "id") Integer id) {

        var result = orderService.getDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/order/delete/{id}")
    public ResponseEntity<ApiResponse<Integer>> delete(
            @PathVariable Integer id,
            Principal principal
    ) {
//        checkPermission(principal, URI, "DELETE");
        var result = orderService.delete(id);
        return Mapper.map(result, ApiResponse::okEntity);
    }

    @PostMapping("/order/update-status")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            Principal principal,
            @RequestBody OrderRequest o) {

        if (!OrderStatus.isValid(o.getStatus())) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        if (!PaymentStatus.isValid(o.getPaymentStatus())) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        orderService.updateStatus(o.getId(), o);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái đơn đặt hàng thành công"));
    }

    @GetMapping("/order/my")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> myOrders(
            @RequestParam(required = false) String orderStatus, Pageable pageable, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getMyOrders(SecurityUtils.userId(principal), orderStatus, pageable)));
    }

    @GetMapping("/order/my/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> myOrder(@PathVariable Integer id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getMyOrder(SecurityUtils.userId(principal), id)));
    }

    @PostMapping("/order/my/{id}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelMyOrder(@PathVariable Integer id, Principal principal) {
        orderService.cancelMyOrder(SecurityUtils.userId(principal), id);
        return ResponseEntity.ok(ApiResponse.ok("Hủy đơn hàng thành công"));
    }

    @PostMapping("/order/{id}/advance")
    public ResponseEntity<ApiResponse<String>> advance(@PathVariable Integer id, @RequestParam String to,
                                                       Principal principal) {
        checkPermission(principal, URI, "UPDATE");
        orderService.advance(id, to);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái đơn đặt hàng thành công"));
    }
}
