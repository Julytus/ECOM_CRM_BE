package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
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
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.data.report.RevenueHourChart;
import vn.aeoc.crm.data.report.RevenueToday;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.entity.backend.tables.records.InvoiceRecord;

import java.time.LocalDateTime;
import java.util.List;

import static vn.entity.backend.tables.BoughtProduct.BOUGHT_PRODUCT;
import static vn.entity.backend.tables.BoughtService.BOUGHT_SERVICE;
import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.Invoice.INVOICE;
import static vn.entity.backend.tables.User.USER;

@Repository
public class InvoiceRepository extends AbsMysqlRepository<InvoiceRecord, Invoice, Integer> {
    public InvoiceRepository(DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    protected TableImpl<InvoiceRecord> getTable() {
        return INVOICE;
    }

    private Condition getWhereCondition(String keyword, Integer customerId, List<Integer> status, Integer branchId) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(INVOICE.BRANCH_ID.eq(branchId));
        }

        if (!StringUtils.isBlank(keyword)) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";

            condition = condition.and(
                    DSL.lower(INVOICE.INVOICE_CODE).like(kw)
                            .or(DSL.lower(INVOICE.INVOICE_TYPE).like(kw))
            );
        }

        if (CommonUtils.NVL(customerId) > 0) {
            condition = condition.and(INVOICE.CUSTOMER_ID.eq(customerId));
        }

        if (!status.isEmpty()) {
            condition = condition.and(INVOICE.STATUS.in(status));
        }

        return condition;
    }

    public Invoice updateDraftInvoice(Invoice invoice) {
        dslContext.update(INVOICE)
                .set(INVOICE.AMOUNT, CommonUtils.NVL(invoice.getAmount()))
                .set(INVOICE.DISCOUNT, CommonUtils.NVL(invoice.getDiscount()))
                .set(INVOICE.FEE, CommonUtils.NVL(invoice.getFee()))
                .where(INVOICE.ID.eq(invoice.getId()))
                .execute();
        return invoice;
    }

    public Double calculateDraftAmount(Integer invoiceId) {
        Double productTotal = dslContext
                .select(DSL.coalesce(DSL.sum(BOUGHT_PRODUCT.FEE), 0.0))
                .from(BOUGHT_PRODUCT)
                .where(
                        BOUGHT_PRODUCT.INVOICE_ID.eq(invoiceId)
                                .and(BOUGHT_PRODUCT.STATUS.eq(1))
                )
                .fetchOne(0, Double.class);

        Double serviceTotal = dslContext
                .select(DSL.coalesce(DSL.sum(BOUGHT_SERVICE.FEE), 0.0))
                .from(BOUGHT_SERVICE)
                .where(
                        BOUGHT_SERVICE.INVOICE_ID.eq(invoiceId)
                                .and(BOUGHT_SERVICE.STATUS.eq(1))
                )
                .fetchOne(0, Double.class);

        return CommonUtils.NVL(productTotal) + CommonUtils.NVL(serviceTotal);
    }

    public List<Field<?>> getJoinFields() {
        var u = USER.as("u");
        var c = CUSTOMER.as("c");

        return mergeFields(
                c.NAME.as("customerName"),
                u.NAME.as("userName")
        );
    }



    private Table<?> getJoinTable() {
        var u = USER.as("u");
        var c = CUSTOMER.as("c");

        return INVOICE
                .leftJoin(c).on(c.ID.eq(INVOICE.CUSTOMER_ID))
                .leftJoin(u).on(u.ID.eq(INVOICE.USER_ID));
    }


    public List<Invoice> getInvoiceByCriteria(String keyword, Integer customerId, List<Integer> status, Integer branchId, Pageable pageable) {
        if (StringUtils.isBlank(keyword)) {
            return getInvoiceByCriteriaNoKeywordFast(customerId, status, branchId, pageable);
        }

        Condition condition = getWhereCondition(keyword, customerId, status, branchId);
        return getListByCriteriaWithJoin(getJoinFields(), getJoinTable(), condition, INVOICE.ID.desc(), pageable);
    }

    /*
     * Fast path for no-keyword query:
     * query id page first, then fetch joined rows by that id set.
     */
    private List<Invoice> getInvoiceByCriteriaNoKeywordFast(
            Integer customerId,
            List<Integer> status,
            Integer branchId,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(null, customerId, status, branchId);

        List<Integer> invoiceIds = dslContext
                .select(INVOICE.ID)
                .from(INVOICE)
                .where(condition)
                .orderBy(INVOICE.ID.desc())
                .limit(pageable.getLimit())
                .offset(pageable.getOffset())
                .fetchInto(Integer.class);

        if (invoiceIds.isEmpty()) {
            return List.of();
        }

        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                INVOICE.ID.in(invoiceIds),
                INVOICE.ID.desc()
        );
    }

    public Long countInvoiceByCriteria(String keyword, Integer customerId, List<Integer> status, Integer branchId) {
        Condition condition = getWhereCondition(keyword, customerId, status, branchId);
        return getCountWithCriteria(condition);
    }

    public Invoice getInvoiceById(Integer id) {
        return getOneByCriteriaWithJoin(getJoinFields(), getJoinTable(), INVOICE.ID.eq(id));
    }

    // Lấy hóa đơn nháp của khách hàng
    public Invoice getDraftInvoice(Integer customerId, Integer branchId) {
        Condition condition = INVOICE.CUSTOMER_ID.eq(customerId)
                .and(INVOICE.STATUS.eq(InvoiceConstant.STATUS_DRAFT));
        if (CommonUtils.NVL(branchId) > 0) {
            condition = condition.and(INVOICE.BRANCH_ID.eq(branchId));
        }
        return getOneByCriteriaWithJoin(getJoinFields(), getJoinTable(), condition);
    }

    // ==========================
    //  INSERT
    // ==========================

    public Integer updateInvoiceAmount(Integer invoiceId, Double amount) {
        return dslContext.update(INVOICE)
                .set(INVOICE.AMOUNT, CommonUtils.NVL(amount))
                .where(INVOICE.ID.eq(invoiceId))
                .execute();
    }

    public Invoice insertDraftInvoice(Integer customerId, Integer userId, Integer branchId) {
        LocalDateTime now = LocalDateTime.now();

        Invoice invoice = new Invoice();
        invoice.setCreatedTime(now);
        invoice.setUpdatedTime(now);
        invoice.setInvoiceCode(CommonUtils.nextCode("INV"));
        invoice.setInvoiceType(InvoiceConstant.SALE_INVOICE);
        invoice.setStatus(InvoiceConstant.STATUS_DRAFT);
        invoice.setUserId(userId);
        invoice.setCustomerId(customerId);
        invoice.setBranchId(branchId);

        return insertReturning(invoice);
    }

    public Integer updateStatus(Integer id, Integer status) {
        return updateField(id, INVOICE.STATUS, status);
    }

    //Xóa mềm
    public Integer deleteInvoice(Integer id) {
        return updateField(id, INVOICE.STATUS, InvoiceConstant.STATUS_CANCEL);
    }


    // Doanh thu theo giờ trong ngày hôm nay
    public List<RevenueHourChart> getRevenueByHourToday(Integer branchId) {

        var start = LocalDateTime.now().toLocalDate().atStartOfDay();
        var end = start.plusDays(1);

        Condition cond = INVOICE.STATUS.eq(InvoiceConstant.STATUS_DONE)
                .and(INVOICE.CREATED_TIME.ge(start))
                .and(INVOICE.CREATED_TIME.lt(end));
        if (CommonUtils.NVL(branchId) > 0) {
            cond = cond.and(INVOICE.BRANCH_ID.eq(branchId));
        }

        return dslContext
                .select(
                        DSL.hour(INVOICE.CREATED_TIME).as("hour"),

                        DSL.count(INVOICE.ID).as("totalInvoice"),

                        DSL.coalesce(
                                DSL.sum(INVOICE.FEE),
                                0.0
                        ).as("totalAmount")
                )
                .from(INVOICE)
                .where(cond)
                .groupBy(
                        DSL.hour(INVOICE.CREATED_TIME)
                )
                .orderBy(
                        DSL.hour(INVOICE.CREATED_TIME)
                )
                .fetchInto(RevenueHourChart.class);

    }

    // Doanh thu hôm nay
    public RevenueToday getRevenueToday(Integer branchId) {
        var start = LocalDateTime.now().toLocalDate().atStartOfDay();
        var end = start.plusDays(1);

        Condition cond = INVOICE.STATUS.eq(InvoiceConstant.STATUS_DONE)
                .and(INVOICE.CREATED_TIME.ge(start))
                .and(INVOICE.CREATED_TIME.lt(end));
        if (CommonUtils.NVL(branchId) > 0) {
            cond = cond.and(INVOICE.BRANCH_ID.eq(branchId));
        }

        return dslContext
                .select(
                        DSL.coalesce(DSL.count(INVOICE.ID)).as("totalInvoice"),
                        DSL.coalesce(DSL.sum(INVOICE.FEE), 0.0).as("totalAmount"),

                        DSL.coalesce(DSL.sum(
                                DSL.when(INVOICE.PAYMENT_TYPE.eq(InvoiceConstant.PAYMENT_CASH), INVOICE.FEE)
                                        .otherwise(0.0)
                        ), 0.0).as("cashAmount"),

                        DSL.coalesce(DSL.sum(
                                DSL.when(INVOICE.PAYMENT_TYPE.eq(InvoiceConstant.PAYMENT_TRANSFER), INVOICE.FEE)
                                        .otherwise(0.0)
                        ), 0.0).as("transferAmount")
                )
                .from(INVOICE)
                .where(cond)
                .fetchOneInto(RevenueToday.class);
    }
}
