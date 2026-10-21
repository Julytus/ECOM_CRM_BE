package vn.aeoc.crm.repository.sale;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.data.report.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.jooq.impl.DSL.*;
import static vn.entity.backend.tables.CallHistory.CALL_HISTORY;
import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.CustomerSource.CUSTOMER_SOURCE;
import static vn.entity.backend.tables.Invoice.INVOICE;
import static vn.entity.backend.tables.Order.ORDER;
import static vn.entity.backend.tables.OrderItem.ORDER_ITEM;
import static vn.entity.backend.tables.Product.PRODUCT;

@Repository
@RequiredArgsConstructor
public class ReportRepository {

    private final DSLContext dsl;

    private Condition customerBranch(Integer branchId) {
        if (CommonUtils.NVL(branchId) <= 0) {
            return noCondition();
        }
        return CUSTOMER.BRANCH_ID.eq(branchId);
    }

    private Condition invoiceBranch(Integer branchId) {
        if (CommonUtils.NVL(branchId) <= 0) {
            return noCondition();
        }
        return INVOICE.BRANCH_ID.eq(branchId);
    }

    private Condition callHistoryBranch(Integer branchId) {
        if (CommonUtils.NVL(branchId) <= 0) {
            return noCondition();
        }
        return CALL_HISTORY.BRANCH_ID.eq(branchId);
    }

    private Condition customerSourceBranch(Integer branchId) {
        if (CommonUtils.NVL(branchId) <= 0) {
            return noCondition();
        }
        return CUSTOMER_SOURCE.BRANCH_ID.eq(branchId);
    }

    /**
     * Báo cáo số khách hàng theo từng tháng trong năm
     */
    public List<CustomerMonthlyReport> countCustomerByMonth(Integer year, Integer branchId) {
        return dsl.select(
                        month(CUSTOMER.CREATED_TIME).as("month"),
                        count().as("totalCustomer")
                )
                .from(CUSTOMER)
                .where(year(CUSTOMER.CREATED_TIME).eq(year))
                        .and(customerBranch(branchId))
                .groupBy(month(CUSTOMER.CREATED_TIME))
                .orderBy(month(CUSTOMER.CREATED_TIME))
                .fetchInto(CustomerMonthlyReport.class);
    }

    /**
     * Báo cáo số khách hàng theo nguồn trong năm
     */
    public List<CustomerSourceReport> countCustomerBySource(Integer year, Integer branchId) {
        return dsl.select(
                        CUSTOMER_SOURCE.ID.as("sourceId"),
                        CUSTOMER_SOURCE.NAME.as("sourceName"),
                        count(CUSTOMER.ID).as("totalCustomer")
                )
                .from(CUSTOMER)
                .leftJoin(CUSTOMER_SOURCE)
                .on(CUSTOMER.SOURCE_ID.eq(CUSTOMER_SOURCE.ID))
                .where(year(CUSTOMER.CREATED_TIME).eq(year)
                        .and(CUSTOMER_SOURCE.STATUS.ne(0))
                        .and(customerBranch(branchId))
                        .and(customerSourceBranch(branchId)))
                .groupBy(CUSTOMER_SOURCE.ID, CUSTOMER_SOURCE.NAME)
                .orderBy(count(CUSTOMER.ID).desc())
                .fetchInto(CustomerSourceReport.class);
    }

    public InvoiceMonthlyRevenueReport getMonthlyRevenue(Integer year, Integer month, Integer branchId) {
        return dsl.select(
                        val(year).as("year"),
                        val(month).as("month"),
                        coalesce(sum(INVOICE.FEE), 0.0).as("totalRevenue")
                )
                .from(INVOICE)
                .where(year(INVOICE.RECEIPT_DATE).eq(year))
                .and(month(INVOICE.RECEIPT_DATE).eq(month))
                .and(INVOICE.STATUS.ne(InvoiceConstant.STATUS_DRAFT))
                .and(invoiceBranch(branchId))
                .fetchOneInto(InvoiceMonthlyRevenueReport.class);
    }

    /**
     * Tần suất mua bán theo khách hàng trong tháng
     */
    public List<InvoiceFrequencyReport> getInvoiceFrequency(
            Integer year,
            Integer month,
            Integer branchId,
            Pageable pageable) {
        var select =
                dsl.select(
                        CUSTOMER.ID.as("customerId"),
                        CUSTOMER.NAME.as("customerName"),
                        count(INVOICE.ID).as("totalInvoice"),
                        coalesce(sum(INVOICE.FEE), 0.0).as("totalFee"),
                        coalesce(avg(INVOICE.FEE), 0.0).as("avgFee")
                )
                .from(INVOICE)
                .join(CUSTOMER).on(CUSTOMER.ID.eq(INVOICE.CUSTOMER_ID))
                .where(year(INVOICE.CREATED_TIME).eq(year))
                .and(month(INVOICE.CREATED_TIME).eq(month))
                .and(INVOICE.STATUS.ne(InvoiceConstant.STATUS_DRAFT))
                .and(invoiceBranch(branchId))
                .groupBy(CUSTOMER.ID, CUSTOMER.NAME)
                        .orderBy(sum(INVOICE.FEE).desc());
        return select
                .offset(pageable.getOffset())
                .limit(pageable.getLimit())
                .fetchInto(InvoiceFrequencyReport.class);
    }

    public Long countInvoiceFrequency(Integer year, Integer month, Integer branchId) {
        return dsl.selectCount()
                .from(
                        dsl.select(CUSTOMER.ID)
                                .from(INVOICE)
                                .join(CUSTOMER).on(CUSTOMER.ID.eq(INVOICE.CUSTOMER_ID))
                                .where(year(INVOICE.RECEIPT_DATE).eq(year))
                                .and(month(INVOICE.RECEIPT_DATE).eq(month))
                                .and(INVOICE.STATUS.ne(InvoiceConstant.STATUS_DRAFT))
                                .and(invoiceBranch(branchId))
                                .groupBy(CUSTOMER.ID, CUSTOMER.NAME)
                )
                .fetchOne(0, Long.class);
    }

    private Condition where(String customerName, Integer branchId) {
        Condition condition = noCondition()
                .and(CALL_HISTORY.STATUS.eq(1))
                .and(callHistoryBranch(branchId));

        if (!StringUtils.isBlank(customerName)) {
            condition = condition.and(
                    lower(CUSTOMER.NAME)
                            .like("%" + customerName.trim().toLowerCase() + "%")
            );
        }
        return condition;
    }

    /**
     * Danh sách trung bình điểm quan tâm theo khách hàng
     */
    public List<CustomerInterestAvgReport> getAvgInterestByCustomer(
            String customerName,
            Integer branchId,
            Pageable pageable) {

        return dsl.select(
                        CUSTOMER.ID.as("customerId"),
                        CUSTOMER.NAME.as("customerName"),
                        count(CALL_HISTORY.ID).as("totalCall"),
                        coalesce(avg(CALL_HISTORY.INTEREST_LEVEL), 0.0)
                                .as("avgInterestLevel")
                )
                .from(CALL_HISTORY)
                .join(CUSTOMER)
                .on(CUSTOMER.ID.eq(CALL_HISTORY.CUSTOMER_ID))
                .where(where(customerName, branchId))
                .groupBy(CUSTOMER.ID, CUSTOMER.NAME)
                .orderBy(avg(CALL_HISTORY.INTEREST_LEVEL).desc())
                .offset(pageable.getOffset())
                .limit(pageable.getLimit())
                .fetchInto(CustomerInterestAvgReport.class);
    }

    public Long countAvgInterestByCustomer(String customerName, Integer branchId) {
        return dsl.selectCount()
                .from(
                        dsl.select(CUSTOMER.ID)
                                .from(CALL_HISTORY)
                                .join(CUSTOMER)
                                .on(CUSTOMER.ID.eq(CALL_HISTORY.CUSTOMER_ID))
                                .where(where(customerName, branchId))
                                .groupBy(CUSTOMER.ID)
                )
                .fetchOne(0, Long.class);
    }

    /**
     * Biểu đồ cột đôi:
     * - Tổng KH có call trong tháng
     * - KH có avg interest > minAvgInterest
     */
    public List<CustomerInterestMonthlyBarReport> getMonthlyInterestBar(
            Integer year,
            Double minAvgInterest,
            Integer branchId) {

        /*
         * Subquery: avg interest theo customer + month
         */
        var sub = dsl.select(
                        month(CALL_HISTORY.CREATED_TIME).as("month"),
                        CALL_HISTORY.CUSTOMER_ID.as("customerId"),
                        avg(CALL_HISTORY.INTEREST_LEVEL).as("avgInterest")
                )
                .from(CALL_HISTORY)
                .where(year(CALL_HISTORY.CREATED_TIME).eq(year))
                .and(CALL_HISTORY.STATUS.eq(1))
                .and(callHistoryBranch(branchId))
                .groupBy(
                        month(CALL_HISTORY.CREATED_TIME),
                        CALL_HISTORY.CUSTOMER_ID
                )
                .asTable("t");

        return dsl.select(
                        field("month", Integer.class),
                        count().as("totalCustomer"),
                        sum(
                                when(field("avgInterest", Double.class)
                                        .gt(minAvgInterest), 1)
                                        .otherwise(0)
                        ).as("highInterestCustomer")
                )
                .from(sub)
                .groupBy(field("month"))
                .orderBy(field("month"))
                .fetchInto(CustomerInterestMonthlyBarReport.class);
    }

    public RevenueSummary getRevenueSummary(LocalDate from, LocalDate to, Integer branchId) {
        long periodDays = to.toEpochDay() - from.toEpochDay() + 1;
        LocalDate previousFrom = from.minusDays(periodDays);
        LocalDateTime previousStart = previousFrom.atStartOfDay();
        LocalDateTime currentStart = from.atStartOfDay();
        LocalDateTime currentEnd = to.plusDays(1).atStartOfDay();
        var effectiveTime = coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME);
        Condition inRange = effectiveTime.ge(previousStart).and(effectiveTime.lt(currentEnd));
        Condition current = effectiveTime.ge(currentStart).and(effectiveTime.lt(currentEnd));
        Condition previous = effectiveTime.ge(previousStart).and(effectiveTime.lt(currentStart));

        RevenueSummary summary = dsl.select(
                        revenue(current, InvoiceConstant.STATUS_DONE).as("totalRevenue"),
                        conditionalCount(current, InvoiceConstant.STATUS_DONE).as("invoiceCount"),
                        revenue(current, InvoiceConstant.STATUS_DONE, INVOICE.DISCOUNT).as("discountTotal"),
                        revenue(current, InvoiceConstant.STATUS_DONE, INVOICE.SHIPPING_FEE).as("shippingTotal"),
                        revenue(current, InvoiceConstant.STATUS_UNPAID).as("unpaidAmount"),
                        conditionalCount(current, InvoiceConstant.STATUS_CANCEL).as("cancelledCount"),
                        revenue(current, InvoiceConstant.STATUS_REFUNDED).as("refundedAmount"),
                        revenue(previous, InvoiceConstant.STATUS_DONE).as("previousTotalRevenue"),
                        conditionalCount(previous, InvoiceConstant.STATUS_DONE).as("previousInvoiceCount"),
                        revenue(previous, InvoiceConstant.STATUS_DONE, INVOICE.DISCOUNT).as("previousDiscountTotal"),
                        revenue(previous, InvoiceConstant.STATUS_DONE, INVOICE.SHIPPING_FEE).as("previousShippingTotal"),
                        revenue(previous, InvoiceConstant.STATUS_UNPAID).as("previousUnpaidAmount"),
                        conditionalCount(previous, InvoiceConstant.STATUS_CANCEL).as("previousCancelledCount"),
                        revenue(previous, InvoiceConstant.STATUS_REFUNDED).as("previousRefundedAmount")
                )
                .from(INVOICE)
                .where(inRange)
                .and(invoiceBranch(branchId))
                .fetchOneInto(RevenueSummary.class);
        return summary == null ? new RevenueSummary() : summary;
    }

    private org.jooq.Field<Double> revenue(Condition period, int status) {
        return revenue(period, status, INVOICE.FEE);
    }

    private org.jooq.Field<Double> revenue(Condition period, int status, org.jooq.Field<Double> amount) {
        return coalesce(sum(when(period.and(INVOICE.STATUS.eq(status)), amount).otherwise(0.0)).cast(Double.class), 0.0);
    }

    private org.jooq.Field<Long> conditionalCount(Condition period, int status) {
        return coalesce(sum(when(period.and(INVOICE.STATUS.eq(status)), 1L).otherwise(0L)).cast(Long.class), 0L);
    }

    public List<RevenueBucket> getRevenueSeries(LocalDate from, LocalDate to, Integer branchId, String groupBy) {
        var effectiveTime = coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME);
        org.jooq.Field<String> bucket = switch (groupBy) {
            case "day" -> field("DATE_FORMAT({0}, '%Y-%m-%d')", String.class, effectiveTime);
            case "week" -> field("YEARWEEK({0}, 3)", Integer.class, effectiveTime).cast(String.class);
            case "month" -> field("DATE_FORMAT({0}, '%Y-%m')", String.class, effectiveTime);
            default -> throw new IllegalArgumentException("Unsupported revenue groupBy: " + groupBy);
        };
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        var rows = dsl.select(
                        bucket.as("bucket"),
                        coalesce(sum(INVOICE.FEE), 0.0).as("revenue"),
                        count(INVOICE.ID).as("invoiceCount")
                )
                .from(INVOICE)
                .where(INVOICE.STATUS.eq(InvoiceConstant.STATUS_DONE))
                .and(effectiveTime.ge(start).and(effectiveTime.lt(end)))
                .and(invoiceBranch(branchId))
                .groupBy(bucket)
                .orderBy(bucket)
                .fetch();
        return rows.map(row -> {
            String key = row.get("bucket", String.class);
            if ("week".equals(groupBy) && key != null && key.length() == 6) {
                key = key.substring(0, 4) + "-W" + key.substring(4);
            }
            return new RevenueBucket(key, row.get("revenue", Double.class), row.get("invoiceCount", Long.class));
        });
    }

    public List<RevenuePaymentMethod> getRevenueByPaymentMethod(LocalDate from, LocalDate to, Integer branchId) {
        var effectiveTime = coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME);
        var method = coalesce(
                INVOICE.PAYMENT_METHOD,
                choose(INVOICE.PAYMENT_TYPE).when(1, "CASH").when(2, "TRANSFER").otherwise((String) null)
        );
        return dsl.select(
                        method.as("method"),
                        coalesce(sum(INVOICE.FEE), 0.0).as("revenue"),
                        count(INVOICE.ID).as("invoiceCount")
                )
                .from(INVOICE)
                .where(INVOICE.STATUS.eq(InvoiceConstant.STATUS_DONE))
                .and(effectiveTime.ge(from.atStartOfDay()).and(effectiveTime.lt(to.plusDays(1).atStartOfDay())))
                .and(invoiceBranch(branchId))
                .groupBy(method)
                .orderBy(method)
                .fetchInto(RevenuePaymentMethod.class);
    }

    public List<TopProductStatistic> getRevenueTopProducts(LocalDate from, LocalDate to, Integer branchId, int limit) {
        var effectiveTime = coalesce(INVOICE.RECEIPT_DATE, INVOICE.CREATED_TIME);
        return dsl.select(
                        PRODUCT.ID,
                        PRODUCT.NAME,
                        coalesce(sum(ORDER_ITEM.QUANTITY), 0).cast(Integer.class),
                        coalesce(sum(ORDER_ITEM.TOTAL_PRICE), 0.0).cast(Double.class)
                )
                .from(INVOICE)
                .join(ORDER).on(ORDER.ID.eq(INVOICE.ORDER_ID))
                .join(ORDER_ITEM).on(ORDER_ITEM.ORDER_ID.eq(ORDER.ID))
                .join(PRODUCT).on(PRODUCT.ID.eq(ORDER_ITEM.PRODUCT_ID))
                .where(INVOICE.STATUS.eq(InvoiceConstant.STATUS_DONE))
                .and(INVOICE.ORDER_ID.isNotNull())
                .and(effectiveTime.ge(from.atStartOfDay()).and(effectiveTime.lt(to.plusDays(1).atStartOfDay())))
                .and(invoiceBranch(branchId))
                .groupBy(PRODUCT.ID, PRODUCT.NAME)
                .orderBy(sum(ORDER_ITEM.TOTAL_PRICE).desc())
                .limit(limit)
                .fetch(row -> new TopProductStatistic(
                        row.value1(), row.value2(), row.value3(), row.value4()));
    }
}
