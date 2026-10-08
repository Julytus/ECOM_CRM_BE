package vn.aeoc.crm.service.sale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.base.util.Tuple5;
import vn.aeoc.crm.constant.InvoiceConstant;
import vn.aeoc.crm.data.report.RevenueHourChart;
import vn.aeoc.crm.data.report.RevenueToday;
import vn.aeoc.crm.data.response.InvoiceResponse;
import vn.aeoc.crm.repository.sale.CustomerVoucherUsageRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.InvoiceVoucherRepository;
import vn.aeoc.crm.service.cache.CacheKeyFactory;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;
import vn.aeoc.entity.data.mysql.sale.Invoice;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InvoiceService extends BaseService<InvoiceRepository, Invoice, Integer> {
    private static final Duration TTL_REVENUE_TODAY = Duration.ofSeconds(45);
    private static final Duration TTL_REVENUE_HOURLY = Duration.ofSeconds(30);

    private final InvoiceVoucherRepository invoiceVoucherRepository;
    private final CustomerVoucherUsageRepository customerVoucherUsageRepository;
    private final QueryExecutor queryExecutor;
    private final BoughtProductService boughtProductService;
    private final BoughtServiceItemService boughtServiceItemService;
    private final ThreeLayerCacheTemplate cacheTemplate;

    private final ConcurrentHashMap<String, Object> recalculateLocks = new ConcurrentHashMap<>();

    protected InvoiceService(InvoiceRepository repository, InvoiceVoucherRepository invoiceVoucherRepository,
                             CustomerVoucherUsageRepository customerVoucherUsageRepository,
                             QueryExecutor queryExecutor,
                             BoughtProductService boughtProductService,
                             BoughtServiceItemService boughtServiceItemService,
                             ThreeLayerCacheTemplate cacheTemplate) {
        super(repository);
        this.invoiceVoucherRepository = invoiceVoucherRepository;
        this.customerVoucherUsageRepository = customerVoucherUsageRepository;
        this.queryExecutor = queryExecutor;
        this.boughtProductService = boughtProductService;
        this.boughtServiceItemService = boughtServiceItemService;
        this.cacheTemplate = cacheTemplate;
    }

    public Page<Invoice> getListByCriteria(String keyword, Integer customerId, List<Integer> status, Integer branchId, Pageable pageable) {
        return pageQuery(
                pageable,
                () -> repository.getInvoiceByCriteria(keyword, customerId, status, branchId, pageable),
                () -> repository.countInvoiceByCriteria(keyword, customerId, status, branchId)
        );
    }

    public RevenueToday getRevenueToday(Integer branchId) {
        long version = cacheTemplate.getLong(CacheKeyFactory.dashboardRevenueVersion(branchId), 0L);
        String key = CacheKeyFactory.revenueToday(branchId, version);
        return cacheTemplate.getOrLoad(key, TTL_REVENUE_TODAY, () -> repository.getRevenueToday(branchId));
    }

    public InvoiceResponse getDetailById(Integer id) {
        var tuple3 = queryExecutor.parallel3(
                () -> repository.getInvoiceById(id),
                () -> boughtProductService.getByInvoiceId(id),
                () -> boughtServiceItemService.getByInvoiceId(id)
        );

        return new InvoiceResponse(tuple3.v1(), tuple3.v2(), tuple3.v3());
    }

    public List<RevenueHourChart> getRevenueByHourToday(Integer branchId) {
        long version = cacheTemplate.getLong(CacheKeyFactory.dashboardRevenueVersion(branchId), 0L);
        String key = CacheKeyFactory.revenueHourToday(branchId, version);
        return cacheTemplate.getOrLoad(key, TTL_REVENUE_HOURLY, () -> {
            List<RevenueHourChart> data = repository.getRevenueByHourToday(branchId);

            Map<Integer, RevenueHourChart> map =
                    data.stream().collect(Collectors.toMap(
                            RevenueHourChart::getHour,
                            Function.identity()
                    ));

            List<RevenueHourChart> result = new ArrayList<>();

            for (int i = 0; i < 24; i++) {

                RevenueHourChart item = map.getOrDefault(i, new RevenueHourChart());

                item.setHour(i);

                if (item.getTotalInvoice() == null) {
                    item.setTotalInvoice(0);
                }
                if (item.getTotalAmount() == null) {
                    item.setTotalAmount(0.0);
                }

                result.add(item);
            }

            return result;
        });
    }

    public Invoice recalculateDraftAmount(Integer invoiceId) {
        return recalculateDraftAmount(invoiceId, null);
    }

    public Invoice recalculateDraftAmount(Integer invoiceId, Long version) {
        String key = CacheKeyFactory.invoiceRecalculate(invoiceId, version);
        Object lock = recalculateLocks.computeIfAbsent(key, k -> new Object());

        synchronized (lock) {
            return cacheTemplate.getOrLoad(key, Duration.ofSeconds(15), () -> {
                Double totalAmount = repository.calculateDraftAmount(invoiceId);
                repository.updateInvoiceAmount(invoiceId, totalAmount);
                return repository.getInvoiceById(invoiceId);
            });
        }
    }

    public Invoice createDraftInvoice(Integer customerId, Integer userId, Integer branchId) {
        return repository.insertDraftInvoice(customerId, userId, branchId);
    }

    public Invoice updateInvoice(Invoice invoice) {
        return repository.updateDraftInvoice(invoice);
    }

    public Integer updateStatus(Integer invoiceId, Integer status) {
        Invoice invoice = repository.getById(invoiceId);
        Integer updated = repository.updateStatus(invoiceId, status);
        if (updated > 0 && invoice != null) {
            bumpDashboardRevenueVersion(invoice.getBranchId());
        }
        return updated;
    }

    @Transactional
    public Invoice create(Invoice invoice) {

        Tuple5<Invoice, Integer, Integer, Integer, Integer> result = queryExecutor.parallel5(
                () -> repository.update(invoice.getId(), invoice),
                () -> boughtProductService.updateDoneStatus(invoice.getId()),
                () -> boughtServiceItemService.updateDoneStatus(invoice.getId()),
                () -> invoiceVoucherRepository.insert(invoice.getId(), invoice.getVoucherId(), invoice.getDiscount()),
                () -> customerVoucherUsageRepository.increaseUsage(invoice.getVoucherId(), invoice.getCustomerId()));

//        bumpDashboardRevenueVersion(invoice.getBranchId());
        return result.v1();
    }

    // Xóa cứng, chỉ dùng khi hóa đơn đang ở trạng thái nháp
    public Integer delete(Integer id) {
        Invoice invoice = repository.getById(id);
        if (invoice.getStatus() != InvoiceConstant.STATUS_DRAFT) {
            throw new AppException("Chỉ có thể xóa hóa đơn nháp", ErrorCode.BAD_REQUEST.code);
        }
        return repository.delete(id);
    }

    public Integer cancelInvoice(Integer id) {
        Invoice invoice = repository.getById(id);
        Integer updated = repository.updateStatus(id, InvoiceConstant.STATUS_CANCEL);
        if (updated > 0 && invoice != null) {
            bumpDashboardRevenueVersion(invoice.getBranchId());
        }
        return updated;
    }

    private void bumpDashboardRevenueVersion(Integer branchId) {
        cacheTemplate.increment(CacheKeyFactory.dashboardRevenueVersion(branchId));
    }
}
