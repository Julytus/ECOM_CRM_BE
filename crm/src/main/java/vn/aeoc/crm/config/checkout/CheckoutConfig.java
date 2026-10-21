package vn.aeoc.crm.config.checkout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CheckoutConfig {

    @Value("${app.checkout.shipping-fee:30000}")
    public double shippingFee;

    @Value("${app.checkout.default-branch-id:0}")
    public int defaultBranchId;
}
