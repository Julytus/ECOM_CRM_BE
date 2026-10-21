package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import vn.aeoc.entity.data.mysql.sale.Payment;

import java.util.List;
import java.time.LocalDateTime;

import static vn.entity.backend.tables.Payment.PAYMENT;
import static vn.entity.backend.tables.Invoice.INVOICE;

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

    public List<Payment> findExpiredPendingPayos(LocalDateTime expiredBefore) {
        var latestPending = PAYMENT.as("latest_pending");
        var latestPendingId = dsl.select(latestPending.ID)
                .from(latestPending)
                .where(latestPending.INVOICE_ID.eq(PAYMENT.INVOICE_ID)
                        .and(latestPending.STATUS.eq("PENDING")))
                .orderBy(latestPending.CREATED_TIME.desc(), latestPending.ID.desc())
                .limit(1);
        return dsl.select(PAYMENT.fields())
                .from(PAYMENT.join(INVOICE).on(INVOICE.ID.eq(PAYMENT.INVOICE_ID)))
                .where(INVOICE.STATUS.eq(vn.aeoc.crm.constant.InvoiceConstant.STATUS_UNPAID)
                        .and(INVOICE.PAYMENT_METHOD.eq("PAYOS"))
                        .and(PAYMENT.STATUS.eq("PENDING"))
                        .and(PAYMENT.CREATED_TIME.lt(expiredBefore))
                        .and(PAYMENT.ID.eq(latestPendingId)))
                .orderBy(PAYMENT.CREATED_TIME.asc(), PAYMENT.ID.asc())
                .fetchInto(Payment.class);
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
