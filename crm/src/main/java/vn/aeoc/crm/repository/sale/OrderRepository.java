package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
//import vn.aeoc.crm.data.constant.OrderStatus;
import vn.aeoc.crm.data.constant.PaymentStatus;
import vn.aeoc.entity.data.mysql.sale.Order;
import vn.aeoc.crm.data.response.OrderResponse;
import vn.entity.backend.tables.records.OrderRecord;

import java.util.List;

import static vn.entity.backend.tables.Order.ORDER;
import static vn.entity.backend.tables.Invoice.INVOICE;

@Repository
public class OrderRepository
        extends AbsMysqlRepository<OrderRecord, Order, Integer> {
    public OrderRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<OrderRecord> getTable() {
        return ORDER;
    }

    private Condition getWhereCondition(
            String keyword,
            Integer userId,
            String orderStatus,
            String paymentStatus,
            String status
    ) {
        Condition condition = DSL.trueCondition();

        if (!StringUtils.isBlank(keyword)) {
            String kw = "%" + keyword.trim().toLowerCase() + "%";
            condition = condition.and(DSL.lower(ORDER.CODE).like(kw));
        }
        if (userId != null && userId > 0) {
            condition = condition.and(ORDER.USER_ID.eq(userId));
        }
//        if (!StringUtils.isBlank(orderStatus)) {
//            condition = condition.and(ORDER.ORDER_STATUS.eq(orderStatus));
//        }
        if (!StringUtils.isBlank(paymentStatus)) {
            condition = condition.and(ORDER.PAYMENT_STATUS.eq(paymentStatus));
        }
        if (!StringUtils.isBlank(status)) {
            condition = condition.and(ORDER.STATUS.eq(status));
        }

        return condition;
    }

    public List<Order> getByCriteria(
            String keyword,
            Integer userId,
            String orderStatus,
            String paymentStatus,
            String status,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(
                keyword, userId, orderStatus, paymentStatus, status
        );
        return getListByCriteriaWithJoin(
                List.of(), ORDER, condition, ORDER.ID.desc(), pageable
        );
    }

    public Long countByCriteria(
            String keyword,
            Integer userId,
            String orderStatus,
            String paymentStatus,
            String status
    ) {
        return getCountWithCriteria(
                getWhereCondition(keyword, userId, orderStatus, paymentStatus, status)
        );
    }

    public void updateStatus(Integer id, Order o) {
        updateIgnoreNull(id, o);
    }

    public int updateStatusAndPayment(Integer orderId, String status, String paymentStatus) {
        return dslContext.update(ORDER)
                .set(ORDER.STATUS, status)
                .set(ORDER.PAYMENT_STATUS, paymentStatus)
                .set(ORDER.UPDATED_AT, java.time.LocalDateTime.now())
                .where(ORDER.ID.eq(orderId))
                .execute();
    }

    public List<OrderResponse> findByUserId(Integer userId, String orderStatus, Pageable pageable) {
        Condition condition = ORDER.USER_ID.eq(userId);
        if (!StringUtils.isBlank(orderStatus)) condition = condition.and(ORDER.STATUS.eq(orderStatus));
        return dslContext.select(ORDER.fields())
                .select(INVOICE.ID.as("invoiceId"), INVOICE.INVOICE_CODE.as("invoiceCode"),
                        INVOICE.STATUS.as("invoiceStatus"))
                .from(ORDER.leftJoin(INVOICE).on(INVOICE.ORDER_ID.eq(ORDER.ID)))
                .where(condition).orderBy(ORDER.ID.desc())
                .limit(pageable.getLimit()).offset(pageable.getOffset())
                .fetchInto(OrderResponse.class);
    }

    public Long countByUserId(Integer userId, String orderStatus) {
        Condition condition = ORDER.USER_ID.eq(userId);
        if (!StringUtils.isBlank(orderStatus)) condition = condition.and(ORDER.STATUS.eq(orderStatus));
        return dslContext.selectCount().from(ORDER).where(condition).fetchOne(0, Long.class);
    }

    public Order createNewOrder(Order o) {
        o.setCode(CommonUtils.nextCode("ORD"));

        o.setTotalAmount(
                o.getTotalAmount() == null ? 0.0 : o.getTotalAmount()
        );

        o.setDiscountAmount(
                o.getDiscountAmount() == null ? 0.0 : o.getDiscountAmount()
        );

        o.setFinalAmount(
                o.getFinalAmount() == null ? 0.0 : o.getFinalAmount()
        );

        o.setStatus(
                o.getStatus() == null ? "NEW" : o.getStatus()
        );

        o.setPaymentStatus(PaymentStatus.UNPAID);

        return insertReturning(o);
    }

    public Order insertCheckoutOrder(Order order, String address) {
        order.setCode(CommonUtils.nextCode("ORD"));
        order.setAddress(address);
        order.setStatus("NEW");
        order.setPaymentStatus(PaymentStatus.UNPAID);
        return dslContext.insertInto(ORDER)
                .set(ORDER.CODE, order.getCode())
                .set(ORDER.USER_ID, order.getUserId())
                .set(ORDER.TOTAL_AMOUNT, order.getTotalAmount())
                .set(ORDER.DISCOUNT_AMOUNT, order.getDiscountAmount())
                .set(ORDER.FINAL_AMOUNT, order.getFinalAmount())
                .set(ORDER.STATUS, order.getStatus())
                .set(ORDER.PAYMENT_STATUS, order.getPaymentStatus())
                .set(ORDER.PAYMENT_METHOD, order.getPaymentMethod())
                .set(ORDER.NOTE, order.getNote())
                .set(ORDER.ADDRESS, order.getAddress())
                .set(ORDER.BUYER_NAME, order.getBuyerName())
                .set(ORDER.BUYER_PHONE, order.getBuyerPhone())
                .set(ORDER.BUYER_EMAIL, order.getBuyerEmail())
                .set(ORDER.RECEIVER_NAME, order.getReceiverName())
                .set(ORDER.RECEIVER_PHONE, order.getReceiverPhone())
                .set(ORDER.SHIPPING_FEE, order.getShippingFee())
                .set(ORDER.VOUCHER_ID, order.getVoucherId())
                .set(ORDER.VOUCHER_CODE, order.getVoucherCode())
                .set(ORDER.BRANCH_ID, order.getBranchId())
                .set(ORDER.CUSTOMER_ID, order.getCustomerId())
                .returning()
                .fetchOneInto(Order.class);
    }
}
