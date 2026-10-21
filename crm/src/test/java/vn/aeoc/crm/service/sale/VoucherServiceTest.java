package vn.aeoc.crm.service.sale;

import org.junit.jupiter.api.Test;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.repository.sale.CustomerVoucherUsageRepository;
import vn.aeoc.crm.repository.sale.InvoiceVoucherRepository;
import vn.aeoc.crm.repository.sale.VoucherRepository;
import vn.aeoc.entity.data.mysql.sale.Invoice;
import vn.aeoc.entity.data.mysql.sale.Voucher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class VoucherServiceTest {
    @Test
    void evaluateCalculatesFixedDiscount() {
        VoucherService service = serviceWithVoucher(voucher(1, 25D, null, null, null), 0);
        assertEquals(25D, service.evaluate("SAVE", 100D, 7, null).discount());
    }

    @Test
    void evaluateRejectsExpiredExhaustedAndBelowMinimumVouchers() {
        Voucher expired = voucher(1, 10D, null, null, null);
        expired.setEndDate(java.time.LocalDateTime.now().minusMinutes(1));
        assertThrows(AppException.class, () -> serviceWithVoucher(expired, 0).evaluate("SAVE", 100D, 7, null));

        Voucher exhausted = voucher(1, 10D, null, null, 1);
        assertThrows(AppException.class, () -> serviceWithVoucher(exhausted, 1).evaluate("SAVE", 100D, 7, null));

        Voucher minimum = voucher(1, 10D, null, 200D, null);
        assertThrows(AppException.class, () -> serviceWithVoucher(minimum, 0).evaluate("SAVE", 100D, 7, null));
    }

    @Test
    void applyVoucherRetainsPercentageDiscountCapBehavior() {
        VoucherRepository voucherRepository = mock(VoucherRepository.class);
        InvoiceService invoiceService = mock(InvoiceService.class);
        InvoiceVoucherRepository invoiceVoucherRepository = mock(InvoiceVoucherRepository.class);
        CustomerVoucherUsageRepository usageRepository = mock(CustomerVoucherUsageRepository.class);
        VoucherService service = new VoucherService(voucherRepository, new QueryExecutor(Runnable::run),
                invoiceService, invoiceVoucherRepository, usageRepository);

        Voucher voucher = new Voucher();
        voucher.setId(3);
        voucher.setStatus(1);
        voucher.setDiscountType(2);
        voucher.setDiscountValue(50D);
        voucher.setMaxDiscount(20D);
        Invoice invoice = new Invoice();
        invoice.setAmount(100D);
        invoice.setBranchId(7);
        invoice.setCustomerId(null);
        when(voucherRepository.getByCode("SAVE")).thenReturn(voucher);
        when(invoiceService.getById(8)).thenReturn(invoice);
        when(invoiceVoucherRepository.countTotalUsed(3)).thenReturn(0);

        assertEquals(20D, service.applyVoucher(8, "SAVE"));
        verify(usageRepository, never()).getUsedCount(anyInt(), anyInt());
    }

    private VoucherService serviceWithVoucher(Voucher voucher, int used) {
        VoucherRepository voucherRepository = mock(VoucherRepository.class);
        InvoiceService invoiceService = mock(InvoiceService.class);
        InvoiceVoucherRepository invoiceVoucherRepository = mock(InvoiceVoucherRepository.class);
        CustomerVoucherUsageRepository usageRepository = mock(CustomerVoucherUsageRepository.class);
        when(voucherRepository.getByCode("SAVE")).thenReturn(voucher);
        when(invoiceVoucherRepository.countTotalUsed(voucher.getId())).thenReturn(used);
        return new VoucherService(voucherRepository, new QueryExecutor(Runnable::run), invoiceService,
                invoiceVoucherRepository, usageRepository);
    }

    private Voucher voucher(int discountType, double discountValue, Double maxDiscount,
                            Double minimum, Integer totalQuantity) {
        Voucher voucher = new Voucher();
        voucher.setId(3);
        voucher.setStatus(1);
        voucher.setDiscountType(discountType);
        voucher.setDiscountValue(discountValue);
        voucher.setMaxDiscount(maxDiscount);
        voucher.setMinInvoiceAmount(minimum);
        voucher.setTotalQuantity(totalQuantity);
        return voucher;
    }
}
