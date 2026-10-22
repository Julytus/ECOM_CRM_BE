package vn.aeoc.crm.service.sale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.aeoc.crm.config.checkout.CheckoutConfig;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.entity.data.mysql.sale.Payment;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryJob {
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final CheckoutConfig checkoutConfig;

    @Scheduled(fixedDelay = 300000)
    public void expireUnpaidPayosInvoices() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(Math.max(1, checkoutConfig.payosExpireMinutes));
        for (Payment payment : paymentRepository.findExpiredPendingPayos(cutoff)) {
            if (payment.getCreatedTime() == null || !payment.getCreatedTime().isBefore(cutoff)) continue;
            try {
                paymentService.syncFromPayosForExpiry(payment);
            } catch (Exception ex) {
                log.warn("PayOS expiry sync failed; leaving invoice {} unchanged (payment {}, orderCode {}): {}",
                        payment.getInvoiceId(), payment.getId(), payment.getOrderCode(), ex.getMessage());
                log.debug("PayOS expiry sync failure detail", ex);
            }
        }
    }
}
