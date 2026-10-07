package vn.aeoc.crm.service.cache;

public final class CacheKeyFactory {

    private CacheKeyFactory() {
    }

    public static String revenueToday(Integer branchId) {
        return "dashboard:revenue:today:branch:" + branchId;
    }

    public static String revenueToday(Integer branchId, Long version) {
        return revenueToday(branchId) + ":version:" + (version == null ? 0L : version);
    }

    public static String revenueHourToday(Integer branchId) {
        return "dashboard:revenue:hourly:today:branch:" + branchId;
    }

    public static String revenueHourToday(Integer branchId, Long version) {
        return revenueHourToday(branchId) + ":version:" + (version == null ? 0L : version);
    }

    public static String reportCustomerByMonth(Integer branchId, Integer year) {
        return "report:customer:by-month:branch:" + branchId + ":year:" + year;
    }

    public static String reportCustomerBySource(Integer branchId, Integer year) {
        return "report:customer:by-source:branch:" + branchId + ":year:" + year;
    }

    public static String reportMonthlyRevenue(Integer branchId, Integer year, Integer month) {
        return "report:invoice:monthly-revenue:branch:" + branchId + ":year:" + year + ":month:" + month;
    }

    public static String reportInvoiceFrequency(Integer branchId, Integer year, Integer month, Integer page, Integer size) {
        return "report:invoice:frequency:branch:" + branchId + ":year:" + year + ":month:" + month
                + ":page:" + page + ":size:" + size;
    }

    public static String reportInvoiceFrequencyCount(Integer branchId, Integer year, Integer month) {
        return "report:invoice:frequency:count:branch:" + branchId + ":year:" + year + ":month:" + month;
    }

    public static String reportAvgInterest(Integer branchId, String keyword, Integer page, Integer size) {
        return "report:call-history:avg-interest:branch:" + branchId + ":keyword:" + (keyword == null ? "" : keyword)
                + ":page:" + page + ":size:" + size;
    }

    public static String reportAvgInterestCount(Integer branchId, String keyword) {
        return "report:call-history:avg-interest:count:branch:" + branchId + ":keyword:" + (keyword == null ? "" : keyword);
    }

    public static String reportInterestBar(Integer branchId, Integer year, Double minAvgInterest) {
        return "report:call-history:interest-bar:branch:" + branchId + ":year:" + year + ":min:" + minAvgInterest;
    }

    public static String invoiceRecalculate(Integer invoiceId, Long version) {
        return "invoice:draft:recalc:invoice:" + invoiceId + ":version:" + (version == null ? "0" : version);
    }

    public static String notificationUnreadCount(Integer userId, Integer branchId) {
        return "notification:unread-count:user:" + userId + ":branch:" + branchId;
    }

    public static String segmentSummary(Integer branchId) {
        return "segment:summary:branch:" + branchId;
    }

    public static String customerListWithExtra(Integer branchId, String keyword, String segmentCode, Integer page, Integer size, Long version) {
        return "customer:list:extra:branch:" + branchId
                + ":keyword:" + (keyword == null ? "" : keyword.trim().toLowerCase())
                + ":segment:" + (segmentCode == null ? "" : segmentCode.trim().toUpperCase())
                + ":page:" + page
                + ":size:" + size
                + ":version:" + (version == null ? 0L : version);
    }

    public static String customerListExtraVersion(Integer branchId) {
        return "customer:list:extra:version:branch:" + branchId;
    }

    public static String dashboardRevenueVersion(Integer branchId) {
        return "dashboard:revenue:version:branch:" + branchId;
    }
}
