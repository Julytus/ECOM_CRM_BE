package vn.aeoc.crm.service.loyalty;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.crm.data.response.LoyaltyAccountResponse;
import vn.aeoc.crm.data.response.MyVoucherResponse;
import vn.aeoc.crm.data.response.RedeemableVoucherResponse;
import vn.aeoc.crm.repository.loyalty.LoyaltyTransactionRepository;
import vn.aeoc.crm.repository.sale.VoucherRepository;
import vn.aeoc.entity.data.mysql.sale.LoyaltyAccount;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTier;
import vn.aeoc.entity.data.mysql.sale.LoyaltyTransaction;
import vn.aeoc.entity.data.mysql.sale.Voucher;

import java.util.List;

import static vn.entity.backend.tables.CustomerVoucher.CUSTOMER_VOUCHER;
import static vn.entity.backend.tables.LoyaltyAccount.LOYALTY_ACCOUNT;
import static vn.entity.backend.tables.LoyaltyTier.LOYALTY_TIER;
import static vn.entity.backend.tables.Voucher.VOUCHER;

@Service
public class LoyaltyQueryService {
    private final DSLContext dsl;
    private final LoyaltyTransactionRepository transactions;
    private final VoucherRepository vouchers;

    public LoyaltyQueryService(DSLContext dsl, LoyaltyTransactionRepository transactions, VoucherRepository vouchers) {
        this.dsl = dsl;
        this.transactions = transactions;
        this.vouchers = vouchers;
    }

    public LoyaltyAccountResponse getMyAccount(Integer customerId) {
        LoyaltyAccount account = dsl.selectFrom(LOYALTY_ACCOUNT)
                .where(LOYALTY_ACCOUNT.CUSTOMER_ID.eq(customerId)).fetchOneInto(LoyaltyAccount.class);
        long balance = account == null ? 0 : value(account.getBalance());
        long rank = account == null ? 0 : value(account.getRankPoints());
        LoyaltyTier current = dsl.selectFrom(LOYALTY_TIER)
                .where(LOYALTY_TIER.STATUS.eq(1).and(LOYALTY_TIER.MIN_RANK_POINTS.le(rank)))
                .orderBy(LOYALTY_TIER.MIN_RANK_POINTS.desc()).limit(1).fetchOneInto(LoyaltyTier.class);
        LoyaltyTier next = dsl.selectFrom(LOYALTY_TIER)
                .where(LOYALTY_TIER.STATUS.eq(1).and(LOYALTY_TIER.MIN_RANK_POINTS.gt(rank)))
                .orderBy(LOYALTY_TIER.MIN_RANK_POINTS.asc()).limit(1).fetchOneInto(LoyaltyTier.class);
        return new LoyaltyAccountResponse(balance, rank, current == null ? null : current.getId(),
                current == null ? null : current.getName(), next == null ? null : next.getId(),
                next == null ? null : next.getName(), next == null ? null : next.getMinRankPoints(),
                next == null ? 0 : Math.max(0, next.getMinRankPoints() - rank));
    }

    public Page<LoyaltyTransaction> getMyTransactions(Integer customerId, Pageable pageable) {
        return transactions.pageByCustomer(customerId, pageable);
    }

    public List<RedeemableVoucherResponse> listRedeemable(Integer customerId) {
        LoyaltyAccount account = dsl.selectFrom(LOYALTY_ACCOUNT)
                .where(LOYALTY_ACCOUNT.CUSTOMER_ID.eq(customerId)).fetchOneInto(LoyaltyAccount.class);
        long balance = account == null ? 0 : value(account.getBalance());
        long rank = account == null ? 0 : value(account.getRankPoints());
        return vouchers.findRedeemable().stream()
                .map(v -> new RedeemableVoucherResponse(v.getId(), v.getCode(), v.getName(), v.getPointsCost(),
                        v.getRedeemStock(), v.getRedeemedCount(), v.getMinTierId(),
                        v.getPointsCost() != null && balance >= v.getPointsCost(), tierOk(v.getMinTierId(), rank),
                        v.getEndDate()))
                .toList();
    }

    public Page<MyVoucherResponse> getMyVouchers(Integer customerId, String status, Pageable pageable) {
        var condition = CUSTOMER_VOUCHER.CUSTOMER_ID.eq(customerId);
        if (status != null && !status.isBlank()) condition = condition.and(CUSTOMER_VOUCHER.STATUS.eq(status));
        long total = dsl.fetchCount(CUSTOMER_VOUCHER, condition);
        List<MyVoucherResponse> rows = dsl.select(CUSTOMER_VOUCHER.ID, CUSTOMER_VOUCHER.VOUCHER_ID,
                        VOUCHER.CODE, VOUCHER.NAME, CUSTOMER_VOUCHER.STATUS, CUSTOMER_VOUCHER.POINTS_SPENT,
                        CUSTOMER_VOUCHER.EXPIRES_AT, CUSTOMER_VOUCHER.CREATED_AT)
                .from(CUSTOMER_VOUCHER.join(VOUCHER).on(VOUCHER.ID.eq(CUSTOMER_VOUCHER.VOUCHER_ID)))
                .where(condition).orderBy(CUSTOMER_VOUCHER.ID.desc())
                .limit(pageable.getLimit()).offset(pageable.getOffset())
                .fetch(r -> new MyVoucherResponse(r.get(CUSTOMER_VOUCHER.ID), r.get(CUSTOMER_VOUCHER.VOUCHER_ID),
                        r.get(VOUCHER.CODE), r.get(VOUCHER.NAME), r.get(CUSTOMER_VOUCHER.STATUS),
                        r.get(CUSTOMER_VOUCHER.POINTS_SPENT), r.get(CUSTOMER_VOUCHER.EXPIRES_AT),
                        r.get(CUSTOMER_VOUCHER.CREATED_AT)));
        pageable.setTotal(total);
        return new Page<>(pageable, rows);
    }

    private boolean tierOk(Integer minTierId, long rankPoints) {
        if (minTierId == null) return true;
        Long threshold = dsl.select(LOYALTY_TIER.MIN_RANK_POINTS).from(LOYALTY_TIER)
                .where(LOYALTY_TIER.ID.eq(minTierId)).fetchOne(LOYALTY_TIER.MIN_RANK_POINTS);
        return threshold != null && rankPoints >= threshold;
    }

    private long value(Long value) { return value == null ? 0 : value; }
}
