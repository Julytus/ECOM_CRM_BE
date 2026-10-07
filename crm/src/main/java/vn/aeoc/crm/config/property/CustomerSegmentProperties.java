package vn.aeoc.crm.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.segment")
public class CustomerSegmentProperties {

    private boolean enabled = true;
    private String fullCron = "0 0 1 * * ?";

    private double recencyWeight = 0.4;
    private double frequencyWeight = 0.3;
    private double monetaryWeight = 0.3;

    private double vipScoreThreshold = 4.2;
    private double loyalScoreThreshold = 3.4;
    private double potentialScoreThreshold = 2.5;

    private int churnRecencyDays = 180;

    private double monetaryTier5 = 50000000;
    private double monetaryTier4 = 20000000;
    private double monetaryTier3 = 7000000;
    private double monetaryTier2 = 1000000;

    /**
     * Chunk size for recompute to prevent OOM on large datasets.
     */
    private int recomputeChunkSize = 5000;

    /**
     * Small sleep between chunks to reduce CPU contention.
     */
    private int recomputeChunkSleepMs = 50;
}
