package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.entity.data.mysql.sale.OrderItem;
import vn.entity.backend.tables.records.OrderItemRecord;

import java.util.List;

import static vn.entity.backend.tables.OrderItem.ORDER_ITEM;
import static vn.entity.backend.tables.Product.PRODUCT;
import static vn.entity.backend.tables.ProductVariant.PRODUCT_VARIANT;

@Repository
public class OrderItemRepository
        extends AbsMysqlRepository<OrderItemRecord, OrderItem, Integer> {
    public OrderItemRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<OrderItemRecord> getTable() {
        return ORDER_ITEM;
    }

    private Condition getWhereCondition(Integer orderId) {
        Condition condition = DSL.trueCondition();
        if (orderId != null && orderId > 0) {
            condition = condition.and(ORDER_ITEM.ORDER_ID.eq(orderId));
        }
        return condition;
    }

    public List<OrderItem> getByCriteria(Integer orderId, Pageable pageable) {
        return getListByCriteriaWithJoin(
                List.of(),
                ORDER_ITEM,
                getWhereCondition(orderId),
                ORDER_ITEM.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(Integer orderId) {
        return getCountWithCriteria(getWhereCondition(orderId));
    }

    public List<OrderItem> fetchOrderItemDetail(Integer orderId) {

        return dslContext.select(
                        ORDER_ITEM.ID,

                        PRODUCT.ID.as("product_id"),
                        PRODUCT.NAME.as("product_name"),

                        PRODUCT_VARIANT.ID.as("product_variant_id"),
                        PRODUCT_VARIANT.SKU.as("sku"),
                        PRODUCT_VARIANT.IMAGE.as("product_variant_image"),

                        ORDER_ITEM.PRICE,
                        ORDER_ITEM.QUANTITY,
                        ORDER_ITEM.TOTAL_PRICE
                )
                .from(ORDER_ITEM)
                .join(PRODUCT_VARIANT)
                .on(PRODUCT_VARIANT.ID.eq(ORDER_ITEM.PRODUCT_VARIANT_ID))
                .join(PRODUCT)
                .on(PRODUCT.ID.eq(PRODUCT_VARIANT.PRODUCT_ID))
                .where(ORDER_ITEM.ORDER_ID.eq(orderId))
                .orderBy(ORDER_ITEM.ID.asc())
                .fetchInto(OrderItem.class);
    }

    public void insertMany(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        dslContext.batch(
                items.stream()
                        .map(item -> dslContext.insertInto(ORDER_ITEM)
                                .set(ORDER_ITEM.ORDER_ID, item.getOrderId())
                                .set(ORDER_ITEM.PRODUCT_ID, item.getProductId())
                                .set(ORDER_ITEM.PRODUCT_VARIANT_ID, item.getProductVariantId())
                                .set(ORDER_ITEM.PRICE, item.getPrice())
                                .set(ORDER_ITEM.QUANTITY, item.getQuantity())
                                .set(ORDER_ITEM.TOTAL_PRICE, item.getTotalPrice())
                        )
                        .toList()
        ).execute();
    }
}