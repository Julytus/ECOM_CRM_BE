package vn.aeoc.crm.service.loyalty;

import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.constant.LoyaltyConstant;
import vn.aeoc.crm.repository.loyalty.LoyaltyAccountRepository;
import vn.aeoc.crm.repository.sale.VoucherRepository;
import vn.aeoc.entity.data.mysql.sale.LoyaltyAccount;
import vn.aeoc.entity.data.mysql.sale.Voucher;

import java.time.LocalDateTime;

import static vn.entity.backend.tables.CustomerVoucher.CUSTOMER_VOUCHER;
import static vn.entity.backend.tables.LoyaltyTier.LOYALTY_TIER;
import static vn.entity.backend.tables.Voucher.VOUCHER;

@Service
public class LoyaltyRedeemService {
    private final DSLContext dsl;
    private final LoyaltyAccountRepository accounts;
    private final VoucherRepository vouchers;
    private final LoyaltyLedgerService ledger;

    public LoyaltyRedeemService(DSLContext dsl, LoyaltyAccountRepository accounts,
                                VoucherRepository vouchers, LoyaltyLedgerService ledger) {
        this.dsl = dsl;
        this.accounts = accounts;
        this.vouchers = vouchers;
        this.ledger = ledger;
    }

    @Transactional
    public Integer redeem(Integer customerId, Integer voucherId) {
        if (customerId == null || customerId <= 0 || voucherId == null || voucherId <= 0) {
            throw badRequest("Thông tin đổi voucher không hợp lệ");
        }
        accounts.getOrCreate(customerId);
        LoyaltyAccount account = accounts.lockByCustomerId(customerId);
        if (account == null) throw badRequest("Không tìm thấy tài khoản loyalty");

        Voucher voucher = dsl.selectFrom(VOUCHER).where(VOUCHER.ID.eq(voucherId)).forUpdate().fetchOneInto(Voucher.class);
        if (voucher == null || voucher.getStatus() == null || voucher.getStatus() != 1
                || voucher.getPointsCost() == null || voucher.getPointsCost() <= 0) {
            throw badRequest("Voucher không thể đổi");
        }
        LocalDateTime now = LocalDateTime.now();
        if ((voucher.getStartDate() != null && now.isBefore(voucher.getStartDate()))
                || (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate()))) {
            throw badRequest("Voucher chưa đến thời gian áp dụng hoặc đã hết hạn");
        }
        long rankPoints = value(account.getRankPoints());
        if (voucher.getMinTierId() != null) {
            Long threshold = dsl.select(LOYALTY_TIER.MIN_RANK_POINTS).from(LOYALTY_TIER)
                    .where(LOYALTY_TIER.ID.eq(voucher.getMinTierId())).fetchOne(LOYALTY_TIER.MIN_RANK_POINTS);
            if (threshold == null || rankPoints < threshold) throw badRequest("Hạng khách hàng chưa đủ để đổi voucher");
        }
        long balance = value(account.getBalance());
        if (balance < 0 || balance < voucher.getPointsCost()) throw badRequest("Số dư điểm không đủ");
        if (vouchers.increaseRedeemedIfInStock(voucherId) == 0) throw badRequest("Voucher đã hết lượt đổi");

        var inserted = dsl.insertInto(CUSTOMER_VOUCHER)
                .set(CUSTOMER_VOUCHER.CUSTOMER_ID, customerId)
                .set(CUSTOMER_VOUCHER.VOUCHER_ID, voucherId)
                .set(CUSTOMER_VOUCHER.STATUS, "AVAILABLE")
                .set(CUSTOMER_VOUCHER.POINTS_SPENT, voucher.getPointsCost())
                .set(CUSTOMER_VOUCHER.EXPIRES_AT, voucher.getEndDate())
                .returning(CUSTOMER_VOUCHER.ID)
                .fetchOne();
        if (inserted == null) throw badRequest("Không thể tạo voucher khách hàng");
        Integer customerVoucherId = inserted.getId();
        if (!ledger.spend(customerId, voucher.getPointsCost(), LoyaltyConstant.REF_CUSTOMER_VOUCHER,
                customerVoucherId.toString())) {
            throw badRequest("Yêu cầu đổi voucher đã được xử lý");
        }
        return customerVoucherId;
    }

    private long value(Long value) { return value == null ? 0L : value; }

    private AppException badRequest(String message) {
        return new AppException(message, ErrorCode.BAD_REQUEST.code);
    }
}
