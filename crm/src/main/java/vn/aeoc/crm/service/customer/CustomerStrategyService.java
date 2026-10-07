package vn.aeoc.crm.service.customer;

import org.springframework.stereotype.Service;
import vn.aeoc.crm.data.pojo.Purchase;
import vn.aeoc.crm.repository.sale.BoughtServiceItemRepository;
import vn.aeoc.crm.repository.segment.CustomerSegmentRepository;
import vn.aeoc.crm.repository.sale.BoughtProductRepository;
import vn.aeoc.crm.repository.sale.InvoiceRepository;

import java.util.*;

@Service
public class CustomerStrategyService {

    private final CustomerSegmentRepository segmentRepository;
    private final BoughtProductRepository boughtProductRepository;
    private final BoughtServiceItemRepository boughtServiceRepository;
    private final InvoiceRepository invoiceRepository;

    public CustomerStrategyService(
            CustomerSegmentRepository segmentRepository,
            BoughtProductRepository boughtProductRepository,
            BoughtServiceItemRepository boughtServiceRepository,
            InvoiceRepository invoiceRepository
    ) {
        this.segmentRepository = segmentRepository;
        this.boughtProductRepository = boughtProductRepository;
        this.boughtServiceRepository = boughtServiceRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public Map<String, Object> buildAiContext(Integer customerId) {

        // 1. Segment
        var segment = segmentRepository.getCustomerSegmentSnapshot(customerId);

        // 2. Preferences (top category)
        List<String> productPrefs =
                boughtProductRepository.getTopProductCategories(customerId, 3);

        List<String> servicePrefs =
                boughtServiceRepository.getTopServiceCategories(customerId, 3);

        List<String> preferences = mergePreferences(productPrefs, servicePrefs);

        // 3. Recent purchases
        List<Purchase> recentProducts =
                boughtProductRepository.getRecentProducts(customerId, 5);

        List<Purchase> recentServices =
                boughtServiceRepository.getRecentServices(customerId, 5);

        List<Map<String, Object>> recentPurchases =
                mergeRecentPurchases(recentProducts, recentServices);

        // 4. Build profile
        Map<String, Object> profile = Map.of(
                "segment", segment.getSegmentCode(),
                "recencyDays", segment.getRecencyDays(),
                "frequency90d", segment.getFrequency90d(),
                "monetary90d", segment.getMonetary90d(),
                "branchId", segment.getBranchId(),
                "preferences", preferences,
                "recentPurchases", recentPurchases
        );

        return Map.of(
                "profile", profile
        );
    }

    // =========================
    // Helpers
    // =========================

    private List<String> mergePreferences(List<String> productPrefs, List<String> servicePrefs) {
        Set<String> merged = new LinkedHashSet<>();

        if (productPrefs != null) merged.addAll(productPrefs);
        if (servicePrefs != null) merged.addAll(servicePrefs);

        return new ArrayList<>(merged);
    }

    private List<Map<String, Object>> mergeRecentPurchases(
            List<Purchase> products,
            List<Purchase> services
    ) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (products != null) {
            for (Purchase p : products) {
                result.add(toMap(p));
            }
        }

        if (services != null) {
            for (Purchase p : services) {
                result.add(toMap(p));
            }
        }

        return result;
    }

    private Map<String, Object> toMap(Purchase p) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", p.getName());
        map.put("value", p.getValue());
        return map;
    }
}