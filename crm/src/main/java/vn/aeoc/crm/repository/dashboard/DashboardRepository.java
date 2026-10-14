package vn.aeoc.crm.repository.dashboard;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import vn.aeoc.crm.data.report.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static vn.entity.backend.tables.Order.ORDER;
import static vn.entity.backend.tables.OrderItem.ORDER_ITEM;
import static vn.entity.backend.tables.Product.PRODUCT;
import static vn.entity.backend.tables.ProductVariant.PRODUCT_VARIANT;

@Repository
public class DashboardRepository {

    private final DSLContext dslContext;

    public DashboardRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public List<RevenueStatistic> getRevenue(
            LocalDate fromDate,
            LocalDate toDate,
            String groupBy
    ) {
        String dateFormat = switch (groupBy) {
            case "DAY" -> "%Y-%m-%d";
            case "MONTH" -> "%Y-%m";
            case "YEAR" -> "%Y";
            default -> throw new IllegalArgumentException(
                    "Invalid groupBy: " + groupBy
            );
        };

        Field<String> periodField = DSL.field(
                "DATE_FORMAT({0}, '" + dateFormat + "')",
                String.class,
                ORDER.CREATED_AT
        );

        Field<Integer> totalOrdersField =
                DSL.count().as("total_orders");

        Field<BigDecimal> revenueField =
                DSL.sum(ORDER.FINAL_AMOUNT).as("revenue");

        return dslContext
                .select(periodField, totalOrdersField, revenueField)
                .from(ORDER)
                .where(ORDER.PAYMENT_STATUS.eq("PAID"))
                .and(ORDER.CREATED_AT.ge(fromDate.atStartOfDay()))
                .and(ORDER.CREATED_AT.lt(toDate.atStartOfDay()))
                .groupBy(periodField)
                .orderBy(periodField.asc())
                .fetch(record -> new RevenueStatistic(
                        record.get(periodField),
                        record.get(totalOrdersField),
                        record.get(revenueField)
                ));
    }

    private Field<String> periodField(String groupBy) {
        String format = switch (groupBy) {
            case "DAY" -> "%Y-%m-%d";
            case "MONTH" -> "%Y-%m";
            case "YEAR" -> "%Y";
            default -> throw new IllegalArgumentException("Invalid groupBy");
        };
        return DSL.field(
                "DATE_FORMAT({0}, '" + format + "')",
                String.class, ORDER.CREATED_AT
        );
    }

    public List<OrderStatusStatistic> getOrderStatus(LocalDate from, LocalDate to) {
        Field<String> status = ORDER.STATUS.as("status");
        Field<Integer> count = DSL.count().cast(Integer.class).as("total_orders");

        return dslContext.select(status, count).from(ORDER)
                .where(ORDER.CREATED_AT.ge(from.atStartOfDay()))
                .and(ORDER.CREATED_AT.lt(to.atStartOfDay()))
                .groupBy(ORDER.STATUS).orderBy(count.desc())
                .fetch(r -> new OrderStatusStatistic(r.get(status), r.get(count), 0.0));
    }

    // API 3: top sản phẩm, gộp các biến thể theo product_id
    public List<TopProductStatistic> getTopProducts(
            LocalDate from, LocalDate to, int limit, String sortBy) {
        Field<Integer> productId = PRODUCT.ID.as("product_id");
        Field<String> productName = PRODUCT.NAME.as("product_name");
        Field<Integer> quantity = DSL.sum(ORDER_ITEM.QUANTITY)
                .cast(Integer.class).as("total_quantity");
        Field<Double> revenue = DSL.sum(ORDER_ITEM.TOTAL_PRICE)
                .cast(Double.class).as("revenue");

        var query = dslContext.select(productId, productName, quantity, revenue)
                .from(ORDER_ITEM)
                .join(ORDER).on(ORDER.ID.eq(ORDER_ITEM.ORDER_ID))
                .join(PRODUCT).on(PRODUCT.ID.eq(ORDER_ITEM.PRODUCT_ID))
                .where(ORDER.PAYMENT_STATUS.eq("PAID"))
                .and(ORDER.CREATED_AT.ge(from.atStartOfDay()))
                .and(ORDER.CREATED_AT.lt(to.atStartOfDay()))
                .groupBy(PRODUCT.ID, PRODUCT.NAME);

        if ("REVENUE".equals(sortBy)) {
            return query.orderBy(revenue.desc()).limit(limit)
                    .fetch(r -> new TopProductStatistic(
                            r.get(productId), r.get(productName),
                            r.get(quantity) == null ? 0 : r.get(quantity),
                            r.get(revenue) == null ? 0.0 : r.get(revenue)));
        }

        return query.orderBy(quantity.desc()).limit(limit)
                .fetch(r -> new TopProductStatistic(
                        r.get(productId), r.get(productName),
                        r.get(quantity) == null ? 0 : r.get(quantity),
                        r.get(revenue) == null ? 0.0 : r.get(revenue)));
    }

    // API 4: AOV theo kỳ
    public List<AovStatistic> getAov(LocalDate from, LocalDate to, String groupBy) {
        Field<String> period = periodField(groupBy);
        Field<Integer> count = DSL.count().cast(Integer.class);
        Field<Double> avg = DSL.avg(ORDER.FINAL_AMOUNT).cast(Double.class);

        return dslContext.select(period, count, avg).from(ORDER)
                .where(ORDER.PAYMENT_STATUS.eq("PAID"))
                .and(ORDER.CREATED_AT.ge(from.atStartOfDay()))
                .and(ORDER.CREATED_AT.lt(to.atStartOfDay()))
                .groupBy(period).orderBy(period.asc())
                .fetch(r -> new AovStatistic(
                        r.get(period), r.get(count),
                        r.get(avg) == null ? 0.0 : r.get(avg)));
    }

    public record InventoryCounts(
            Integer totalVariants, Integer lowStockVariants, Integer outOfStockVariants) {}

    // API 5: tổng quan tồn kho
    public InventoryCounts getInventoryCounts(int threshold) {
        Field<Integer> total = DSL.count().cast(Integer.class).as("total_variants");
        Field<Integer> low = DSL.sum(DSL.when(PRODUCT_VARIANT.STOCK.le(threshold), 1)
                .otherwise(0)).cast(Integer.class).as("low_stock");
        Field<Integer> out = DSL.sum(DSL.when(PRODUCT_VARIANT.STOCK.le(0), 1)
                .otherwise(0)).cast(Integer.class).as("out_of_stock");

        return dslContext.select(total, low, out).from(PRODUCT_VARIANT)
                .fetchOne(r -> new InventoryCounts(
                        r.get(total) == null ? 0 : r.get(total),
                        r.get(low) == null ? 0 : r.get(low),
                        r.get(out) == null ? 0 : r.get(out)));
    }

    public List<InventoryItemStatistic> getLowStockItems(int threshold, int limit) {
        Field<Integer> variantId = PRODUCT_VARIANT.ID.as("variant_id");
        Field<Integer> productId = PRODUCT_VARIANT.PRODUCT_ID.as("product_id");
        Field<String> productName = PRODUCT.NAME.as("product_name");
        Field<String> sku = PRODUCT_VARIANT.SKU.as("sku");
        Field<Integer> stock = PRODUCT_VARIANT.STOCK.as("stock");
        Field<Double> price = PRODUCT_VARIANT.PRICE.cast(Double.class).as("price");

        return dslContext.select(variantId, productId, productName, sku, stock, price)
                .from(PRODUCT_VARIANT)
                .join(PRODUCT).on(PRODUCT.ID.eq(PRODUCT_VARIANT.PRODUCT_ID))
                .where(PRODUCT_VARIANT.STOCK.le(threshold))
                .orderBy(PRODUCT_VARIANT.STOCK.asc(), PRODUCT_VARIANT.ID.asc())
                .limit(limit)
                .fetch(r -> new InventoryItemStatistic(
                        r.get(variantId), r.get(productId), r.get(productName),
                        r.get(sku), r.get(stock), r.get(price)));
    }
}
