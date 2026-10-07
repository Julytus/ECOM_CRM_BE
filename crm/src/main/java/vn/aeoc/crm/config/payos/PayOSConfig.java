package vn.aeoc.crm.config.payos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayOSConfig {

    @Value("${payos.client-id:}")
    public String clientId;

    @Value("${payos.api-key:}")
    public String apiKey;

    @Value("${payos.checksum-key:}")
    public String checksumKey;

    @Value("${app.frontend.base-url:https://dat.aeoc.io.vn}")
    public String baseUrl;

    @Value("${payos.api-url:https://api-merchant.payos.vn/v2/payment-requests}")
    public String apiUrl;

}
