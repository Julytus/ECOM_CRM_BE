package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import vn.aeoc.entity.data.mysql.sale.Payment;

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
                .fetchOneInto(Payment.class);
    }

    public Payment findByPayosOrderCode(Long orderCode) {
        return dsl.selectFrom(PAYMENT)
                .where(PAYMENT.ORDER_CODE.eq(orderCode))
                .fetchOne()
                .into(Payment.class);
    }

    public Payment insert(Payment payment) {
        return dsl.insertInto(PAYMENT)
                .set(PAYMENT.INVOICE_ID, payment.getInvoiceId())
                .set(PAYMENT.AMOUNT, payment.getAmount())
                .set(PAYMENT.PAYMENT_TYPE, payment.getPaymentType())
                .set(PAYMENT.STATUS, payment.getStatus())
                .set(PAYMENT.ORDER_CODE, payment.getOrderCode())
                .set(PAYMENT.PAYMENT_LINK_ID, payment.getPaymentLinkId())
                .set(PAYMENT.CHECKOUT_URL, payment.getCheckoutUrl())
                .set(PAYMENT.QR_CODE, payment.getQrCode())
                .returning()
                .fetchOne()
                .into(Payment.class);
    }

    public int updateStatus(Integer id, String status) {
        return dsl.update(PAYMENT)
                .set(PAYMENT.STATUS, status)
                .where(PAYMENT.ID.eq(id))
                .execute();
    }
}
