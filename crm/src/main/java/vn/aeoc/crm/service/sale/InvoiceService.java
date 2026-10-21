package vn.aeoc.crm.service.sale;

import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
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
import vn.aeoc.crm.data.constant.OrderStatus;
import vn.aeoc.crm.data.constant.PaymentStatus;
import vn.aeoc.crm.event.InvoicePaidEvent;
import vn.aeoc.crm.repository.sale.CustomerVoucherUsageRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;
import vn.aeoc.crm.repository.sale.InvoiceVoucherRepository;
import vn.aeoc.crm.repository.sale.OrderRepository;
import vn.aeoc.crm.repository.sale.PaymentRepository;
import vn.aeoc.crm.service.cache.CacheKeyFactory;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Order;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static vn.entity.backend.tables.Invoice.INVOICE;

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
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final ConcurrentHashMap<String, Object> recalculateLocks = new ConcurrentHashMap<>();

    protected InvoiceService(InvoiceRepository repository, InvoiceVoucherRepository invoiceVoucherRepository,
                             CustomerVoucherUsageRepository customerVoucherUsageRepository,
                             QueryExecutor queryExecutor,
                             BoughtProductService boughtProductService,
                             BoughtServiceItemService boughtServiceItemService,
                             ThreeLayerCacheTemplate cacheTemplate,
                             OrderRepository orderRepository,
                             PaymentRepository paymentRepository,
                             ApplicationEventPublisher eventPublisher) {
        super(repository, queryExecutor);
        this.invoiceVoucherRepository = invoiceVoucherRepository;
        this.customerVoucherUsageRepository = customerVoucherUsageRepository;
        this.queryExecutor = queryExecutor;
        this.boughtProductService = boughtProductService;
        this.boughtServiceItemService = boughtServiceItemService;
        this.cacheTemplate = cacheTemplate;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
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

    @Transactional
    public Integer updateStatus(Integer invoiceId, Integer status) {
        Invoice invoice = repository.getById(invoiceId);
        if (invoice == null) {
            throw new AppException("Không tìm thấy hóa đơn", ErrorCode.BAD_REQUEST.code);
        }
        if (invoice.getOrderId() != null) {
            if (status == InvoiceConstant.STATUS_DONE) return markPaid(invoiceId, "MANUAL");
            if (status == InvoiceConstant.STATUS_CANCEL) return cancelOrderInvoice(invoiceId);
            if (status == InvoiceConstant.STATUS_REFUNDED) return refund(invoiceId, null);
            throw new AppException("Chuyển trạng thái hóa đơn không hợp lệ", ErrorCode.BAD_REQUEST.code);
        }
        Integer updated = repository.updateStatus(invoiceId, status);
        if (updated > 0 && invoice != null) {
            bumpDashboardRevenueVersion(invoice.getBranchId());
        }
        return updated;
    }

    @Transactional
    public Integer markPaid(Integer invoiceId, String source) {
        Invoice invoice = repository.getById(invoiceId);
        if (invoice == null) throw new AppException("Không tìm thấy hóa đơn", ErrorCode.BAD_REQUEST.code);
        if (invoice.getStatus() == InvoiceConstant.STATUS_DONE) return 0;
        if (invoice.getStatus() != InvoiceConstant.STATUS_UNPAID) {
            throw new AppException("Chỉ có thể xác nhận hóa đơn chưa thanh toán", ErrorCode.BAD_REQUEST.code);
        }

        Map<org.jooq.Field<?>, Object> paidFields = new HashMap<>();
        paidFields.put(INVOICE.RECEIPT_DATE, LocalDateTime.now());
        paidFields.put(INVOICE.PAID, invoice.getFee());
        paidFields.put(INVOICE.DEBT, 0D);
        int updated = repository.transitionStatus(invoiceId, InvoiceConstant.STATUS_UNPAID,
                InvoiceConstant.STATUS_DONE, paidFields);
        if (updated == 0) return 0;

        if (invoice.getOrderId() != null) {
            Order order = orderRepository.getById(invoice.getOrderId());
            if (order != null) {
                orderRepository.updateStatusAndPayment(invoice.getOrderId(), order.getStatus(), PaymentStatus.PAID);
            }
        }
        paymentRepository.markStatusByInvoice(invoiceId, "COMPLETED");
        bumpDashboardRevenueVersion(invoice.getBranchId());
        eventPublisher.publishEvent(new InvoicePaidEvent(invoiceId, invoice.getOrderId(), invoice.getUserId(),
                invoice.getCustomerId(), invoice.getFee() == null ? 0D : invoice.getFee()));
        return updated;
    }

    @Transactional
    public Integer cancelOrderInvoice(Integer invoiceId) {
        Invoice invoice = repository.getById(invoiceId);
        if (invoice == null) throw new AppException("Không tìm thấy hóa đơn", ErrorCode.BAD_REQUEST.code);
        if (invoice.getStatus() != InvoiceConstant.STATUS_UNPAID) {
            throw new AppException("Chỉ có thể hủy hóa đơn chưa thanh toán", ErrorCode.BAD_REQUEST.code);
        }
        int updated = repository.transitionStatus(invoiceId, InvoiceConstant.STATUS_UNPAID,
                InvoiceConstant.STATUS_CANCEL, Collections.emptyMap());
        if (updated == 0) return 0;

        if (invoice.getOrderId() != null) {
            orderRepository.updateStatusAndPayment(invoice.getOrderId(), OrderStatus.CANCELLED, PaymentStatus.UNPAID);
        }
        paymentRepository.cancelPendingByInvoice(invoiceId);
        Integer voucherId = invoiceVoucherRepository.findVoucherIdByInvoice(invoiceId);
        Integer removed = invoiceVoucherRepository.deleteByInvoice(invoiceId);
        if (removed != null && removed > 0 && invoice.getCustomerId() != null) {
            customerVoucherUsageRepository.decreaseUsage(voucherId, invoice.getCustomerId());
        }
        bumpDashboardRevenueVersion(invoice.getBranchId());
        return updated;
    }

    @Transactional
    public Integer refund(Integer invoiceId, String reason) {
        Invoice invoice = repository.getById(invoiceId);
        if (invoice == null) throw new AppException("Không tìm thấy hóa đơn", ErrorCode.BAD_REQUEST.code);
        if (invoice.getStatus() != InvoiceConstant.STATUS_DONE) {
            throw new AppException("Chỉ có thể hoàn tiền hóa đơn đã thanh toán", ErrorCode.BAD_REQUEST.code);
        }
        int updated = repository.transitionStatus(invoiceId, InvoiceConstant.STATUS_DONE,
                InvoiceConstant.STATUS_REFUNDED, Collections.emptyMap());
        if (updated == 0) throw new AppException("Hóa đơn đã thay đổi trạng thái", ErrorCode.BAD_REQUEST.code);
        if (invoice.getOrderId() != null) {
            orderRepository.updateStatusAndPayment(invoice.getOrderId(), OrderStatus.REFUNDED, PaymentStatus.REFUNDED);
        }
        paymentRepository.markStatusByInvoice(invoiceId, "REFUNDED");
        bumpDashboardRevenueVersion(invoice.getBranchId());
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

    @Transactional
    public Integer cancelInvoice(Integer id) {
        Invoice invoice = repository.getById(id);
        if (invoice == null) throw new AppException("Không tìm thấy hóa đơn", ErrorCode.BAD_REQUEST.code);
        if (invoice.getStatus() == InvoiceConstant.STATUS_DRAFT) {
            return delete(id);
        }
        return cancelOrderInvoice(id);
    }

    private void bumpDashboardRevenueVersion(Integer branchId) {
        cacheTemplate.increment(CacheKeyFactory.dashboardRevenueVersion(branchId));
    }
}
