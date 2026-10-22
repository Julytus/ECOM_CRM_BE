package vn.aeoc.crm.controller.loyalty;

import org.junit.jupiter.api.Test;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.crm.config.security.principal.impl.SimplePrincipal;
import vn.aeoc.crm.data.request.RedeemRequest;
import vn.aeoc.crm.repository.customer.CustomerRepository;
import vn.aeoc.crm.service.loyalty.LoyaltyQueryService;
import vn.aeoc.crm.service.loyalty.LoyaltyRedeemService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LoyaltyControllerTest {
    @Test
    void resolvesCustomerFromPrincipalForMyAccount() {
        CustomerRepository customers = mock(CustomerRepository.class);
        LoyaltyQueryService queries = mock(LoyaltyQueryService.class);
        LoyaltyRedeemService redeemer = mock(LoyaltyRedeemService.class);
        when(customers.findCustomerIdByAccountUserId(91)).thenReturn(37);
        LoyaltyController controller = new LoyaltyController(customers, queries, redeemer);

        controller.my(principal(91));

        verify(queries).getMyAccount(37);
    }

    @Test
    void redeemUsesMappedCustomerAndRejectsUnmappedPrincipal() {
        CustomerRepository customers = mock(CustomerRepository.class);
        LoyaltyQueryService queries = mock(LoyaltyQueryService.class);
        LoyaltyRedeemService redeemer = mock(LoyaltyRedeemService.class);
        when(customers.findCustomerIdByAccountUserId(91)).thenReturn(37);
        LoyaltyController controller = new LoyaltyController(customers, queries, redeemer);

        controller.redeem(new RedeemRequest(12), principal(91));
        verify(redeemer).redeem(37, 12);
        clearInvocations(redeemer);
        when(customers.findCustomerIdByAccountUserId(92)).thenReturn(null);
        assertThrows(AppException.class, () -> controller.redeem(new RedeemRequest(12), principal(92)));
        verifyNoInteractions(redeemer);
    }

    private SimplePrincipal principal(int userId) {
        return SimplePrincipal.builder().id(userId).build();
    }
}
