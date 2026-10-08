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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import vn.aeoc.crm.config.payos.PayOSConfig;
import vn.aeoc.crm.config.socket.ChatSocketHandler;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class PaymentService {

    private final PayOSConfig payOSConfig;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ChatSocketHandler socketHandler;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public PaymentService(PayOSConfig payOSConfig,
                          PaymentRepository paymentRepository,
                          InvoiceRepository invoiceRepository,
                          ChatSocketHandler socketHandler,
                          ObjectMapper objectMapper) {
        this.payOSConfig = payOSConfig;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.socketHandler = socketHandler;
        this.objectMapper = objectMapper;
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

        double amountInVND = invoice.getFee();
        int amount = (int) amountInVND;
        String description = invoice.getInvoiceCode();

        String cancelUrl = payOSConfig.baseUrl + "/payment/" + invoiceId + "/cancel";
        String returnUrl = payOSConfig.baseUrl + "/payment/" + invoiceId + "/return";

        long orderCode = generateOrderCode(invoiceId);

        String rawData = String.format(
                "amount=%d&cancelUrl=%s&description=%s&orderCode=%d&returnUrl=%s",
                amount, cancelUrl, description, orderCode, returnUrl
        );

        String signature = hmacSha256(rawData, payOSConfig.checksumKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", "Invoice Payment");
        item.put("quantity", 1);
        item.put("price", amount);
        body.put("items", List.of(item));

        body.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", payOSConfig.clientId);
        headers.set("x-api-key", payOSConfig.apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                payOSConfig.apiUrl, entity, String.class
        );

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            String code = root.get("code").asText();
            if (!"00".equals(code)) {
                String desc = root.has("desc") ? root.get("desc").asText() : "Unknown error";
                throw new RuntimeException("PayOS API error: " + desc);
            }

            JsonNode data = root.get("data");
            String checkoutUrl = data.get("checkoutUrl").asText();
            String qrCode = data.has("qrCode") && !data.get("qrCode").isNull() ? data.get("qrCode").asText() : null;
            String paymentLinkId = data.get("paymentLinkId").asText();

            Payment payment = new Payment();
            payment.setInvoiceId(invoiceId);
            payment.setAmount(amountInVND);
            payment.setPaymentType("FULL");
            payment.setStatus("PENDING");
            payment.setOrderCode(orderCode);
            payment.setPaymentLinkId(paymentLinkId);
            payment.setCheckoutUrl(checkoutUrl);
            payment.setQrCode(generateQrCodeBase64(qrCode, 300, 300));

            return paymentRepository.insert(payment);

        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new RuntimeException("Failed to parse PayOS response: " + response.getBody(), e);
        }
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

    @Transactional
    public String handleWebhook(String requestBody) {
        try {
            JsonNode root = objectMapper.readTree(requestBody);
            String code = root.get("code").asText();
            boolean success = root.has("success") && root.get("success").asBoolean();

            if (!"00".equals(code) || !success) {
                log.warn("Webhook not success: code={}", code);
                return "IGNORED";
            }

            JsonNode data = root.get("data");
            Long orderCode = data.get("orderCode").asLong();

            Payment payment = paymentRepository.findByPayosOrderCode(orderCode);
            if (payment == null) {
                log.warn("Payment not found for orderCode: {}", orderCode);
                return "IGNORED";
            }

            payment.setStatus("COMPLETED");
            paymentRepository.updateStatus(payment.getId(), "COMPLETED");

            invoiceRepository.updateStatus(payment.getInvoiceId(), 1);

            Invoice invoice = invoiceRepository.getById(payment.getInvoiceId());
            if (invoice != null && invoice.getUserId() != null) {
                socketHandler.emitPaymentDone(invoice.getUserId(), payment.getInvoiceId());
            }

            log.info("Payment COMPLETED: orderCode={}, invoiceId={}", orderCode, payment.getInvoiceId());

            return "SUCCESS";
        } catch (Exception e) {
            log.error("Failed to handle webhook: {}", e.getMessage(), e);
            return "ERROR: " + e.getMessage();
        }
    }

    private long generateOrderCode(Integer invoiceId) {
        long min = 1_000_000;
        long max = 9_999_999;
        return min + (long) (Math.random() * (max - min + 1));
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