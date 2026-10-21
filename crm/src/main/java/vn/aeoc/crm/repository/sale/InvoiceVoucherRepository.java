package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.entity.backend.tables.InvoiceVoucher;
import vn.entity.backend.tables.records.InvoiceVoucherRecord;

import static vn.entity.backend.tables.InvoiceVoucher.INVOICE_VOUCHER;

@Repository
public class InvoiceVoucherRepository extends AbsMysqlRepository<InvoiceVoucherRecord, InvoiceVoucher, Integer> {
    public InvoiceVoucherRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<InvoiceVoucherRecord> getTable() {
        return INVOICE_VOUCHER;
    }

    public Integer insert(Integer invoiceId, Integer voucherId, Double discountAmount) {

        if (CommonUtils.NVL(voucherId) <= 0) {
            return 0;
        }

        return dslContext.insertInto(INVOICE_VOUCHER)
                .set(INVOICE_VOUCHER.INVOICE_ID, invoiceId)
                .set(INVOICE_VOUCHER.VOUCHER_ID, voucherId)
                .set(INVOICE_VOUCHER.DISCOUNT_AMOUNT, discountAmount)
                .execute();
    }

    public boolean existsByInvoice(Integer invoiceId) {
        return dslContext.fetchExists(
                dslContext.selectOne()
                        .from(INVOICE_VOUCHER)
                        .where(INVOICE_VOUCHER.INVOICE_ID.eq(invoiceId))
        );
    }

    public Integer deleteByInvoice(Integer invoiceId) {
        return dslContext.deleteFrom(INVOICE_VOUCHER)
                .where(INVOICE_VOUCHER.INVOICE_ID.eq(invoiceId))
                .execute();
    }

    public Integer findVoucherIdByInvoice(Integer invoiceId) {
        return dslContext.select(INVOICE_VOUCHER.VOUCHER_ID)
                .from(INVOICE_VOUCHER)
                .where(INVOICE_VOUCHER.INVOICE_ID.eq(invoiceId))
                .orderBy(INVOICE_VOUCHER.ID.desc())
                .limit(1)
                .fetchOne(INVOICE_VOUCHER.VOUCHER_ID);
    }

    public Integer countTotalUsed(Integer voucherId) {
        return dslContext.selectCount()
                .from(INVOICE_VOUCHER)
                .where(INVOICE_VOUCHER.VOUCHER_ID.eq(voucherId))
                .fetchOne(0, Integer.class);
    }


}
