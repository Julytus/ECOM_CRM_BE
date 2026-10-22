package vn.aeoc.crm.service.loyalty;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.constant.LoyaltyConstant;
import vn.aeoc.crm.repository.loyalty.LoyaltyAccountRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyConfigRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyTierRepository;
import vn.aeoc.crm.repository.loyalty.LoyaltyTransactionRepository;
import vn.aeoc.entity.data.mysql.sale.LoyaltyAccount;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTier;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTransaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class LoyaltyLedgerService {
    private static final String CONFIG_ERROR = "Cấu hình earn_rate phải lớn hơn 0";
    private final LoyaltyAccountRepository accounts;
    private final LoyaltyTransactionRepository transactions;
    private final LoyaltyTierRepository tiers;
    private final LoyaltyConfigRepository config;

    public LoyaltyLedgerService(LoyaltyAccountRepository accounts,
                                LoyaltyTransactionRepository transactions,
                                LoyaltyTierRepository tiers,
                                LoyaltyConfigRepository config) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.tiers = tiers;
        this.config = config;
    }

    @Transactional
    public EarnResult earnForOrder(Integer customerId, Integer orderId, double baseAmount) {
        if (customerId == null || customerId <= 0 || orderId == null || orderId <= 0) {
            return new EarnResult(0, 0, false);
        }
        Integer earnRate = config.getEarnRate();
        if (earnRate == null || earnRate <= 0) throw new AppException(CONFIG_ERROR, ErrorCode.BAD_REQUEST.code);

        long base = BigDecimal.valueOf(baseAmount)
                .setScale(0, RoundingMode.HALF_UP)
                .divideToIntegralValue(BigDecimal.valueOf(earnRate))
                .longValueExact();
        accounts.getOrCreate(customerId);
        LoyaltyAccount account = accounts.lockByCustomerId(customerId);
        if (account == null) throw new AppException("Không tìm thấy tài khoản loyalty", ErrorCode.BAD_REQUEST.code);
        if (transactions.findByRef(LoyaltyConstant.EARN, LoyaltyConstant.REF_ORDER, orderId.toString()) != null) {
            return new EarnResult(0, 0, false);
        }

        BigDecimal multiplier = tiers.listActiveOrderedByThreshold().stream()
                .filter(tier -> tier.getId().equals(account.getTierId()))
                .map(LoyaltyTier::getEarnMultiplier)
                .findFirst().orElse(BigDecimal.ONE);
        long points = BigDecimal.valueOf(base).multiply(multiplier)
                .setScale(0, RoundingMode.DOWN).longValueExact();
        if (points <= 0 && base <= 0) return new EarnResult(0, 0, false);
        return recordDelta(account, LoyaltyConstant.EARN, points, base, LoyaltyConstant.REF_ORDER,
                orderId.toString(), null, null);
    }

    @Transactional
    public ReverseResult reverseOrder(Integer orderId) {
        if (orderId == null || orderId <= 0) return ReverseResult.NOTHING_TO_REVERSE;
        String refId = orderId.toString();
        LoyaltyTransaction earn = transactions.findByRef(LoyaltyConstant.EARN, LoyaltyConstant.REF_ORDER, refId);
        if (earn == null) return ReverseResult.NOTHING_TO_REVERSE;
        LoyaltyAccount account = accounts.lockByCustomerId(earn.getCustomerId());
        if (account == null) throw new AppException("Không tìm thấy tài khoản loyalty", ErrorCode.BAD_REQUEST.code);
        if (transactions.findByRef(LoyaltyConstant.REVERSE_EARN, LoyaltyConstant.REF_ORDER, refId) != null) {
            return ReverseResult.ALREADY_REVERSED;
        }
        recordDelta(account, LoyaltyConstant.REVERSE_EARN, -earn.getPoints(), -value(earn.getRankPointsDelta()),
                LoyaltyConstant.REF_ORDER, refId, "Điều chỉnh do hoàn đơn #" + orderId, earn.getCreatedBy());
        return ReverseResult.REVERSED;
    }

    @Transactional
    public boolean spend(Integer customerId, long points, String refType, String refId) {
        validateCustomer(customerId);
        if (points <= 0 || !StringUtils.hasText(refType) || !StringUtils.hasText(refId)) {
            throw new AppException("Thông tin sử dụng điểm không hợp lệ", ErrorCode.BAD_REQUEST.code);
        }
        accounts.getOrCreate(customerId);
        LoyaltyAccount account = accounts.lockByCustomerId(customerId);
        if (transactions.findByRef(LoyaltyConstant.REDEEM, refType, refId) != null) return false;
        if (value(account.getBalance()) < 0 || value(account.getBalance()) < points) {
            throw new AppException("Số dư điểm không đủ", ErrorCode.BAD_REQUEST.code);
        }
        recordDelta(account, LoyaltyConstant.REDEEM, -points, 0, refType, refId, null, null);
        return true;
    }

    @Transactional
    public boolean adjust(Integer customerId, long points, long rankDelta, String reason, Integer adminId) {
        validateCustomer(customerId);
        if (!StringUtils.hasText(reason)) throw new AppException("Lý do điều chỉnh là bắt buộc", ErrorCode.BAD_REQUEST.code);
        accounts.getOrCreate(customerId);
        LoyaltyAccount account = accounts.lockByCustomerId(customerId);
        return recordDelta(account, LoyaltyConstant.ADJUST, points, rankDelta, "ADMIN",
                UUID.randomUUID().toString(), reason.trim(), adminId).applied();
    }

    private EarnResult recordDelta(LoyaltyAccount account, String type, long points, long rankDelta,
                                   String refType, String refId, String note, Integer createdBy) {
        long newBalance = value(account.getBalance()) + points;
        long newRankPoints = value(account.getRankPoints()) + rankDelta;
        LoyaltyTier tier = tiers.findHighestTier(newRankPoints);
        LoyaltyTransaction transaction = new LoyaltyTransaction();
        transaction.setCustomerId(account.getCustomerId());
        transaction.setType(type);
        transaction.setPoints(points);
        transaction.setRankPointsDelta(rankDelta);
        transaction.setBalanceAfter(newBalance);
        transaction.setRankPointsAfter(newRankPoints);
        transaction.setRefType(refType);
        transaction.setRefId(refId);
        transaction.setNote(note);
        transaction.setCreatedBy(createdBy);
        if (!transactions.insert(transaction)) return new EarnResult(0, 0, false);
        accounts.applyDelta(account.getId(), points, rankDelta, tier == null ? null : tier.getId());
        return new EarnResult(points, rankDelta, true);
    }

    private void validateCustomer(Integer customerId) {
        if (customerId == null || customerId <= 0) throw new AppException("customerId không hợp lệ", ErrorCode.BAD_REQUEST.code);
    }

    private long value(Long value) { return value == null ? 0L : value; }

    public record EarnResult(long points, long rankDelta, boolean applied) { }
    public enum ReverseResult { REVERSED, ALREADY_REVERSED, NOTHING_TO_REVERSE }
}
