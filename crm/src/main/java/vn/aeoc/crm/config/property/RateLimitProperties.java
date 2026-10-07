package vn.aeoc.crm.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /**
     * Max requests per key within window.
     */
    private int maxRequests = 120;

    /**
     * Sliding window duration.
     */
    private Duration window = Duration.ofSeconds(30);
}
