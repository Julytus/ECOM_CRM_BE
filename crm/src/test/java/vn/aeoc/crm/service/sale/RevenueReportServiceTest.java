package vn.aeoc.crm.service.sale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.data.report.RevenueBucket;
import vn.aeoc.crm.data.report.RevenueSummary;
import vn.aeoc.crm.repository.sale.ReportRepository;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RevenueReportServiceTest {
    private ReportRepository repository;
    private RevenueReportService service;

    @BeforeEach
    void setUp() {
        repository = mock(ReportRepository.class);
        ThreeLayerCacheTemplate cache = mock(ThreeLayerCacheTemplate.class);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(2)).get())
                .when(cache).getOrLoad(anyString(), any(Duration.class), any(Supplier.class));
        service = new RevenueReportService(repository, cache);
    }

    @Test
    void summaryCalculatesAveragesAndChangesFromCurrentAndPreviousPeriodTotals() {
        RevenueSummary fixture = new RevenueSummary();
        fixture.setTotalRevenue(120.0);
        fixture.setInvoiceCount(2L);
        fixture.setDiscountTotal(10.0);
        fixture.setShippingTotal(8.0);
        fixture.setUnpaidAmount(40.0);
        fixture.setCancelledCount(1L);
        fixture.setRefundedAmount(25.0);
        fixture.setPreviousTotalRevenue(80.0);
        fixture.setPreviousInvoiceCount(4L);
        when(repository.getRevenueSummary(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 11), 7))
                .thenReturn(fixture);

        RevenueSummary result = service.getSummary(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 11), 7);

        assertEquals(60.0, result.getAvgOrderValue());
        assertEquals(20.0, result.getPreviousAvgOrderValue());
        assertEquals(50.0, result.getTotalRevenueChangePercent());
        assertEquals(-50.0, result.getInvoiceCountChangePercent());
        assertEquals(120.0, result.getTotalRevenue());
        assertEquals(2L, result.getInvoiceCount());
        assertEquals(10.0, result.getDiscountTotal());
        assertEquals(8.0, result.getShippingTotal());
        assertEquals(40.0, result.getUnpaidAmount());
        assertEquals(1L, result.getCancelledCount());
        assertEquals(25.0, result.getRefundedAmount());
        verify(repository).getRevenueSummary(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 11), 7);
    }

    @Test
    void seriesZeroFillsEveryDayIncludingDaysWithoutDoneInvoices() {
        LocalDate from = LocalDate.of(2026, 1, 10);
        LocalDate to = LocalDate.of(2026, 1, 12);
        when(repository.getRevenueSeries(from, to, 3, "day"))
                .thenReturn(List.of(new RevenueBucket("2026-01-10", 55.0, 1L)));

        List<RevenueBucket> result = service.getSeries(from, to, 3, "day");

        assertEquals(List.of("2026-01-10", "2026-01-11", "2026-01-12"),
                result.stream().map(RevenueBucket::getBucket).toList());
        assertEquals(55.0, result.get(0).getRevenue());
        assertEquals(0.0, result.get(1).getRevenue());
        assertEquals(0L, result.get(2).getInvoiceCount());
    }

    @Test
    void seriesZeroFillsIsoWeeksAcrossYearBoundary() {
        LocalDate from = LocalDate.of(2025, 12, 29);
        LocalDate to = LocalDate.of(2026, 1, 11);
        when(repository.getRevenueSeries(from, to, 1, "week")).thenReturn(List.of());

        List<RevenueBucket> result = service.getSeries(from, to, 1, "week");

        assertEquals(List.of("2026-W01", "2026-W02"), result.stream().map(RevenueBucket::getBucket).toList());
    }

    @Test
    void rejectsInvalidDateRangesAndUnsupportedSeriesGroups() {
        assertThrows(AppException.class, () -> service.getSummary(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), 1));
        assertThrows(AppException.class, () -> service.getSummary(
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2), 1));
        assertThrows(AppException.class, () -> service.getSeries(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 1, "year"));
    }
}
