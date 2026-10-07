package vn.aeoc.crm.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "security.api-key")
public class ApiKeyProperties {
    /**
     * Shared API key used by trusted internal callers.
     */
    private String value;
}
