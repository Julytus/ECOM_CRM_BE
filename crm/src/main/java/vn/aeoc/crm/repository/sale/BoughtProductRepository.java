package vn.aeoc.crm.repository.sale;

import org.jooq.DSLContext;
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
import vn.aeoc.crm.data.pojo.Purchase;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.sale.BoughtProduct;
import vn.entity.backend.tables.records.BoughtProductRecord;

import java.util.*;
import java.util.stream.Collectors;

import static vn.entity.backend.tables.BoughtProduct.BOUGHT_PRODUCT;
import static vn.entity.backend.tables.CategoryItem.CATEGORY_ITEM;
import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.Invoice.INVOICE;
import static vn.entity.backend.tables.ProductV1.PRODUCT_V1;
import static vn.entity.backend.tables.Unit.UNIT;

@Repository
public class BoughtProductRepository
        extends AbsMysqlRepository<BoughtProductRecord, BoughtProduct, Integer> {
    public BoughtProductRepository(DSLContext dslContext) {
        super(dslContext);
    }


    @Override
    protected TableImpl<BoughtProductRecord> getTable() {
        return BOUGHT_PRODUCT;
    }



    private Condition getWhereCondition(Integer invoiceId, Integer status, Integer branchId) {

        Condition condition = DSL.trueCondition();

        condition = condition.and(BranchFilter.branchEq(BOUGHT_PRODUCT.BRANCH_ID, CommonUtils.NVL(branchId)));

        if (invoiceId != null && invoiceId > 0) {
            condition = condition.and(BOUGHT_PRODUCT.INVOICE_ID.eq(invoiceId));
        }

        if (status != null && status > 0) {
            condition = condition.and(BOUGHT_PRODUCT.STATUS.eq(status));
        }

        return condition;
    }

    private List<Field<?>> getJoinFields() {
        return mergeFields(
                PRODUCT_V1.NAME.as("productName"),
                CUSTOMER.NAME.as("customerName"),
                UNIT.NAME.as("unitName")
        );
    }

    private Table<?> getJoinTable() {
        return BOUGHT_PRODUCT
                .leftJoin(PRODUCT_V1).on(PRODUCT_V1.ID.eq(BOUGHT_PRODUCT.PRODUCT_ID))
                .leftJoin(CUSTOMER).on(CUSTOMER.ID.eq(BOUGHT_PRODUCT.CUSTOMER_ID))
                .leftJoin(UNIT).on(UNIT.ID.eq(BOUGHT_PRODUCT.UNIT_ID));
    }

    public List<BoughtProduct> getByCriteria(
            Integer invoiceId,
            Integer status,
            Integer branchId,
            Pageable pageable
    ) {
        Condition condition = getWhereCondition(invoiceId, status, branchId);

        /* JOIN TABLE */

        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                condition,
                BOUGHT_PRODUCT.ID.desc(),
                pageable
        );
    }

    public Long countByCriteria(Integer invoiceId, Integer status, Integer branchId) {

        return getCountWithCriteria(
                getWhereCondition(invoiceId, status, branchId)
        );
    }

    public BoughtProduct getById(Integer id) {
        return getOneByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                BOUGHT_PRODUCT.ID.eq(id)
        );
    }

    public Integer updateStatusByInvoiceId(Integer invoiceId, Integer status) {
        return updateFieldByCondition(BOUGHT_PRODUCT.INVOICE_ID.eq(invoiceId), BOUGHT_PRODUCT.STATUS, status);
    }

    public List<BoughtProduct> getByInvoiceId(Integer invoiceId) {
        return getListByCriteriaWithJoin(
                getJoinFields(),
                getJoinTable(),
                BOUGHT_PRODUCT.INVOICE_ID.eq(invoiceId),
                BOUGHT_PRODUCT.ID.desc()
        );
    }

    public List<BoughtProduct> upsertBatch(List<BoughtProduct> products) {

        if (products == null || products.isEmpty()) {
            return List.of();
        }

        // 1. Tách insert / update
        List<BoughtProduct> inserts = products.stream()
                .filter(p -> p.getId() == null || p.getId() <= 0)
                .toList();

        List<BoughtProduct> updates = products.stream()
                .filter(p -> p.getId() != null && p.getId() > 0)
                .toList();

        List<BoughtProduct> result = new ArrayList<>();

        // ==============================
        // INSERT
        // ==============================
        if (!inserts.isEmpty()) {

            // insert batch
            super.insertBatchReturning(inserts);

            // ⚠️ query lại theo invoiceId + điều kiện đặc trưng
            Set<Integer> invoiceIds = inserts.stream()
                    .map(BoughtProduct::getInvoiceId)
                    .collect(Collectors.toSet());

            List<BoughtProduct> inserted = getListByCriteria(
                    BOUGHT_PRODUCT.INVOICE_ID.in(invoiceIds)
            );

            // ⚠️ filter lại đúng record vừa insert (tránh lấy cả data cũ)
            Set<String> uniqueKeys = inserts.stream()
                    .map(p -> p.getProductId() + "_" + p.getInvoiceId())
                    .collect(Collectors.toSet());

            inserted = inserted.stream()
                    .filter(p -> uniqueKeys.contains(p.getProductId() + "_" + p.getInvoiceId()))
                    .toList();

            result.addAll(inserted);
        }

        // ==============================
        // UPDATE
        // ==============================
        if (!updates.isEmpty()) {

            super.updateBatchReturning(updates);

            List<Integer> ids = updates.stream()
                    .map(BoughtProduct::getId)
                    .toList();

            List<BoughtProduct> updated = getListByIds(ids);

            result.addAll(updated);
        }

        // ==============================
        // REMOVE DUPLICATE (IMPORTANT)
        // ==============================
        Map<Integer, BoughtProduct> map = new LinkedHashMap<>();

        for (BoughtProduct p : result) {
            map.put(p.getId(), p); // key = id → auto remove duplicate
        }

        return new ArrayList<>(map.values());
    }

    public List<String> getTopProductCategories(Integer customerId, int limit) {

        return dslContext.select(CATEGORY_ITEM.NAME)
                .from(BOUGHT_PRODUCT)
                .join(PRODUCT_V1).on(PRODUCT_V1.ID.eq(BOUGHT_PRODUCT.PRODUCT_ID))
                .join(CATEGORY_ITEM).on(CATEGORY_ITEM.ID.eq(PRODUCT_V1.CATEGORY_ID))
                .where(BOUGHT_PRODUCT.CUSTOMER_ID.eq(customerId))
                .and(BOUGHT_PRODUCT.STATUS.gt(0))
                .groupBy(CATEGORY_ITEM.ID, CATEGORY_ITEM.NAME)
                .orderBy(DSL.count().desc())
                .limit(limit)
                .fetchInto(String.class);
    }

    public List<Purchase> getRecentProducts(Integer customerId, int limit) {

        return dslContext.select(
                        PRODUCT_V1.NAME.as("name"),
                        BOUGHT_PRODUCT.PRICE.mul(BOUGHT_PRODUCT.QTY)
                                .minus(BOUGHT_PRODUCT.DISCOUNT)
                                .as("value")
                )
                .from(BOUGHT_PRODUCT)
                .join(PRODUCT_V1).on(PRODUCT_V1.ID.eq(BOUGHT_PRODUCT.PRODUCT_ID))
                .join(INVOICE).on(INVOICE.ID.eq(BOUGHT_PRODUCT.INVOICE_ID))
                .where(BOUGHT_PRODUCT.CUSTOMER_ID.eq(customerId))
                .and(INVOICE.STATUS.eq(InvoiceConstant.STATUS_DONE))
                .orderBy(INVOICE.CREATED_TIME.desc())
                .limit(limit)
                .fetchInto(Purchase.class);
    }
}