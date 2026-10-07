package vn.aeoc.crm.repository.sale;

import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;
import org.springframework.stereotype.Repository;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.repo.AbsMysqlRepository;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.crm.data.pojo.Purchase;
import vn.aeoc.crm.util.BranchFilter;
import vn.aeoc.entity.data.mysql.BoughtService;
import vn.entity.backend.tables.records.BoughtServiceRecord;

import java.util.*;
import java.util.stream.Collectors;

import static vn.entity.backend.tables.BoughtService.BOUGHT_SERVICE;
import static vn.entity.backend.tables.CategoryItem.CATEGORY_ITEM;
import static vn.entity.backend.tables.Invoice.INVOICE;
import static vn.entity.backend.tables.Service.SERVICE;

@Repository
public class BoughtServiceItemRepository extends AbsMysqlRepository<BoughtServiceRecord, BoughtService, Integer> {

    @Override
    protected TableImpl<BoughtServiceRecord> getTable() {
        return BOUGHT_SERVICE;
    }

    /* ==============================
     * Condition
     * ============================== */

    private Condition getWhereCondition(Integer invoiceId, Integer status, Integer branchId) {
        Condition condition = DSL.trueCondition();

        condition = condition.and(BranchFilter.branchEq(BOUGHT_SERVICE.BRANCH_ID, CommonUtils.NVL(branchId)));

        if (invoiceId != null && invoiceId > 0) {
            condition = condition.and(BOUGHT_SERVICE.INVOICE_ID.eq(invoiceId));
        }

        if (status != null && status > 0) {
            condition = condition.and(BOUGHT_SERVICE.STATUS.eq(status));
        }

        return condition;
    }

    /* ==============================
     * Page Query
     * ============================== */

    public List<BoughtService> getByCriteria(
            Integer invoiceId,
            Integer status,
            Integer branchId,
            Pageable pageable
    ) {
        return getListByCriteria(getWhereCondition(invoiceId, status, branchId), BOUGHT_SERVICE.ID.desc(), pageable);
    }

    public Long countByCriteria(Integer invoiceId, Integer status, Integer branchId) {
        return getCountWithCriteria(getWhereCondition(invoiceId, status, branchId));
    }

    public Integer updateStatusByInvoiceId(Integer invoiceId, Integer status) {
        return updateFieldByCondition(BOUGHT_SERVICE.INVOICE_ID.eq(invoiceId), BOUGHT_SERVICE.STATUS, status);
    }

    public List<BoughtService>
    upsertBatch(List<BoughtService> services) {

        if (services == null || services.isEmpty()) {
            return List.of();
        }

        // 1. Tách insert / update
        List<BoughtService> inserts = services.stream()
                .filter(p -> p.getId() == null || p.getId() <= 0)
                .toList();

        List<BoughtService> updates = services.stream()
                .filter(p -> p.getId() != null && p.getId() > 0)
                .toList();

        List<BoughtService> result = new ArrayList<>();

        // ==============================
        // INSERT
        // ==============================
        if (!inserts.isEmpty()) {

            // insert batch
            super.insertBatchReturning(inserts);

            // ⚠️ query lại theo invoiceId + điều kiện đặc trưng
            Set<Integer> invoiceIds = inserts.stream()
                    .map(BoughtService::getInvoiceId)
                    .collect(Collectors.toSet());

            List<BoughtService> inserted = getListByCriteria(
                    BOUGHT_SERVICE.INVOICE_ID.in(invoiceIds)
            );

            // ⚠️ filter lại đúng record vừa insert (tránh lấy cả data cũ)
            Set<String> uniqueKeys = inserts.stream()
                    .map(p -> p.getServiceId() + "_" + p.getInvoiceId())
                    .collect(Collectors.toSet());

            inserted = inserted.stream()
                    .filter(p -> uniqueKeys.contains(p.getServiceId() + "_" + p.getInvoiceId()))
                    .toList();

            result.addAll(inserted);
        }

        // ==============================
        // UPDATE
        // ==============================
        if (!updates.isEmpty()) {

            super.updateBatchReturning(updates);

            List<Integer> ids = updates.stream()
                    .map(BoughtService::getId)
                    .toList();

            List<BoughtService> updated = getListByIds(ids);

            result.addAll(updated);
        }

        // ==============================
        // REMOVE DUPLICATE (IMPORTANT)
        // ==============================
        Map<Integer, BoughtService> map = new LinkedHashMap<>();

        for (BoughtService p : result) {
            map.put(p.getId(), p); // key = id → auto remove duplicate
        }

        return new ArrayList<>(map.values());
    }

    public List<BoughtService> getByInvoiceId(Integer invoiceId) {
        return getListByCriteria(BOUGHT_SERVICE.INVOICE_ID.eq(invoiceId), BOUGHT_SERVICE.ID.desc());
    }

    public List<String> getTopServiceCategories(Integer customerId, int limit) {

        return dslContext.select(CATEGORY_ITEM.NAME)
                .from(BOUGHT_SERVICE)
                .join(SERVICE).on(SERVICE.ID.eq(BOUGHT_SERVICE.SERVICE_ID))
                .join(CATEGORY_ITEM).on(CATEGORY_ITEM.ID.eq(SERVICE.CATEGORY_ID))
                .join(INVOICE).on(INVOICE.ID.eq(BOUGHT_SERVICE.INVOICE_ID))
                .where(BOUGHT_SERVICE.CUSTOMER_ID.eq(customerId))
                .and(INVOICE.STATUS.eq(1)) // DONE
                .groupBy(CATEGORY_ITEM.ID, CATEGORY_ITEM.NAME)
                .orderBy(DSL.count().desc())
                .limit(limit)
                .fetchInto(String.class);
    }

    public List<Purchase> getRecentServices(Integer customerId, int limit) {

        return dslContext.select(
                        SERVICE.NAME.as("name"),
                        BOUGHT_SERVICE.FEE.as("value")
                )
                .from(BOUGHT_SERVICE)
                .join(SERVICE).on(SERVICE.ID.eq(BOUGHT_SERVICE.SERVICE_ID))
                .join(INVOICE).on(INVOICE.ID.eq(BOUGHT_SERVICE.INVOICE_ID))
                .where(BOUGHT_SERVICE.CUSTOMER_ID.eq(customerId))
                .and(INVOICE.STATUS.eq(1)) // DONE
                .orderBy(INVOICE.CREATED_TIME.desc())
                .limit(limit)
                .fetchInto(Purchase.class);
    }
}
