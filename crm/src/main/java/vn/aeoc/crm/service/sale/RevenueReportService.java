package vn.aeoc.crm.service.sale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.data.report.RevenueBucket;
import vn.aeoc.crm.data.report.RevenuePaymentMethod;
import vn.aeoc.crm.data.report.RevenueSummary;
import vn.aeoc.crm.data.report.TopProductStatistic;
import vn.aeoc.crm.repository.sale.ReportRepository;
import vn.aeoc.crm.service.cache.CacheKeyFactory;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RevenueReportService {
    private static final Duration CACHE_TTL = Duration.ofSeconds(45);
    private static final int MAX_RANGE_DAYS = 366;
    private static final int MAX_LIMIT = 50;

    private final ReportRepository repository;
    private final ThreeLayerCacheTemplate cacheTemplate;

    public RevenueSummary getSummary(LocalDate from, LocalDate to, Integer branchId) {
        DateRange range = normalizeAndValidate(from, to);
        String key = key("summary", branchId, range.from(), range.to(), "");
        return cacheTemplate.getOrLoad(key, CACHE_TTL, () -> {
            RevenueSummary summary = repository.getRevenueSummary(range.from(), range.to(), branchId);
            if (summary == null) summary = new RevenueSummary();
            summary.setAvgOrderValue(divide(summary.getTotalRevenue(), summary.getInvoiceCount()));
            summary.setPreviousAvgOrderValue(divide(summary.getPreviousTotalRevenue(), summary.getPreviousInvoiceCount()));
            summary.setTotalRevenueChangePercent(changePercent(summary.getTotalRevenue(), summary.getPreviousTotalRevenue()));
            summary.setInvoiceCountChangePercent(changePercent(summary.getInvoiceCount(), summary.getPreviousInvoiceCount()));
            return summary;
        });
    }

    public List<RevenueBucket> getSeries(LocalDate from, LocalDate to, Integer branchId, String groupBy) {
        DateRange range = normalizeAndValidate(from, to);
        if (groupBy == null || !List.of("day", "week", "month").contains(groupBy)) throw inputWrong();
        String key = key("series", branchId, range.from(), range.to(), groupBy);
        return cacheTemplate.getOrLoad(key, CACHE_TTL,
                () -> zeroFill(range.from(), range.to(), groupBy,
                        repository.getRevenueSeries(range.from(), range.to(), branchId, groupBy)));
    }

    public List<RevenuePaymentMethod> getByPaymentMethod(LocalDate from, LocalDate to, Integer branchId) {
        DateRange range = normalizeAndValidate(from, to);
        String key = key("payment-method", branchId, range.from(), range.to(), "");
        return cacheTemplate.getOrLoad(key, CACHE_TTL,
                () -> repository.getRevenueByPaymentMethod(range.from(), range.to(), branchId));
    }

    public List<TopProductStatistic> getTopProducts(LocalDate from, LocalDate to, Integer branchId, Integer limit) {
        DateRange range = normalizeAndValidate(from, to);
        int resolvedLimit = limit == null ? 10 : limit;
        if (resolvedLimit < 1 || resolvedLimit > MAX_LIMIT) throw inputWrong();
        String key = key("top-products", branchId, range.from(), range.to(), Integer.toString(resolvedLimit));
        return cacheTemplate.getOrLoad(key, CACHE_TTL,
                () -> repository.getRevenueTopProducts(range.from(), range.to(), branchId, resolvedLimit));
    }

    private String key(String report, Integer branchId, LocalDate from, LocalDate to, String options) {
        long version = cacheTemplate.getLong(CacheKeyFactory.dashboardRevenueVersion(branchId), 0L);
        return CacheKeyFactory.reportRevenue(report, branchId, from, to, options, version);
    }

    private DateRange normalizeAndValidate(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            to = LocalDate.now();
            from = to.minusDays(29);
        }
        if (from.isAfter(to) || to.toEpochDay() - from.toEpochDay() + 1 > MAX_RANGE_DAYS) throw inputWrong();
        return new DateRange(from, to);
    }

    private List<RevenueBucket> zeroFill(LocalDate from, LocalDate to, String groupBy, List<RevenueBucket> rows) {
        Map<String, RevenueBucket> values = new HashMap<>();
        if (rows != null) rows.forEach(row -> values.put(row.getBucket(), row));
        List<RevenueBucket> result = new ArrayList<>();
        LocalDate cursor = switch (groupBy) {
            case "week" -> from.with(WeekFields.ISO.dayOfWeek(), 1);
            case "month" -> from.withDayOfMonth(1);
            default -> from;
        };
        while (!cursor.isAfter(to)) {
            String bucket = bucketKey(cursor, groupBy);
            result.add(values.getOrDefault(bucket, new RevenueBucket(bucket, 0.0, 0L)));
            cursor = switch (groupBy) {
                case "week" -> cursor.plusWeeks(1);
                case "month" -> cursor.plusMonths(1);
                default -> cursor.plusDays(1);
            };
        }
        return result;
    }

    private String bucketKey(LocalDate date, String groupBy) {
        return switch (groupBy) {
            case "week" -> "%04d-W%02d".formatted(
                    date.get(WeekFields.ISO.weekBasedYear()), date.get(WeekFields.ISO.weekOfWeekBasedYear()));
            case "month" -> "%04d-%02d".formatted(date.getYear(), date.getMonthValue());
            default -> date.toString();
        };
    }

    private Double divide(Double total, Long count) {
        return total == null || count == null || count == 0 ? 0.0 : total / count;
    }

    private Double changePercent(Number current, Number previous) {
        double currentValue = current == null ? 0 : current.doubleValue();
        double previousValue = previous == null ? 0 : previous.doubleValue();
        if (previousValue == 0) return currentValue == 0 ? 0.0 : 100.0;
        return (currentValue - previousValue) * 100.0 / Math.abs(previousValue);
    }

    private AppException inputWrong() {
        return new AppException(ErrorCode.INPUT_WRONG);
    }

    private record DateRange(LocalDate from, LocalDate to) { }
}
