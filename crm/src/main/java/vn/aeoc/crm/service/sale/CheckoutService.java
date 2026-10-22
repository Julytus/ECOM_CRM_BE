package vn.aeoc.crm.service.sale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.config.checkout.CheckoutConfig;
import vn.aeoc.crm.config.payos.PayOSConfig;
import vn.aeoc.crm.data.constant.PaymentMethod;
import vn.aeoc.crm.data.request.CheckoutRequest;
import vn.aeoc.crm.data.response.CheckoutPreviewResponse;
import vn.aeoc.crm.data.response.CheckoutResponse;
import vn.aeoc.crm.repository.sale.CustomerVoucherUsageRepository;
import vn.aeoc.crm.repository.sale.CustomerVoucherRepository;
import vn.aeoc.crm.repository.customer.CustomerRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.InvoiceVoucherRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Order;
import vn.aeoc.entity.data.mysql.sale.Payment;
import vn.aeoc.entity.data.mysql.sale.Voucher;

import java.util.concurrent.CompletableFuture;

@Service
public class CheckoutService {
    private final CheckoutConfig checkoutConfig;
    private final PayOSConfig payOSConfig;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceVoucherRepository invoiceVoucherRepository;
    private final CustomerVoucherUsageRepository customerVoucherUsageRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemService orderItemService;
    private final VoucherService voucherService;
    private final PaymentService paymentService;
    private final QueryExecutor queryExecutor;

    public CheckoutService(CheckoutConfig checkoutConfig, PayOSConfig payOSConfig,
                           OrderRepository orderRepository, InvoiceRepository invoiceRepository,
                           PaymentRepository paymentRepository, InvoiceVoucherRepository invoiceVoucherRepository,
                           CustomerVoucherUsageRepository customerVoucherUsageRepository,
                           CustomerVoucherRepository customerVoucherRepository, CustomerRepository customerRepository,
                           OrderItemService orderItemService, VoucherService voucherService,
                           PaymentService paymentService, QueryExecutor queryExecutor) {
        this.checkoutConfig = checkoutConfig;
        this.payOSConfig = payOSConfig;
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceVoucherRepository = invoiceVoucherRepository;
        this.customerVoucherUsageRepository = customerVoucherUsageRepository;
        this.customerVoucherRepository = customerVoucherRepository;
        this.customerRepository = customerRepository;
        this.orderItemService = orderItemService;
        this.voucherService = voucherService;
        this.paymentService = paymentService;
        this.queryExecutor = queryExecutor;
    }

    public CheckoutPreviewResponse preview(CheckoutRequest request) {
        return preview(request, null);
    }

    public CheckoutPreviewResponse preview(CheckoutRequest request, Integer userId) {
        request.validate();
        OrderItemService.CalculatedItems items = orderItemService.calculate(request.getItems());
        int branchId = branchId(request);
        Integer customerId = resolveCustomerId(request, userId);
        double regularDiscount = request.getVoucherCode() == null || request.getVoucherCode().isBlank()
                ? 0D
                : voucherService.evaluate(request.getVoucherCode(), items.subtotal(), branchId, customerId).discount();
        double loyaltyDiscount = 0D;
        if (request.getCustomerVoucherId() != null) {
            if (customerId == null) throw unavailableCustomerVoucher();
            Integer customerVoucherId = Math.toIntExact(request.getCustomerVoucherId());
            var customerVoucher = customerVoucherRepository.findAvailable(customerVoucherId, customerId);
            if (!isCustomerVoucherAvailable(customerVoucher)) throw unavailableCustomerVoucher();
            loyaltyDiscount = voucherService.evaluateRedeemed(customerVoucher.getVoucherId(),
                    items.subtotal() - regularDiscount, branchId).discount();
        }
        double discount = regularDiscount + loyaltyDiscount;
        double shippingFee = checkoutConfig.shippingFee;
        return new CheckoutPreviewResponse(items.subtotal(), discount, shippingFee,
                items.subtotal() - discount + shippingFee,
                request.getVoucherCode() == null || request.getVoucherCode().isBlank() ? null : request.getVoucherCode(),
                loyaltyDiscount, request.getCustomerVoucherId());
    }

    @Transactional
    public CheckoutResponse submit(CheckoutRequest request, Integer userId) {
        request.validate();
        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        OrderItemService.CalculatedItems items = orderItemService.calculate(request.getItems());
        int branchId = branchId(request);
        Integer customerId = resolveCustomerId(request, userId);
        Voucher voucher = null;
        double regularDiscount = 0D;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucher = voucherService.lockByCode(request.getVoucherCode());
            VoucherService.VoucherEvaluation evaluation = voucherService.evaluateLocked(
                    voucher, items.subtotal(), branchId, customerId);
            voucher = evaluation.voucher();
            regularDiscount = evaluation.discount();
        }

        Integer customerVoucherId = null;
        Integer loyaltyVoucherId = null;
        double loyaltyDiscount = 0D;
        if (request.getCustomerVoucherId() != null) {
            if (customerId == null) throw unavailableCustomerVoucher();
            customerVoucherId = Math.toIntExact(request.getCustomerVoucherId());
            var customerVoucher = customerVoucherRepository.findAvailable(customerVoucherId, customerId);
            if (!isCustomerVoucherAvailable(customerVoucher)) throw unavailableCustomerVoucher();
            VoucherService.VoucherEvaluation evaluation = voucherService.evaluateRedeemed(
                    customerVoucher.getVoucherId(), items.subtotal() - regularDiscount, branchId);
            loyaltyVoucherId = evaluation.voucher().getId();
            loyaltyDiscount = evaluation.discount();
            if (customerVoucherRepository.markUsedIfAvailable(customerVoucherId, customerId, null) != 1) {
                throw unavailableCustomerVoucher();
            }
        }
        double discount = regularDiscount + loyaltyDiscount;

        double shippingFee = checkoutConfig.shippingFee;
        double finalAmount = items.subtotal() - discount + shippingFee;
        Order order = new Order();
        order.setUserId(userId);
        order.setCustomerId(customerId);
        order.setBranchId(branchId);
        order.setBuyerName(request.getBuyerName());
        order.setBuyerPhone(request.getBuyerPhone());
        order.setBuyerEmail(request.getBuyerEmail());
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setNote(request.getNote());
        order.setPaymentMethod(paymentMethod);
        order.setTotalAmount(items.subtotal());
        order.setDiscountAmount(discount);
        order.setShippingFee(shippingFee);
        order.setFinalAmount(finalAmount);
        order.setVoucherId(voucher == null ? null : voucher.getId());
        order.setVoucherCode(voucher == null ? null : voucher.getCode());
        order.setCustomerVoucherId(customerVoucherId);
        order.setLoyaltyVoucherDiscount(loyaltyDiscount);
        Order insertedOrder = orderRepository.insertCheckoutOrder(order,
                request.getReceiverName() + " | " + request.getReceiverPhone() + " | " + request.getAddressLine());
        orderItemService.createMany(insertedOrder.getId(), items);

        if (voucher != null) {
            if (customerId != null && customerId > 0) {
                customerVoucherUsageRepository.increaseUsage(voucher.getId(), customerId);
            }
        }

        CompletableFuture<PaymentService.PayosLink> payosFuture = null;
        long orderCode = 0L;
        if (PaymentMethod.PAYOS.equals(paymentMethod)) {
            orderCode = paymentService.generateUniqueOrderCode();
            long payosOrderCode = orderCode;
            String cancelUrl = payOSConfig.baseUrl + "/payment/" + payosOrderCode + "/cancel";
            String returnUrl = payOSConfig.baseUrl + "/payment/" + payosOrderCode + "/return";
            payosFuture = queryExecutor.async(() -> paymentService.requestPayosLink(
                    payosOrderCode, finalAmount, insertedOrder.getCode(), returnUrl, cancelUrl));
        }

        Invoice invoice = invoiceRepository.insertFromOrder(insertedOrder.getId(), items.subtotal(), discount,
                shippingFee, paymentMethod, customerId, branchId, userId);
        if (customerVoucherId != null) {
            if (customerVoucherRepository.attachInvoice(customerVoucherId, customerId, invoice.getId()) != 1) {
                throw unavailableCustomerVoucher();
            }
        }
        if (voucher != null) {
            invoiceVoucherRepository.insert(invoice.getId(), voucher.getId(), regularDiscount);
        }
        if (loyaltyVoucherId != null) {
            invoiceVoucherRepository.insert(invoice.getId(), loyaltyVoucherId, loyaltyDiscount);
        }

        PaymentService.PayosLink payosLink = payosFuture == null ? null : payosFuture.join();
        Payment payment = new Payment();
        payment.setOrderId(insertedOrder.getId());
        payment.setInvoiceId(invoice.getId());
        payment.setAmount(finalAmount);
        payment.setPaymentType("FULL");
        payment.setStatus("PENDING");
        payment.setPaymentProvider(paymentMethod);
        if (payosLink != null) {
            payment.setOrderCode(payosLink.orderCode());
            payment.setPaymentLinkId(payosLink.paymentLinkId());
            payment.setCheckoutUrl(payosLink.checkoutUrl());
            payment.setQrCode(payosLink.qrCode());
        }
        Payment insertedPayment = paymentRepository.insert(payment);
        if (insertedPayment == null) throw new AppException(ErrorCode.INPUT_WRONG);

        return new CheckoutResponse(insertedOrder.getId(), insertedOrder.getCode(), invoice.getId(),
                invoice.getInvoiceCode(), paymentMethod, "PENDING", finalAmount,
                payosLink == null ? null : payosLink.checkoutUrl(), payosLink == null ? null : payosLink.qrCode());
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) return PaymentMethod.COD;
        String normalized = paymentMethod.trim().toUpperCase(java.util.Locale.ROOT);
        return PaymentMethod.CHECKOUT_METHODS[0].equals(normalized)
                || PaymentMethod.CHECKOUT_METHODS[1].equals(normalized) ? normalized : PaymentMethod.COD;
    }

    private int branchId(CheckoutRequest request) {
        return request.getBranchId() == null || request.getBranchId() <= 0
                ? checkoutConfig.defaultBranchId : request.getBranchId();
    }

    private Integer resolveCustomerId(CheckoutRequest request, Integer userId) {
        Integer customerId = userId == null || userId <= 0
                ? null : customerRepository.findCustomerIdByAccountUserId(userId);
        if (customerId == null && request.getCustomerVoucherId() != null) {
            throw unavailableCustomerVoucher();
        }
        return customerId == null ? request.getCustomerId() : customerId;
    }

    private AppException unavailableCustomerVoucher() {
        return new AppException("Voucher không khả dụng");
    }

    private boolean isCustomerVoucherAvailable(vn.aeoc.entity.data.mysql.sale.CustomerVoucher voucher) {
        return voucher != null && (voucher.getExpiresAt() == null
                || !voucher.getExpiresAt().isBefore(java.time.LocalDateTime.now()));
    }
}
