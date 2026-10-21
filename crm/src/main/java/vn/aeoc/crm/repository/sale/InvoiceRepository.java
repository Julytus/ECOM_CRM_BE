package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.data.report.RevenueHourChart;
import vn.aeoc.crm.data.report.RevenueToday;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.entity.backend.tables.records.InvoiceRecord;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static vn.entity.backend.tables.BoughtProduct.BOUGHT_PRODUCT;
import static vn.entity.backend.tables.BoughtService.BOUGHT_SERVICE;
import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.Invoice.INVOICE;
import static vn.entity.backend.tables.Order.ORDER;
import static vn.entity.backend.tables.User.USER;

@Repository
public class InvoiceRepository extends AbsMysqlRepository<InvoiceRecord, Invoice, Integer> {
    public record InvoiceFilter(String keyword, Integer customerId, List<Integer> status,
                                String paymentMethod, LocalDate fromDate, LocalDate toDate,
                                String orderCode, String sortBy, String sortDir, Integer branchId) {}

    private static final Set<String> SORT_FIELDS = Set.of("id", "createdTime", "fee");

    public InvoiceRepository(DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    protected TableImpl<InvoiceRecord> getTable() {
        return INVOICE;
    }

    Condition getWhereCondition(InvoiceFilter filter) {
        Condition condition = DSL.trueCondition();

        if (CommonUtils.NVL(filter.branchId()) > 0) {
            condition = condition.and(INVOICE.BRANCH_ID.eq(filter.branchId()));
        }

        var o = ORDER.as("o");
        if (!StringUtils.isBlank(filter.keyword())) {
            String kw = "%" + filter.keyword().trim().toLowerCase(Locale.ROOT) + "%";

            condition = condition.and(
                    DSL.lower(INVOICE.INVOICE_CODE).like(kw)
                            .or(DSL.lower(INVOICE.INVOICE_TYPE).like(kw))
                            .or(DSL.lower(o.CODE).like(kw))
                            .or(DSL.lower(o.BUYER_NAME).like(kw))
                            .or(DSL.lower(o.BUYER_PHONE).like(kw))
            );
        }

        if (CommonUtils.NVL(filter.customerId()) > 0) {
            condition = condition.and(INVOICE.CUSTOMER_ID.eq(filter.customerId()));
        }

        if (filter.status() != null && !filter.status().isEmpty()) {
            condition = condition.and(INVOICE.STATUS.in(filter.status()));
        }
        if (StringUtils.isNotBlank(filter.paymentMethod())) {
            Field<String> effectivePaymentMethod = DSL.coalesce(
                    INVOICE.PAYMENT_METHOD,
                    DSL.choose(INVOICE.PAYMENT_TYPE)
                            .when(1, "CASH")
                            .when(2, "TRANSFER")
                            .otherwise((String) null)
            );
            condition = condition.and(effectivePaymentMethod.eq(
                    filter.paymentMethod().trim().toUpperCase(Locale.ROOT)));
        }
        var effectiveTime = DSL.coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME);
        if (filter.fromDate() != null) {
            condition = condition.and(effectiveTime.ge(filter.fromDate().atStartOfDay()));
        }
        if (filter.toDate() != null) {
            condition = condition.and(effectiveTime.lt(filter.toDate().plusDays(1).atStartOfDay()));
        }
        if (StringUtils.isNotBlank(filter.orderCode())) {
            condition = condition.and(DSL.lower(o.CODE).like("%" + filter.orderCode().trim().toLowerCase(Locale.ROOT) + "%"));
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
        var o = ORDER.as("o");

        return mergeFields(
                c.NAME.as("customerName"),
                u.NAME.as("userName"),
                o.CODE.as("orderCode"),
                o.BUYER_NAME.as("buyerName"),
                o.BUYER_PHONE.as("buyerPhone"),
                o.STATUS.as("orderStatus")
        );
    }



    private Table<?> getJoinTable() {
        var u = USER.as("u");
        var c = CUSTOMER.as("c");
        var o = ORDER.as("o");

        return INVOICE
                .leftJoin(c).on(c.ID.eq(INVOICE.CUSTOMER_ID))
                .leftJoin(u).on(u.ID.eq(INVOICE.USER_ID))
                .leftJoin(o).on(o.ID.eq(INVOICE.ORDER_ID));
    }


    private org.jooq.SortField<?> sortField(InvoiceFilter filter) {
        String sortBy = StringUtils.defaultIfBlank(filter.sortBy(), "id");
        if (!SORT_FIELDS.contains(sortBy)) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        boolean ascending = "asc".equalsIgnoreCase(filter.sortDir());
        return switch (sortBy) {
            case "createdTime" -> ascending ? INVOICE.CREATED_TIME.asc() : INVOICE.CREATED_TIME.desc();
            case "fee" -> ascending ? INVOICE.FEE.asc() : INVOICE.FEE.desc();
            default -> ascending ? INVOICE.ID.asc() : INVOICE.ID.desc();
        };
    }

    public List<Invoice> getInvoiceByCriteria(InvoiceFilter filter, Pageable pageable) {
        var sort = sortField(filter);
        Condition condition = getWhereCondition(filter);
        if (StringUtils.isBlank(filter.keyword()) && StringUtils.isBlank(filter.orderCode())) {
            return getInvoiceByCriteriaNoKeywordFast(pageable, condition, sort, hasSortTieBreaker(filter));
        }
        var query = dslContext.select(getJoinFields())
                .from(getJoinTable())
                .where(condition);
        if (hasSortTieBreaker(filter)) {
            return query.orderBy(sort, INVOICE.ID.desc())
                    .limit(pageable.getLimit()).offset(pageable.getOffset())
                    .fetchInto(Invoice.class);
        }
        return query.orderBy(sort)
                .limit(pageable.getLimit()).offset(pageable.getOffset())
                .fetchInto(Invoice.class);
    }

    /*
     * Fast path for no-keyword query:
     * query id page first, then fetch joined rows by that id set.
     */
    private List<Invoice> getInvoiceByCriteriaNoKeywordFast(
            Pageable pageable, Condition condition,
            org.jooq.SortField<?> sort, boolean tieBreaker
    ) {
        var idQuery = dslContext
                .select(INVOICE.ID)
                .from(INVOICE.leftJoin(ORDER.as("o")).on(ORDER.as("o").ID.eq(INVOICE.ORDER_ID)))
                .where(condition);
        List<Integer> invoiceIds = (tieBreaker
                ? idQuery.orderBy(sort, INVOICE.ID.desc())
                : idQuery.orderBy(sort))
                .limit(pageable.getLimit()).offset(pageable.getOffset())
                .fetchInto(Integer.class);

        if (invoiceIds.isEmpty()) {
            return List.of();
        }

        var rowQuery = dslContext.select(getJoinFields())
                .from(getJoinTable())
                .where(INVOICE.ID.in(invoiceIds));
        if (tieBreaker) {
            return rowQuery.orderBy(sort, INVOICE.ID.desc()).fetchInto(Invoice.class);
        }
        return rowQuery.orderBy(sort).fetchInto(Invoice.class);
    }

    private boolean hasSortTieBreaker(InvoiceFilter filter) {
        return !"id".equals(StringUtils.defaultIfBlank(filter.sortBy(), "id"));
    }

    public Long countInvoiceByCriteria(InvoiceFilter filter) {
        return dslContext.selectCount()
                .from(INVOICE.leftJoin(ORDER.as("o")).on(ORDER.as("o").ID.eq(INVOICE.ORDER_ID)))
                .where(getWhereCondition(filter))
                .fetchOne(0, Long.class);
    }

    public Invoice getInvoiceById(Integer id) {
        return getOneByCriteriaWithJoin(getJoinFields(), getJoinTable(), INVOICE.ID.eq(id));
    }

    public Invoice getInvoiceById(Integer id, int branchId) {
        Condition condition = INVOICE.ID.eq(id);
        if (branchId > 0) condition = condition.and(INVOICE.BRANCH_ID.eq(branchId));
        return getOneByCriteriaWithJoin(getJoinFields(), getJoinTable(), condition);
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

    public Invoice insertFromOrder(Integer orderId, double subtotal, double discount, double shippingFee,
                                    String paymentMethod, Integer customerId, Integer branchId, Integer userId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceCode(CommonUtils.nextCode("INV"));
        invoice.setInvoiceType(InvoiceConstant.SALE_INVOICE);
        invoice.setStatus(InvoiceConstant.STATUS_UNPAID);
        invoice.setAmount(subtotal);
        invoice.setDiscount(discount);
        invoice.setShippingFee(shippingFee);
        invoice.setFee(subtotal - discount + shippingFee);
        invoice.setPaymentMethod(paymentMethod);
        invoice.setOrderId(orderId);
        invoice.setCustomerId(customerId);
        invoice.setBranchId(branchId);
        invoice.setUserId(userId);

        return dslContext.insertInto(INVOICE)
                .set(INVOICE.INVOICE_CODE, invoice.getInvoiceCode())
                .set(INVOICE.INVOICE_TYPE, invoice.getInvoiceType())
                .set(INVOICE.STATUS, invoice.getStatus())
                .set(INVOICE.AMOUNT, invoice.getAmount())
                .set(INVOICE.DISCOUNT, invoice.getDiscount())
                .set(INVOICE.FEE, invoice.getFee())
                .set(INVOICE.USER_ID, invoice.getUserId())
                .set(INVOICE.CUSTOMER_ID, invoice.getCustomerId())
                .set(INVOICE.BRANCH_ID, invoice.getBranchId())
                .set(INVOICE.ORDER_ID, invoice.getOrderId())
                .set(INVOICE.SHIPPING_FEE, invoice.getShippingFee())
                .set(INVOICE.PAYMENT_METHOD, invoice.getPaymentMethod())
                .returning()
                .fetchOneInto(Invoice.class);
    }

    public Integer updateStatus(Integer id, Integer status) {
        return updateField(id, INVOICE.STATUS, status);
    }

    public Integer transitionStatus(Integer id, Integer fromStatus, Integer toStatus,
                                    Map<? extends Field<?>, ?> extraFields) {
        Map<Field<?>, Object> fields = new HashMap<>();
        if (extraFields != null) fields.putAll(extraFields);
        fields.put(INVOICE.STATUS, toStatus);
        return dslContext.update(INVOICE)
                .set(fields)
                .where(INVOICE.ID.eq(id).and(INVOICE.STATUS.eq(fromStatus)))
                .execute();
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
                .and(DSL.coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME).ge(start))
                .and(DSL.coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME).lt(end));
        if (CommonUtils.NVL(branchId) > 0) {
            cond = cond.and(INVOICE.BRANCH_ID.eq(branchId));
        }

        return dslContext
                .select(
                        DSL.hour(DSL.coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME)).as("hour"),

                        DSL.count(INVOICE.ID).as("totalInvoice"),

                        DSL.coalesce(
                                DSL.sum(INVOICE.FEE),
                                0.0
                        ).as("totalAmount")
                )
                .from(INVOICE)
                .where(cond)
                .groupBy(
                        DSL.hour(DSL.coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME))
                )
                .orderBy(
                        DSL.hour(DSL.coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME))
                )
                .fetchInto(RevenueHourChart.class);

    }

    // Doanh thu hôm nay
    public RevenueToday getRevenueToday(Integer branchId) {
        var start = LocalDateTime.now().toLocalDate().atStartOfDay();
        var end = start.plusDays(1);

        Condition cond = INVOICE.STATUS.eq(InvoiceConstant.STATUS_DONE)
                .and(DSL.coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME).ge(start))
                .and(DSL.coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME).lt(end));
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
