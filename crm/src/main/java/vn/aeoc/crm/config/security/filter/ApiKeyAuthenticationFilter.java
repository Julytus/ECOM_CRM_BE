package vn.aeoc.crm.config.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.aeoc.base.config.web.PrincipalRequestWrapper;
import vn.aeoc.crm.config.security.principal.impl.SimpleAuthentication;
import vn.aeoc.crm.config.security.principal.impl.SimplePrincipal;
import vn.aeoc.crm.service.auth.JWTService;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private final JWTService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String apiKey = request.getHeader("x-api-key");
        return apiKey == null || apiKey.isBlank() || !jwtService.isValidApiKey(apiKey);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        request = new PrincipalRequestWrapper(request, buildApiKeyPrincipal(request));
        filterChain.doFilter(request, response);
    }

    private Principal buildApiKeyPrincipal(HttpServletRequest request) {
        Map<String, String> clientInfo = new HashMap<>();
        clientInfo.put("User-Agent", request.getHeader("User-Agent"));
        clientInfo.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        clientInfo.put("X-Real-IP", request.getHeader("X-Real-IP"));
        clientInfo.put("x-api-key", request.getHeader("x-api-key"));

        return SimplePrincipal.builder()
                .authentication(new SimpleAuthentication(Collections.singletonList(1)))
                .otherInfo(JWTService.PrincipalType.API_KEY_OPERATOR)
                .id(-1)
                .principalType(JWTService.PrincipalType.API_KEY_OPERATOR)
                .clientInfo(clientInfo)
                .build();
    }
}
