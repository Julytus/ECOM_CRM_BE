package vn.aeoc.crm.service.sale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.aeoc.crm.config.checkout.CheckoutConfig;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryJob {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final CheckoutConfig checkoutConfig;

    @Scheduled(fixedDelay = 300000)
    public void expireUnpaidPayosInvoices() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(Math.max(1, checkoutConfig.payosExpireMinutes));
        for (Invoice invoice : invoiceRepository.findExpiredPayosUnpaid(cutoff)) {
            try {
                invoiceService.cancelOrderInvoice(invoice.getId());
            } catch (Exception ex) {
                log.error("Failed to expire PayOS invoice {}", invoice.getId(), ex);
            }
        }
    }
}
