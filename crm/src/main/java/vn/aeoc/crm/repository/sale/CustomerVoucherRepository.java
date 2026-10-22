package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import vn.aeoc.entity.data.mysql.sale.CustomerVoucher;

import java.time.LocalDateTime;

import static vn.entity.backend.tables.CustomerVoucher.CUSTOMER_VOUCHER;

@Repository
public class CustomerVoucherRepository {
    private final DSLContext dsl;

    public CustomerVoucherRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public CustomerVoucher findAvailable(Integer id, Integer customerId) {
        return dsl.selectFrom(CUSTOMER_VOUCHER)
                .where(CUSTOMER_VOUCHER.ID.eq(id)
                        .and(CUSTOMER_VOUCHER.CUSTOMER_ID.eq(customerId))
                        .and(CUSTOMER_VOUCHER.STATUS.eq("AVAILABLE")))
                .fetchOneInto(CustomerVoucher.class);
    }

    public Integer markUsedIfAvailable(Integer id, Integer customerId, Integer invoiceId) {
        return dsl.update(CUSTOMER_VOUCHER)
                .set(CUSTOMER_VOUCHER.STATUS, "USED")
                .set(CUSTOMER_VOUCHER.USED_AT, LocalDateTime.now())
                .set(CUSTOMER_VOUCHER.INVOICE_ID, invoiceId)
                .where(CUSTOMER_VOUCHER.ID.eq(id)
                        .and(CUSTOMER_VOUCHER.CUSTOMER_ID.eq(customerId))
                        .and(CUSTOMER_VOUCHER.STATUS.eq("AVAILABLE")))
                .execute();
    }

    public Integer attachInvoice(Integer id, Integer customerId, Integer invoiceId) {
        return dsl.update(CUSTOMER_VOUCHER)
                .set(CUSTOMER_VOUCHER.INVOICE_ID, invoiceId)
                .where(CUSTOMER_VOUCHER.ID.eq(id)
                        .and(CUSTOMER_VOUCHER.CUSTOMER_ID.eq(customerId))
                        .and(CUSTOMER_VOUCHER.STATUS.eq("USED")))
                .execute();
    }

    public Integer findVoucherIdByInvoice(Integer invoiceId) {
        return dsl.select(CUSTOMER_VOUCHER.VOUCHER_ID)
                .from(CUSTOMER_VOUCHER)
                .where(CUSTOMER_VOUCHER.INVOICE_ID.eq(invoiceId)
                        .and(CUSTOMER_VOUCHER.STATUS.eq("USED")))
                .fetchOne(CUSTOMER_VOUCHER.VOUCHER_ID);
    }

    public Integer releaseByInvoice(Integer invoiceId) {
        return dsl.update(CUSTOMER_VOUCHER)
                .set(CUSTOMER_VOUCHER.STATUS, "AVAILABLE")
                .setNull(CUSTOMER_VOUCHER.INVOICE_ID)
                .setNull(CUSTOMER_VOUCHER.USED_AT)
                .where(CUSTOMER_VOUCHER.INVOICE_ID.eq(invoiceId)
                        .and(CUSTOMER_VOUCHER.STATUS.eq("USED")))
                .execute();
    }
}
