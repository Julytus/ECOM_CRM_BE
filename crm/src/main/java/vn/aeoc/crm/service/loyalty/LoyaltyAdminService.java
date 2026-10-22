package vn.aeoc.crm.service.loyalty;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.data.request.LoyaltyTierRequest;
import vn.aeoc.entity.data.mysql.sale.LoyaltyAccount;
import vn.aeoc.entity.data.mysql.sale.LoyaltyConfig;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTier;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTransaction;

import java.math.BigDecimal;
import java.util.List;

import static vn.entity.backend.tables.Customer.CUSTOMER;
import static vn.entity.backend.tables.LoyaltyAccount.LOYALTY_ACCOUNT;
import static vn.entity.backend.tables.LoyaltyConfig.LOYALTY_CONFIG;
import static vn.entity.backend.tables.LoyaltyTier.LOYALTY_TIER;
import static vn.entity.backend.tables.LoyaltyTransaction.LOYALTY_TRANSACTION;
import static vn.entity.backend.tables.User.USER;
import static vn.entity.backend.tables.Voucher.VOUCHER;

@Service
public class LoyaltyAdminService {
    private final DSLContext dsl;
    private final LoyaltyLedgerService ledger;

    public LoyaltyAdminService(DSLContext dsl, LoyaltyLedgerService ledger) {
        this.dsl = dsl;
        this.ledger = ledger;
    }

    public List<LoyaltyTier> listTiers() {
        return dsl.selectFrom(LOYALTY_TIER).orderBy(LOYALTY_TIER.MIN_RANK_POINTS.asc())
                .fetchInto(LoyaltyTier.class);
    }

    @Transactional
    public LoyaltyTier saveTier(LoyaltyTierRequest request) {
        if (request == null || !StringUtils.hasText(request.name()) || request.minRankPoints() == null
                || request.earnMultiplier() == null || request.earnMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("Thông tin hạng loyalty không hợp lệ");
        }
        var duplicate = LOYALTY_TIER.MIN_RANK_POINTS.eq(request.minRankPoints());
        if (request.id() != null) duplicate = duplicate.and(LOYALTY_TIER.ID.ne(request.id()));
        if (dsl.fetchExists(dsl.selectFrom(LOYALTY_TIER).where(duplicate))) {
            throw badRequest("Ngưỡng điểm hạng đã tồn tại");
        }
        LoyaltyTier tier = new LoyaltyTier();
        tier.setId(request.id());
        tier.setName(request.name().trim());
        tier.setMinRankPoints(request.minRankPoints());
        tier.setEarnMultiplier(request.earnMultiplier());
        tier.setStatus(request.status() == null ? 1 : request.status());
        if (request.id() == null || request.id() <= 0) {
            var row = dsl.insertInto(LOYALTY_TIER)
                    .set(LOYALTY_TIER.NAME, tier.getName())
                    .set(LOYALTY_TIER.MIN_RANK_POINTS, tier.getMinRankPoints())
                    .set(LOYALTY_TIER.EARN_MULTIPLIER, tier.getEarnMultiplier())
                    .set(LOYALTY_TIER.STATUS, tier.getStatus())
                    .returning().fetchOne();
            return row == null ? tier : row.into(LoyaltyTier.class);
        }
        int changed = dsl.update(LOYALTY_TIER).set(LOYALTY_TIER.NAME, tier.getName())
                .set(LOYALTY_TIER.MIN_RANK_POINTS, tier.getMinRankPoints())
                .set(LOYALTY_TIER.EARN_MULTIPLIER, tier.getEarnMultiplier())
                .set(LOYALTY_TIER.STATUS, tier.getStatus()).where(LOYALTY_TIER.ID.eq(request.id())).execute();
        if (changed == 0) throw badRequest("Không tìm thấy hạng loyalty");
        return dsl.selectFrom(LOYALTY_TIER).where(LOYALTY_TIER.ID.eq(request.id())).fetchOneInto(LoyaltyTier.class);
    }

    @Transactional
    public int deleteTier(Integer id) {
        if (id == null || id <= 0) throw badRequest("id hạng không hợp lệ");
        if (dsl.fetchExists(dsl.selectOne().from(LOYALTY_ACCOUNT).where(LOYALTY_ACCOUNT.TIER_ID.eq(id)))
                || dsl.fetchExists(dsl.selectOne().from(VOUCHER).where(VOUCHER.MIN_TIER_ID.eq(id)))) {
            throw badRequest("Không thể xóa hạng đang được tài khoản hoặc voucher sử dụng");
        }
        return dsl.deleteFrom(LOYALTY_TIER).where(LOYALTY_TIER.ID.eq(id)).execute();
    }

    public LoyaltyConfig getConfig() {
        return dsl.selectFrom(LOYALTY_CONFIG).where(LOYALTY_CONFIG.ID.eq(1)).fetchOneInto(LoyaltyConfig.class);
    }

    @Transactional
    public LoyaltyConfig updateConfig(Integer earnRate) {
        if (earnRate == null || earnRate <= 0) throw badRequest("earn_rate phải lớn hơn 0");
        int changed = dsl.update(LOYALTY_CONFIG).set(LOYALTY_CONFIG.EARN_RATE, earnRate)
                .where(LOYALTY_CONFIG.ID.eq(1)).execute();
        if (changed == 0) dsl.insertInto(LOYALTY_CONFIG).set(LOYALTY_CONFIG.ID, 1)
                .set(LOYALTY_CONFIG.EARN_RATE, earnRate).execute();
        return getConfig();
    }

    public boolean adjust(Integer customerId, long points, long rankDelta, String reason, Integer adminId) {
        return ledger.adjust(customerId, points, rankDelta, reason, adminId);
    }

    public LoyaltyAccount getAccount(Integer customerId) {
        return dsl.selectFrom(LOYALTY_ACCOUNT).where(LOYALTY_ACCOUNT.CUSTOMER_ID.eq(customerId))
                .fetchOneInto(LoyaltyAccount.class);
    }

    public Page<LoyaltyTransaction> getTransactions(Integer customerId, Pageable pageable) {
        long total = dsl.fetchCount(LOYALTY_TRANSACTION, LOYALTY_TRANSACTION.CUSTOMER_ID.eq(customerId));
        List<LoyaltyTransaction> rows = dsl.selectFrom(LOYALTY_TRANSACTION)
                .where(LOYALTY_TRANSACTION.CUSTOMER_ID.eq(customerId)).orderBy(LOYALTY_TRANSACTION.ID.desc())
                .limit(pageable.getLimit()).offset(pageable.getOffset()).fetchInto(LoyaltyTransaction.class);
        pageable.setTotal(total);
        return new Page<>(pageable, rows);
    }

    @Transactional
    public int linkAccount(Integer customerId, Integer accountUserId) {
        if (customerId == null || customerId <= 0 || accountUserId == null || accountUserId <= 0) {
            throw badRequest("customerId và accountUserId là bắt buộc");
        }
        if (!dsl.fetchExists(dsl.selectOne().from(USER).where(USER.ID.eq(accountUserId)))) {
            throw badRequest("Không tìm thấy tài khoản người dùng");
        }
        if (dsl.fetchExists(dsl.selectOne().from(CUSTOMER)
                .where(CUSTOMER.ACCOUNT_USER_ID.eq(accountUserId).and(CUSTOMER.ID.ne(customerId))))) {
            throw badRequest("Tài khoản đã được liên kết với khách hàng khác");
        }
        int changed = dsl.update(CUSTOMER).set(CUSTOMER.ACCOUNT_USER_ID, accountUserId)
                .where(CUSTOMER.ID.eq(customerId)).execute();
        if (changed == 0 && !dsl.fetchExists(dsl.selectOne().from(CUSTOMER).where(CUSTOMER.ID.eq(customerId)))) {
            throw badRequest("Không tìm thấy khách hàng");
        }
        return changed;
    }

    private AppException badRequest(String message) { return new AppException(message, ErrorCode.BAD_REQUEST.code); }
}
