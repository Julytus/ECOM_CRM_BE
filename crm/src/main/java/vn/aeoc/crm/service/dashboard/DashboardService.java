package vn.aeoc.crm.service.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.aeoc.crm.data.report.*;
import vn.aeoc.crm.repository.dashboard.DashboardRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j(topic = "DASHBOARD-SERVICE")
@Service
public class DashboardService {
    private static final Set<String> GROUPS = Set.of("DAY", "MONTH", "YEAR");
    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) {
        this.repository = repository;
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new IllegalArgumentException("Require fromDate and toDate; toDate must be after fromDate");
        }
    }

    private String group(String value) {
        String result = value == null ? "DAY" : value.trim().toUpperCase();
        if (!GROUPS.contains(result)) {
            throw new IllegalArgumentException("groupBy must be DAY, MONTH or YEAR");
        }
        return result;
    }

    public List<RevenueStatistic> getRevenue(LocalDate from, LocalDate to, String groupBy) {
        validateRange(from, to);
        return repository.getRevenue(from, to, group(groupBy));
    }

    public List<OrderStatusStatistic> getOrderStatus(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<OrderStatusStatistic> rows = repository.getOrderStatus(from, to);
        int total = rows.stream().mapToInt(x -> x.getTotalOrders() == null ? 0 : x.getTotalOrders()).sum();
        rows.forEach(x -> x.setPercentage(total == 0 ? 0.0 :
                Math.round(x.getTotalOrders() * 10000.0 / total) / 100.0));
        return rows;
    }

    public List<TopProductStatistic> getTopProducts(
            LocalDate from, LocalDate to, Integer limit, String sortBy) {
        validateRange(from, to);
        int size = limit == null ? 10 : limit;
        if (size < 1 || size > 100) throw new IllegalArgumentException("limit must be 1..100");
        String sort = sortBy == null ? "QUANTITY" : sortBy.trim().toUpperCase();
        if (!Set.of("QUANTITY", "REVENUE").contains(sort)) {
            throw new IllegalArgumentException("sortBy must be QUANTITY or REVENUE");
        }
        return repository.getTopProducts(from, to, size, sort);
    }

    public List<AovStatistic> getAov(LocalDate from, LocalDate to, String groupBy) {
        validateRange(from, to);
        return repository.getAov(from, to, group(groupBy));
    }

    public InventoryStatistic getInventory(Integer threshold, Integer limit) {
        int stockLimit = threshold == null ? 5 : threshold;
        int size = limit == null ? 20 : limit;
        if (stockLimit < 0) throw new IllegalArgumentException("threshold must be >= 0");
        if (size < 1 || size > 100) throw new IllegalArgumentException("limit must be 1..100");

        DashboardRepository.InventoryCounts c = repository.getInventoryCounts(stockLimit);
        return new InventoryStatistic(stockLimit, c.totalVariants(),
                c.lowStockVariants(), c.outOfStockVariants(),
                repository.getLowStockItems(stockLimit, size));
    }
}