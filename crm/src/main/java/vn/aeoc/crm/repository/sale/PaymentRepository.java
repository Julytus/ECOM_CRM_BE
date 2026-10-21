package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import vn.aeoc.entity.data.mysql.sale.Payment;

import java.util.List;

import static vn.entity.backend.tables.Payment.PAYMENT;

@Repository
public class PaymentRepository {

    private final DSLContext dsl;

    public PaymentRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Payment findByInvoiceId(Integer invoiceId) {
        return dsl.selectFrom(PAYMENT)
                .where(PAYMENT.INVOICE_ID.eq(invoiceId))
                .orderBy(PAYMENT.ID.desc())
                .limit(1)
                .fetchOneInto(Payment.class);
    }

    public List<Payment> findAllByInvoiceId(Integer invoiceId) {
        return dsl.selectFrom(PAYMENT)
                .where(PAYMENT.INVOICE_ID.eq(invoiceId))
                .orderBy(PAYMENT.ID.asc())
                .fetchInto(Payment.class);
    }

    public Payment findByPayosOrderCode(Long orderCode) {
        return dsl.selectFrom(PAYMENT)
                .where(PAYMENT.ORDER_CODE.eq(orderCode))
                .fetchOptionalInto(Payment.class)
                .orElse(null);
    }

    public Payment insert(Payment payment) {
        return dsl.insertInto(PAYMENT)
                .set(PAYMENT.INVOICE_ID, payment.getInvoiceId())
                .set(PAYMENT.ORDER_ID, payment.getOrderId())
                .set(PAYMENT.AMOUNT, payment.getAmount())
                .set(PAYMENT.PAYMENT_TYPE, payment.getPaymentType())
                .set(PAYMENT.STATUS, payment.getStatus())
                .set(PAYMENT.PAYMENT_PROVIDER, payment.getPaymentProvider())
                .set(PAYMENT.ORDER_CODE, payment.getOrderCode())
                .set(PAYMENT.PAYMENT_LINK_ID, payment.getPaymentLinkId())
                .set(PAYMENT.CHECKOUT_URL, payment.getCheckoutUrl())
                .set(PAYMENT.QR_CODE, payment.getQrCode())
                .returning()
                .fetchOne()
                .into(Payment.class);
    }

    public boolean existsByOrderCode(long orderCode) {
        return dsl.fetchExists(dsl.selectOne().from(PAYMENT).where(PAYMENT.ORDER_CODE.eq(orderCode)));
    }

    public int updateStatus(Integer id, String status) {
        return dsl.update(PAYMENT)
                .set(PAYMENT.STATUS, status)
                .where(PAYMENT.ID.eq(id))
                .execute();
    }

    public int cancelPendingByInvoice(Integer invoiceId) {
        return dsl.update(PAYMENT)
                .set(PAYMENT.STATUS, "CANCELLED")
                .where(PAYMENT.INVOICE_ID.eq(invoiceId).and(PAYMENT.STATUS.eq("PENDING")))
                .execute();
    }

    public int markStatusByInvoice(Integer invoiceId, String status) {
        return dsl.update(PAYMENT)
                .set(PAYMENT.STATUS, status)
                .where(PAYMENT.INVOICE_ID.eq(invoiceId))
                .execute();
    }
}
