package vn.aeoc.crm.constant;

/*
 * Constant definitions for customer segmentation rule version and segment labels.
 */
public final class CustomerSegmentConstant {

    private CustomerSegmentConstant() {
    }

    public static final String RULE_VERSION_RFM_V1 = "RFM_V1";

    public static final String SEGMENT_VIP = "VIP";
    public static final String SEGMENT_LOYAL = "LOYAL";
    public static final String SEGMENT_POTENTIAL = "POTENTIAL";
    public static final String SEGMENT_AT_RISK = "AT_RISK";
    public static final String SEGMENT_CHURNED = "CHURNED";

    public static final String JOB_MODE_FULL = "FULL";
    public static final String JOB_STATUS_RUNNING = "RUNNING";
    public static final String JOB_STATUS_SUCCESS = "SUCCESS";
    public static final String JOB_STATUS_FAILED = "FAILED";
    public static final String JOB_STATUS_PAUSED = "PAUSED";
}
