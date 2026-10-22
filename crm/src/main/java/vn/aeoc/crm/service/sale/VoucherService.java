package vn.aeoc.crm.service.sale;

import org.springframework.stereotype.Service;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.service.BaseService;
import vn.aeoc.base.util.CommonUtils;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.base.util.Tuple2;
import vn.aeoc.crm.repository.sale.CustomerVoucherUsageRepository;
import vn.aeoc.crm.repository.sale.InvoiceVoucherRepository;
import vn.aeoc.crm.repository.sale.VoucherRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Voucher;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoucherService extends BaseService<VoucherRepository, Voucher, Integer> {

    private final QueryExecutor queryExecutor;
    private final InvoiceService invoiceService;
    private final InvoiceVoucherRepository invoiceVoucherRepository;
    private final CustomerVoucherUsageRepository customerVoucherUsageRepository;

    protected VoucherService(VoucherRepository repository, QueryExecutor queryExecutor,
                             InvoiceService invoiceService, InvoiceVoucherRepository invoiceVoucherRepository,
                             CustomerVoucherUsageRepository customerVoucherUsageRepository) {
        super(repository, queryExecutor);
        this.queryExecutor = queryExecutor;
        this.invoiceService = invoiceService;
        this.invoiceVoucherRepository = invoiceVoucherRepository;
        this.customerVoucherUsageRepository = customerVoucherUsageRepository;
    }

    public Page<Voucher> getList(String keyword, Integer status, Integer branchId, Pageable pageable) {
        List<Voucher> list = repository.getByCriteria(keyword, status, branchId, pageable);
        Long total = repository.countByCriteria(keyword, status, branchId);

        return pageQuery(
                pageable,
                () -> list,
                () -> total
        );
    }

    @Override
    public Voucher insert(Voucher voucher) {
        if (repository.existByCode(voucher.getCode())) throw new AppException(ErrorCode.EXIST_VOUCHER);
        return super.insert(voucher);
    }

    @Override
    public Voucher update(Integer id , Voucher voucher) {
        if (repository.existByCodeExceptVoucherId(id, voucher.getCode())) throw new AppException(ErrorCode.EXIST_VOUCHER);
        return super.update(id, voucher);
    }

    public Voucher getByCode(String code) {
        return repository.getByCode(code);
    }

    public Double applyVoucher(Integer invoiceId, String voucherCode) {


        Tuple2<Voucher, Invoice> tuple2 = queryExecutor.parallel2(
                () -> repository.getByCode(voucherCode),
                () -> invoiceService.getById(invoiceId)
        );

        Voucher voucher = tuple2.v1();
        Invoice invoice = tuple2.v2();

        return evaluate(voucher, invoice.getAmount(), invoice.getBranchId(), invoice.getCustomerId()).discount();
    }

    public VoucherEvaluation evaluate(String code, double amount, Integer branchId, Integer customerId) {
        Voucher voucher = repository.getByCode(code);
        return evaluate(voucher, amount, branchId, customerId);
    }

    public Voucher lockByCode(String code) {
        return repository.lockByCode(code);
    }

    public VoucherEvaluation evaluateLocked(Voucher voucher, double amount, Integer branchId, Integer customerId) {
        return evaluate(voucher, amount, branchId, customerId);
    }

    public VoucherEvaluation evaluateRedeemed(Integer voucherId, double amount, Integer branchId) {
        Voucher voucher = repository.getById(voucherId);
        Invoice invoice = new Invoice();
        invoice.setAmount(amount);
        invoice.setBranchId(branchId);
        validateVoucher(invoice, voucher, false);
        return new VoucherEvaluation(voucher, calculateDiscount(amount, voucher));
    }

    private VoucherEvaluation evaluate(Voucher voucher, Double amount, Integer branchId, Integer customerId) {
        Invoice invoice = new Invoice();
        invoice.setAmount(amount);
        invoice.setBranchId(branchId);
        invoice.setCustomerId(customerId);
        validateVoucher(invoice, voucher, true);
        return new VoucherEvaluation(voucher, calculateDiscount(amount, voucher));
    }

    public record VoucherEvaluation(Voucher voucher, double discount) {}

    private Double calculateDiscount(Double invoiceAmount, Voucher voucher) {

        if (invoiceAmount == null || invoiceAmount <= 0) {
            return 0D;
        }

        if (voucher == null) {
            return 0D;
        }

        double discount;

        // 1 = FIXED AMOUNT, 2 = PERCENT
        switch (voucher.getDiscountType()) {

            case 1: // FIXED AMOUNT
                discount = voucher.getDiscountValue();
                break;

            case 2: // PERCENT
                discount = invoiceAmount * voucher.getDiscountValue() / 100D;

                // áp max_discount nếu có
                if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount() > 0) {
                    discount = Math.min(discount, voucher.getMaxDiscount());
                }
                break;

            default:
                discount = 0D;
        }

        // không cho giảm vượt quá tiền hóa đơn
        discount = Math.min(discount, invoiceAmount);

        // tránh số âm
        return Math.max(discount, 0D);
    }
    private void validateVoucher(
            Invoice invoice,
            Voucher voucher,
            boolean checkUsageLimits
    ) {
        if (voucher == null) {
            throw new AppException("Voucher không tồn tại");
        }

        // 1. Trạng thái voucher
        if (voucher.getStatus() == null || voucher.getStatus() != 1) {
            throw new AppException("Voucher đã bị vô hiệu hóa");
        }

        // 2. Thời gian hiệu lực
        LocalDateTime now = LocalDateTime.now();

        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new AppException("Voucher chưa đến thời gian áp dụng");
        }

        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            throw new AppException("Voucher đã hết hạn");
        }

        // 3. Chi nhánh
        if (voucher.getBranchId() != null
                && voucher.getBranchId() > 0
                && !voucher.getBranchId().equals(invoice.getBranchId())) {
            throw new AppException("Voucher không áp dụng cho chi nhánh này");
        }

        // 4. Giá trị đơn tối thiểu
        if (voucher.getMinInvoiceAmount() != null
                && invoice.getAmount() < voucher.getMinInvoiceAmount()) {
            throw new AppException(
                    "Hóa đơn tối thiểu phải là: " + CommonUtils.formatVnd(voucher.getMinInvoiceAmount())
            );
        }

        if (checkUsageLimits) {
            // 5. Giới hạn tổng lượt sử dụng (toàn hệ thống)
            Integer totalUsedCount = invoiceVoucherRepository.countTotalUsed(voucher.getId());
            if (voucher.getTotalQuantity() != null && voucher.getTotalQuantity() > 0) {
                if (totalUsedCount >= voucher.getTotalQuantity()) {
                    throw new AppException("Voucher đã hết lượt sử dụng");
                }
            }

            // 6. Giới hạn lượt sử dụng theo customer, khách vãng lai không cần check
            if (invoice.getCustomerId() != null && invoice.getCustomerId() > 0) {
                Integer customerUsedCount = customerVoucherUsageRepository.getUsedCount(
                        voucher.getId(), invoice.getCustomerId());

                if (voucher.getPerUserLimit() != null && voucher.getPerUserLimit() > 0) {
                    if (customerUsedCount >= voucher.getPerUserLimit()) {
                        throw new AppException("Khách hàng đã quá số lần sử dụng voucher này");
                    }
                }
            }
        }
    }
}
