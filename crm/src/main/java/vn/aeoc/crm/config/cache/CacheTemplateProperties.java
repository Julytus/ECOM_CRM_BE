package vn.aeoc.crm.config.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.cache.template")
public class CacheTemplateProperties {

    /**
     * Maximum number of entries in Caffeine L1 cache.
     */
    private long l1MaxSize = 50_000;

    /**
     * Local JVM cache TTL.
     */
    private Duration l1Ttl = Duration.ofMinutes(2);

    /**
     * Default Redis TTL for L2 cache entries.
     */
    private Duration redisTtl = Duration.ofMinutes(10);

    /**
     * Distributed lock lease time.
     */
    private Duration lockTtl = Duration.ofSeconds(10);

    /**
     * Max wait for another worker to finish DB load and publish to Redis.
     */
    private Duration waitTimeout = Duration.ofSeconds(2);

    /**
     * Polling delay while waiting for L2 value after lock contention.
     */
    private Duration waitPollInterval = Duration.ofMillis(60);

    /**
     * Timeout while waiting for local single-flight future.
     */
    private Duration singleFlightTimeout = Duration.ofSeconds(5);
}
