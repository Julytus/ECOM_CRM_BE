package vn.aeoc.crm.service.sale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import vn.aeoc.crm.config.payos.PayOSConfig;
import vn.aeoc.crm.config.socket.ChatSocketHandler;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.data.response.PaymentStatusResponse;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class PaymentService {

    private final PayOSConfig payOSConfig;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ChatSocketHandler socketHandler;
    private final ObjectMapper objectMapper;
    private final InvoiceService invoiceService;
    private final OrderRepository orderRepository;
    private RestTemplate restTemplate = new RestTemplate();

    void setRestTemplateForTest(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PaymentService(PayOSConfig payOSConfig,
                          PaymentRepository paymentRepository,
                          InvoiceRepository invoiceRepository,
                          ChatSocketHandler socketHandler,
                          ObjectMapper objectMapper,
                          InvoiceService invoiceService) {
        this(payOSConfig, paymentRepository, invoiceRepository, socketHandler, objectMapper, invoiceService, null);
    }

    @Autowired
    public PaymentService(PayOSConfig payOSConfig,
                          PaymentRepository paymentRepository,
                          InvoiceRepository invoiceRepository,
                          ChatSocketHandler socketHandler,
                          ObjectMapper objectMapper,
                          InvoiceService invoiceService,
                          OrderRepository orderRepository) {
        this.payOSConfig = payOSConfig;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.socketHandler = socketHandler;
        this.objectMapper = objectMapper;
        this.invoiceService = invoiceService;
        this.orderRepository = orderRepository;
    }

    public Payment createPayment(Integer invoiceId) {
        Invoice invoice = invoiceRepository.getById(invoiceId);
        if (invoice == null) {
            throw new RuntimeException("Invoice not found");
        }

        if (invoice.getFee() == null || invoice.getFee() <= 0) {
            return new Payment();
        }

        Payment existedPayment = paymentRepository.findByInvoiceId(invoiceId);

        if (existedPayment != null && "COMPLETED".equals(existedPayment.getStatus())) {
            throw new IllegalStateException("Invoice already paid");
        }

        String cancelUrl = payOSConfig.baseUrl + "/payment/" + invoiceId + "/cancel";
        String returnUrl = payOSConfig.baseUrl + "/payment/" + invoiceId + "/return";
        PayosLink link = requestPayosLink(generateOrderCode(), invoice.getFee(), invoice.getInvoiceCode(), returnUrl, cancelUrl);

        Payment payment = new Payment();
        payment.setInvoiceId(invoiceId);
        payment.setAmount(invoice.getFee());
        payment.setPaymentType("FULL");
        payment.setStatus("PENDING");
        payment.setPaymentProvider("PAYOS");
        payment.setOrderCode(link.orderCode());
        payment.setPaymentLinkId(link.paymentLinkId());
        payment.setCheckoutUrl(link.checkoutUrl());
        payment.setQrCode(link.qrCode());
        return paymentRepository.insert(payment);
    }

    public PayosLink requestPayosLink(long orderCode, double amount, String description, String returnUrl, String cancelUrl) {
        int roundedAmount = (int) amount;
        String rawData = String.format(
                "amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                roundedAmount, cancelUrl, description, orderCode, returnUrl
        );
        String signature = hmacSha256(rawData, payOSConfig.checksumKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", roundedAmount);
        body.put("description", description);
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);
        body.put("items", List.of(Map.of("name", "Invoice Payment", "quantity", 1, "price", roundedAmount)));
        body.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", payOSConfig.clientId);
        headers.set("x-api-key", payOSConfig.apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(payOSConfig.apiUrl, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"00".equals(root.path("code").asText())) {
                throw new RuntimeException("PayOS API error: " + root.path("desc").asText("Unknown error"));
            }
            JsonNode data = root.path("data");
            String qrText = data.path("qrCode").isMissingNode() || data.path("qrCode").isNull()
                    ? null : data.path("qrCode").asText();
            String qrCode = qrText == null ? null : generateQrCodeBase64(qrText, 300, 300);
            return new PayosLink(orderCode, data.path("paymentLinkId").asText(),
                    data.path("checkoutUrl").asText(), qrCode);
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Failed to parse PayOS response: " + response.getBody(), e);
        }
    }

    public record PayosLink(long orderCode, String paymentLinkId, String checkoutUrl, String qrCode) {}

    public long generateUniqueOrderCode() {
        return generateOrderCode();
    }

    public String generateQrCodeBase64(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] pngData = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(pngData);
        } catch (WriterException e) {
            log.error("Error generating QR code", e);
            throw new RuntimeException("Could not generate QR code", e);
        } catch (IOException e) {
            log.error("Error generating QR code", e);
            throw new RuntimeException(e);
        }
    }

    public String handleWebhook(String requestBody) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(requestBody);
        } catch (IOException e) {
            log.warn("Invalid PayOS webhook payload", e);
            return "INVALID_SIGNATURE";
        }
        JsonNode data = root.path("data");
        String signature = root.path("signature").asText("");
        if (!isValidWebhookSignature(data, signature)) return "INVALID_SIGNATURE";

        String code = root.path("code").asText();
        boolean success = root.path("success").asBoolean(false);
        if (!"00".equals(code) || !success) {
            log.warn("Webhook not success: code={}", code);
            return "IGNORED";
        }
        if (data.isMissingNode() || data.isNull() || !data.hasNonNull("orderCode")) return "IGNORED";
        Long orderCode = data.get("orderCode").asLong();
        Payment payment = paymentRepository.findByPayosOrderCode(orderCode);
        if (payment == null) {
            log.warn("Payment not found for orderCode: {}", orderCode);
            return "IGNORED";
        }
        int updated;
        try {
            updated = invoiceService.markPaid(payment.getInvoiceId(), "PAYOS");
        } catch (AppException ex) {
            Invoice invoice = invoiceRepository.getById(payment.getInvoiceId());
            if (invoice != null && (invoice.getStatus() == InvoiceConstant.STATUS_CANCEL
                    || invoice.getStatus() == InvoiceConstant.STATUS_REFUNDED)) {
                log.error("Ignoring late PayOS payment for cancelled/refunded invoiceId={}, orderCode={}, status={}",
                        payment.getInvoiceId(), orderCode, invoice.getStatus(), ex);
                return "IGNORED";
            }
            throw ex;
        }
        Invoice invoice = invoiceRepository.getById(payment.getInvoiceId());
        if (updated > 0 && invoice != null && invoice.getUserId() != null) {
            socketHandler.emitPaymentDone(invoice.getUserId(), payment.getInvoiceId());
        }
        log.info("Payment COMPLETED: orderCode={}, invoiceId={}", orderCode, payment.getInvoiceId());
        return "SUCCESS";
    }

    @Transactional
    public PaymentStatusResponse syncFromPayos(String orderCode, Integer userId) {
        long code;
        try { code = Long.parseLong(orderCode); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Mã đơn hàng không hợp lệ", ex); }
        Payment payment = paymentRepository.findByPayosOrderCode(code);
        if (payment == null) throw new AppException(ErrorCode.DATA_NOT_FOUND);
        Invoice invoice = invoiceRepository.getById(payment.getInvoiceId());
        if (invoice == null) throw new AppException(ErrorCode.DATA_NOT_FOUND);
        if (userId != null && userId > 0 && !userId.equals(invoice.getUserId())) {
            throw new AppException(ErrorCode.DATA_NOT_FOUND);
        }

        syncPaymentFromPayos(payment, invoice);
        Invoice refreshed = invoiceRepository.getById(invoice.getId());
        vn.aeoc.entity.data.mysql.sale.Order order = orderRepository == null || invoice.getOrderId() == null
                ? null : orderRepository.getById(invoice.getOrderId());
        return new PaymentStatusResponse(invoice.getOrderId(), order == null ? null : order.getCode(),
                invoice.getId(), refreshed == null ? invoice.getStatus() : refreshed.getStatus(),
                order == null ? null : order.getStatus(), order == null ? null : order.getPaymentStatus(),
                refreshed == null ? invoice.getReceiptDate() : refreshed.getReceiptDate());
    }

    void syncFromPayosForExpiry(Payment payment) {
        Invoice invoice = invoiceRepository.getById(payment.getInvoiceId());
        if (invoice == null) throw new AppException(ErrorCode.DATA_NOT_FOUND);
        syncPaymentFromPayos(payment, invoice);
    }

    private String syncPaymentFromPayos(Payment payment, Invoice invoice) {
        if (payment.getOrderCode() == null) throw new IllegalStateException("Pending PayOS payment has no orderCode");
        long code = payment.getOrderCode();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", payOSConfig.clientId);
        headers.set("x-api-key", payOSConfig.apiKey);
        ResponseEntity<String> response = restTemplate.exchange(payOSConfig.apiUrl + "/" + code,
                org.springframework.http.HttpMethod.GET, new HttpEntity<>(headers), String.class);
        String status;
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"00".equals(root.path("code").asText()))
                throw new IllegalStateException("PayOS API error: " + root.path("desc").asText());
            status = root.path("data").path("status").asText("").toUpperCase(Locale.ROOT);
            if ("PAID".equals(status)) invoiceService.markPaid(invoice.getId(), "PAYOS");
            else if ("CANCELLED".equals(status) || "EXPIRED".equals(status)) {
                if (invoice.getStatus() == InvoiceConstant.STATUS_UNPAID) invoiceService.cancelOrderInvoice(invoice.getId());
            } else if ("PENDING".equals(status)) {
                log.info("PayOS payment is still pending; invoice {} remains unchanged", invoice.getId());
            } else {
                throw new IllegalStateException("PayOS returned an unsupported payment status: " + status);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse PayOS response", ex);
        }
        return status;
    }

    @Transactional
    public PayosLink retryPayment(Integer invoiceId, Integer userId) {
        Invoice invoice = invoiceRepository.getById(invoiceId);
        if (invoice == null || (userId != null && userId > 0 && !userId.equals(invoice.getUserId())))
            throw new AppException(ErrorCode.DATA_NOT_FOUND);
        if (invoice.getStatus() != InvoiceConstant.STATUS_UNPAID || !"PAYOS".equalsIgnoreCase(invoice.getPaymentMethod()))
            throw new AppException("Chỉ có thể tạo lại thanh toán PayOS chưa thanh toán", 400);
        if (invoice.getOrderId() == null) throw new IllegalStateException("PayOS checkout invoice has no order_id");
        paymentRepository.cancelPendingByInvoice(invoiceId);
        long code = generateUniqueOrderCode();
        String cancelUrl = payOSConfig.baseUrl + "/payment/" + code + "/cancel";
        String returnUrl = payOSConfig.baseUrl + "/payment/" + code + "/return";
        PayosLink link = requestPayosLink(code, invoice.getFee(), invoice.getInvoiceCode(), returnUrl, cancelUrl);
        Payment payment = new Payment();
        payment.setOrderId(invoice.getOrderId());
        payment.setInvoiceId(invoiceId);
        payment.setAmount(invoice.getFee());
        payment.setPaymentType("FULL");
        payment.setStatus("PENDING");
        payment.setPaymentProvider("PAYOS");
        payment.setOrderCode(link.orderCode());
        payment.setPaymentLinkId(link.paymentLinkId());
        payment.setCheckoutUrl(link.checkoutUrl());
        payment.setQrCode(link.qrCode());
        paymentRepository.insert(payment);
        return link;
    }

    private boolean isValidWebhookSignature(JsonNode data, String signature) {
        String key = payOSConfig.checksumKey;
        if (key == null || key.isBlank() || data == null || !data.isObject() || signature == null || signature.isBlank()) {
            return false;
        }
        List<String> keys = new ArrayList<>();
        data.fieldNames().forEachRemaining(keys::add);
        Collections.sort(keys);
        StringJoiner payload = new StringJoiner("&");
        for (String field : keys) {
            JsonNode value = data.get(field);
            String text = value == null || value.isNull() ? "" : value.isValueNode() ? value.asText() : value.toString();
            payload.add(field + "=" + text);
        }
        String expected = hmacSha256(payload.toString(), key);
        return MessageDigest.isEqual(expected.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII),
                signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    private long generateOrderCode() {
        long orderCode;
        do {
            orderCode = (System.currentTimeMillis() % 10_000_000L) * 100L
                    + ThreadLocalRandom.current().nextInt(100);
        } while (paymentRepository.existsByOrderCode(orderCode));
        return orderCode;
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC SHA256", e);
        }
    }
}
