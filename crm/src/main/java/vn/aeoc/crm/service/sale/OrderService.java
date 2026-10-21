package vn.aeoc.crm.service.sale;

import vn.aeoc.base.util.QueryExecutor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.config.vnpay.VNPayConfig;
import vn.aeoc.crm.data.constant.PaymentMethod;
import vn.aeoc.crm.data.request.OrderRequest;
import vn.aeoc.crm.data.response.OrderResponse;
import vn.aeoc.crm.repository.sale.OrderItemRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.crm.data.constant.OrderStatus;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Payment;
import vn.aeoc.entity.data.mysql.sale.Order;
import vn.aeoc.entity.data.mysql.sale.OrderItem;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j(topic = "ORDER-SERVICE")
@Service
public class OrderService extends BaseService<OrderRepository, Order, Integer> {
    private final OrderItemRepository orderItemRepository;
    private final OrderItemService orderItemService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;

    public OrderService(OrderRepository repository, OrderItemRepository orderItemRepository, OrderItemService orderItemService,
                        InvoiceRepository invoiceRepository, PaymentRepository paymentRepository,
                        InvoiceService invoiceService, QueryExecutor queryExecutor) {
        super(repository, queryExecutor);
        this.orderItemRepository = orderItemRepository;
        this.orderItemService = orderItemService;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
    }

    public Page<Order> getByCriteria(
            String keyword,
            Integer userId,
            String orderStatus,
            String paymentStatus,
            String status,
            Pageable pageable
    ) {
        return pageQuery(
                pageable,
                () -> repository.getByCriteria(
                        keyword, userId, orderStatus, paymentStatus, status, pageable
                ),
                () -> repository.countByCriteria(
                        keyword, userId, orderStatus, paymentStatus, status
                )
        );
    }

    public OrderResponse getDetail(Integer id) {

        // 1. Order
        Order order = repository.getById(id);
        if (order == null) {
            throw new AppException(ErrorCode.DATA_NOT_FOUND);
        }

        List<OrderItem> items =
                orderItemRepository.fetchOrderItemDetail(id);

        // 3. Assemble response
        OrderResponse response = new OrderResponse(order, items);
        return response;
    }

    public Page<OrderResponse> getMyOrders(Integer userId, String orderStatus, Pageable pageable) {
        Long count = repository.countByUserId(userId, orderStatus);
        pageable.setTotal(count);
        return new Page<>(pageable, repository.findByUserId(userId, orderStatus, pageable));
    }

    public OrderResponse getMyOrder(Integer userId, Integer orderId) {
        Order order = repository.getById(orderId);
        if (order == null || !userId.equals(order.getUserId())) throw new AppException(ErrorCode.DATA_NOT_FOUND);
        Invoice invoice = invoiceRepository.findByOrderId(orderId);
        List<OrderItem> items = orderItemRepository.fetchOrderItemDetail(orderId);
        List<Payment> payments = invoice == null ? List.of() : paymentRepository.findAllByInvoiceId(invoice.getId());
        return new OrderResponse(order, items, invoice, payments);
    }

    @Transactional
    public void cancelMyOrder(Integer userId, Integer orderId) {
        Order order = repository.getById(orderId);
        if (order == null || !userId.equals(order.getUserId())) throw new AppException(ErrorCode.DATA_NOT_FOUND);
        Invoice invoice = invoiceRepository.findByOrderId(orderId);
        if (!OrderStatus.NEW.equals(order.getStatus()) || invoice == null
                || invoice.getStatus() != InvoiceConstant.STATUS_UNPAID) {
            throw new AppException("Chỉ có thể hủy đơn hàng mới chưa thanh toán", ErrorCode.BAD_REQUEST.code);
        }
        invoiceService.cancelOrderInvoice(invoice.getId());
    }

    @Transactional
    public void advance(Integer orderId, String to) {
        Order order = repository.getById(orderId);
        if (order == null) throw new AppException(ErrorCode.DATA_NOT_FOUND);
        Invoice invoice = invoiceRepository.findByOrderId(orderId);
        if (invoice == null) throw new AppException(ErrorCode.DATA_NOT_FOUND);
        if (invoice.getStatus() == InvoiceConstant.STATUS_CANCEL || invoice.getStatus() == InvoiceConstant.STATUS_REFUNDED) {
            throw new AppException("Không thể cập nhật đơn hàng đã hủy hoặc hoàn tiền", ErrorCode.BAD_REQUEST.code);
        }
        boolean valid = (OrderStatus.NEW.equals(order.getStatus()) && OrderStatus.CONFIRMED.equals(to))
                || (OrderStatus.CONFIRMED.equals(order.getStatus()) && OrderStatus.SHIPPING.equals(to))
                || (OrderStatus.SHIPPING.equals(order.getStatus()) && OrderStatus.COMPLETED.equals(to));
        if (!valid) throw new AppException("Trạng thái đơn hàng không hợp lệ", ErrorCode.BAD_REQUEST.code);
        if (OrderStatus.SHIPPING.equals(to) && "PAYOS".equalsIgnoreCase(invoice.getPaymentMethod())
                && invoice.getStatus() != InvoiceConstant.STATUS_DONE) {
            throw new AppException("Đơn PayOS phải thanh toán trước khi giao hàng", ErrorCode.BAD_REQUEST.code);
        }
        if (OrderStatus.COMPLETED.equals(to) && "COD".equalsIgnoreCase(invoice.getPaymentMethod())) {
            invoiceService.markPaid(invoice.getId(), "COD");
        }
        String paymentStatus = OrderStatus.COMPLETED.equals(to) && "COD".equalsIgnoreCase(invoice.getPaymentMethod())
                ? vn.aeoc.crm.data.constant.PaymentStatus.PAID : order.getPaymentStatus();
        repository.updateStatusAndPayment(orderId, to, paymentStatus);
    }

    public void updateStatus(Integer id, Order o) {
        Invoice invoice = invoiceRepository.findByOrderId(id);
        if (invoice != null && o.getPaymentStatus() != null) {
            throw new AppException("Không thể sửa trạng thái thanh toán của đơn checkout", ErrorCode.BAD_REQUEST.code);
        }
        if (invoice != null) throw new AppException("Vui lòng dùng API chuyển trạng thái đơn hàng", ErrorCode.BAD_REQUEST.code);
        repository.updateStatus(id, o);
    }

    @Override
    public Order update(Integer id, Order order) {
        Invoice invoice = invoiceRepository.findByOrderId(id);
        if (invoice != null && (order.getPaymentStatus() != null || order.getStatus() != null)) {
            throw new AppException("Không thể sửa trạng thái thanh toán hoặc đơn checkout trực tiếp", ErrorCode.BAD_REQUEST.code);
        }
        return super.update(id, order);
    }

    @Transactional
    public String create(OrderRequest req) {

        Order inserted = repository.createNewOrder(req);
        Integer orderId = inserted.getId();

        //dùng nếu muốn bảo mật hơn
        Double totalAmount = orderItemService.createMany(orderId, req.getItems());

        String returnUrl = req.getPaymentReturnUrl();

        switch (req.getPaymentMethod()) {
            case PaymentMethod.VNPAY:
                return createVnpayOrder(Long.valueOf(orderId), totalAmount, "https://exciting-earliest-educated-happened.trycloudflare.com");
            case PaymentMethod.COD:
                return "";
            default:
                throw new IllegalArgumentException(
                        "Unsupported payment type: " + req.getPaymentMethod()
                );
        }
    }

    public String createVnpayOrder(Long orderId, Double total, String urlReturn) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
        String vnp_IpAddr = "127.0.0.1";
        String vnp_TmnCode = VNPayConfig.vnp_TmnCode;
        String orderType = "order-type";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        long vnpAmount = Math.round(total * 100);
        vnp_Params.put("vnp_Amount", String.valueOf(vnpAmount));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", String.valueOf(orderId));
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = "vn";
        vnp_Params.put("vnp_Locale", locate);

        urlReturn += VNPayConfig.vnp_Returnurl;
        vnp_Params.put("vnp_ReturnUrl", urlReturn);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    //Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    log.error("Failed to URL-encode VNPay hash field: {}", fieldName, e);
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;
        return paymentUrl;
    }
}
