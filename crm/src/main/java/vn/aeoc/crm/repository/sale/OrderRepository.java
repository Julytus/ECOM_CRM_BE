package vn.aeoc.crm.repository.sale;

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
import vn.entity.backend.tables.records.OrderRecord;

import java.util.List;

import static vn.entity.backend.tables.Order.ORDER;

@Repository
public class OrderRepository
        extends AbsMysqlRepository<OrderRecord, Order, Integer> {

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
}