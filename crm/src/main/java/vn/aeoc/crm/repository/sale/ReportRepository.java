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

import java.util.List;

import static org.jooq.impl.DSL.*;
import static vn.entity.backend.tables.CallHistory.CALL_HISTORY;
import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.CustomerSource.CUSTOMER_SOURCE;
import static vn.entity.backend.tables.Invoice.INVOICE;

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
}