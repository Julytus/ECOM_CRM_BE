package vn.aeoc.crm.service.sale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.crm.data.report.*;
import vn.aeoc.crm.repository.sale.ReportRepository;
import vn.aeoc.crm.service.cache.CacheKeyFactory;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Duration TTL_SHORT = Duration.ofSeconds(45);
    private static final Duration TTL_NORMAL = Duration.ofSeconds(90);

    private final ReportRepository repository;
    private final ThreeLayerCacheTemplate cacheTemplate;

    public List<CustomerMonthlyReport> getCustomerByMonth(Integer year, Integer branchId) {
        String key = CacheKeyFactory.reportCustomerByMonth(branchId, year);
        return cacheTemplate.getOrLoad(key, TTL_NORMAL, () -> repository.countCustomerByMonth(year, branchId));
    }

    public List<CustomerSourceReport> getCustomerBySource(Integer year, Integer branchId) {
        String key = CacheKeyFactory.reportCustomerBySource(branchId, year);
        return cacheTemplate.getOrLoad(key, TTL_NORMAL, () -> repository.countCustomerBySource(year, branchId));
    }

    public InvoiceMonthlyRevenueReport getMonthlyRevenue(Integer year, Integer month, Integer branchId) {
        String key = CacheKeyFactory.reportMonthlyRevenue(branchId, year, month);
        return cacheTemplate.getOrLoad(key, TTL_SHORT, () -> repository.getMonthlyRevenue(year, month, branchId));
    }

    public Page<InvoiceFrequencyReport> getInvoiceFrequency(Integer year, Integer month, Integer branchId, Pageable pageable) {
        String pageKey = CacheKeyFactory.reportInvoiceFrequency(
                branchId,
                year,
                month,
                pageable.getPage(),
                pageable.getLimit()
        );
        String countKey = CacheKeyFactory.reportInvoiceFrequencyCount(branchId, year, month);

        List<InvoiceFrequencyReport> list = cacheTemplate.getOrLoad(
                pageKey,
                TTL_SHORT,
                () -> repository.getInvoiceFrequency(year, month, branchId, pageable)
        );

        Long count = cacheTemplate.getOrLoad(
                countKey,
                TTL_SHORT,
                () -> repository.countInvoiceFrequency(year, month, branchId)
        );

        pageable.setTotal(count);
        return new Page<>(pageable, list);
    }

    public Page<CustomerInterestAvgReport> getAvgInterestByCustomer(
            String customerName,
            Integer branchId,
            Pageable pageable) {

        String pageKey = CacheKeyFactory.reportAvgInterest(
                branchId,
                customerName,
                pageable.getPage(),
                pageable.getLimit()
        );

        String countKey = CacheKeyFactory.reportAvgInterestCount(branchId, customerName);

        List<CustomerInterestAvgReport> list = cacheTemplate.getOrLoad(
                pageKey,
                TTL_SHORT,
                () -> repository.getAvgInterestByCustomer(customerName, branchId, pageable)
        );

        Long count = cacheTemplate.getOrLoad(
                countKey,
                TTL_SHORT,
                () -> repository.countAvgInterestByCustomer(customerName, branchId)
        );

        pageable.setTotal(count);
        return new Page<>(pageable, list);
    }

    public List<CustomerInterestMonthlyBarReport> getMonthlyInterestBar(
            Integer year,
            Double minAvgInterest,
            Integer branchId) {

        String key = CacheKeyFactory.reportInterestBar(branchId, year, minAvgInterest);
        return cacheTemplate.getOrLoad(key, TTL_NORMAL, () -> {
            List<CustomerInterestMonthlyBarReport> raw =
                    repository.getMonthlyInterestBar(year, minAvgInterest, branchId);

            List<CustomerInterestMonthlyBarReport> result = new ArrayList<>();

            for (int m = 1; m <= 12; m++) {
                int month = m;
                CustomerInterestMonthlyBarReport item = raw.stream()
                        .filter(r -> r.getMonth().equals(month))
                        .findFirst()
                        .orElseGet(() -> {
                            CustomerInterestMonthlyBarReport empty =
                                    new CustomerInterestMonthlyBarReport();
                            empty.setMonth(month);
                            empty.setTotalCustomer(0L);
                            empty.setHighInterestCustomer(0L);
                            return empty;
                        });
                result.add(item);
            }
            return result;
        });
    }
}
