package vn.aeoc.crm.repository.sale;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.entity.data.mysql.sale.Voucher;
import vn.entity.backend.tables.records.VoucherRecord;

import java.util.List;

import static vn.entity.backend.tables.Branch.BRANCH;
import static vn.entity.backend.tables.InvoiceVoucher.INVOICE_VOUCHER;
import static vn.entity.backend.tables.Voucher.VOUCHER;

@Repository
public class VoucherRepository extends AbsMysqlRepository<VoucherRecord, Voucher, Integer> {

    @Override
    protected TableImpl<VoucherRecord> getTable() {
        return VOUCHER;
    }

    private List<Field<?>> getSelectFields() {
        return mergeFields(
                BRANCH.NAME.as("branch_name"),
                DSL.selectCount()
                        .from(INVOICE_VOUCHER)
                        .where(INVOICE_VOUCHER.VOUCHER_ID.eq(VOUCHER.ID))
                        .asField("usage_quantity")
        );
    }

    private Table<?> getJoinTables() {
        return VOUCHER
                .leftJoin(BRANCH).on(BRANCH.ID.eq(VOUCHER.BRANCH_ID));
    }

    public List<Voucher> getByCriteria(
            String keyword,
            Integer status,
            Integer branchId,
            Pageable pageable) {

        return getListByCriteriaWithJoin(getSelectFields(), getJoinTables(),
                where(keyword, status, branchId), VOUCHER.ID.desc(), pageable);
    }


    public Long countByCriteria(String keyword, Integer status, Integer branchId) {
        return getCountWithCriteria(where(keyword, status, branchId));
    }


    public Voucher getByCode(String code) {
        return getOne(VOUCHER.CODE.eq(code));
    }

    public Boolean existByCode(String code) {
        return exists(VOUCHER.CODE.eq(code));
    }

    public Boolean existByCodeExceptVoucherId(Integer id, String code) {
        return exists(VOUCHER.CODE.eq(code).and(VOUCHER.ID.ne(id)));
    }


    private Condition where(String keyword, Integer status, Integer branchId) {
        Condition c = DSL.trueCondition();

        if (StringUtils.isNotBlank(keyword)) {
            String kw = "%" + keyword.toLowerCase() + "%";
            c = c.and(
                    DSL.lower(VOUCHER.CODE).like(kw)
                            .or(DSL.lower(VOUCHER.NAME).like(kw))
            );
        }

        if (CommonUtils.NVL(status) > 0) {
            c = c.and(VOUCHER.STATUS.eq(status));
        }

        if (CommonUtils.NVL(branchId) > 0) {
            c = c.and(VOUCHER.BRANCH_ID.eq(branchId));
        }

        return c;
    }
}
