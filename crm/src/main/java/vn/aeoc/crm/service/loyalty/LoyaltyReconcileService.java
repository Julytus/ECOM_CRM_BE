package vn.aeoc.crm.service.loyalty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.aeoc.crm.repository.loyalty.LoyaltyReconcileRepository;

import java.util.List;

@Service
public class LoyaltyReconcileService {
    private static final Logger log = LoggerFactory.getLogger(LoyaltyReconcileService.class);
    private final LoyaltyReconcileRepository repository;

    public LoyaltyReconcileService(LoyaltyReconcileRepository repository) {
        this.repository = repository;
    }

    public List<LoyaltyReconcileRepository.Mismatch> reconcile() {
        List<LoyaltyReconcileRepository.Mismatch> mismatches = repository.findMismatches();
        mismatches.forEach(mismatch -> log.warn(
                "Loyalty ledger mismatch for customer {}: balance={} ledgerPoints={} rankPoints={} ledgerRankPoints={}",
                mismatch.customerId(), mismatch.balance(), mismatch.ledgerPoints(),
                mismatch.rankPoints(), mismatch.ledgerRankPoints()));
        return mismatches;
    }
}
