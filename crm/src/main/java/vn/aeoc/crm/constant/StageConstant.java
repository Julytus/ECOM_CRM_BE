package vn.aeoc.crm.constant;

/**
 * Opportunity Stage Constant
 */
public final class StageConstant {

    private StageConstant() {}

    /** Cơ hội mới tạo */
    public static final int LEAD = 1;

    /** Sale đã liên hệ khách */
    public static final int CONTACTED = 2;

    /** Đang tư vấn */
    public static final int CONSULTING = 3;

    /** Đã gửi đề xuất/báo giá */
    public static final int PROPOSAL = 4;

    /** Đang đàm phán */
    public static final int NEGOTIATION = 5;

    /** Đã chốt sale */
    public static final int WON = 6;

    /** Mất cơ hội */
    public static final int LOST = 7;

}