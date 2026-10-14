package vn.aeoc.crm.service.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.aeoc.crm.config.property.ApiKeyProperties;
import vn.aeoc.crm.config.security.SimpleSecurityUser;
import vn.aeoc.crm.config.security.principal.impl.SimpleAuthentication;
import vn.aeoc.crm.config.security.principal.impl.SimplePrincipal;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JWTService {
    public static final class PrincipalType {
        public static final String JWT_USER = "JWT_USER";
        public static final String API_KEY_OPERATOR = "API_KEY_OPERATOR";
        public static final String ANONYMOUS = "ANONYMOUS";

        private PrincipalType() {
        }
    }

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expired-in}")
    private Long expiresIn;

    private final ApiKeyProperties apiKeyProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public Principal extractPrincipal(String token) {
        return extractPrincipal(token, null);
    }

    public Principal extractPrincipal(String token, HttpServletRequest request) {
        try {
            Claims body = Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userString = (String) body.get("user");
            SimpleSecurityUser user = objectMapper.readValue(userString, SimpleSecurityUser.class);
            SimpleAuthentication authentication = new SimpleAuthentication(user.getRoles());

            Map<String, String> clientInfo = buildClientInfo(request, true);
            return SimplePrincipal.builder()
                    .authentication(authentication)
                    .otherInfo(user)
                    .id(user.getId())
                    .principalType(PrincipalType.JWT_USER)
                    .clientInfo(clientInfo)
                    .build();
        } catch (Exception e) {
            return SimplePrincipal.builder()
                    .principalType(PrincipalType.ANONYMOUS)
                    .clientInfo(buildClientInfo(request, false))
                    .build();
        }
    }

    public Principal extractApiKeyPrincipal(HttpServletRequest request) {
        return SimplePrincipal.builder()
                .authentication(new SimpleAuthentication(Collections.singletonList(1)))
                .otherInfo(PrincipalType.API_KEY_OPERATOR)
                .id(-1)
                .principalType(PrincipalType.API_KEY_OPERATOR)
                .clientInfo(buildClientInfo(request, false))
                .build();
    }

    public boolean isValidApiKey(String headerValue) {
        String configuredApiKey = apiKeyProperties == null ? null : apiKeyProperties.getValue();
        return headerValue != null && configuredApiKey != null && !configuredApiKey.isBlank()
                && configuredApiKey.equals(headerValue);
    }

    /**
     * Kiểm tra token đã hết hạn hay chưa
     *
     * @param token
     * @return
     */
    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateJwt(SimpleSecurityUser securityUser) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("user", objectMapper.writeValueAsString(securityUser));

            return Jwts.builder()
                    .subject(securityUser.getId().toString())
                    .claims(claims)
                    .expiration(new Date(System.currentTimeMillis() + expiresIn))
                    .signWith(getSignInKey())
                    .compact();
        } catch (JsonProcessingException e) {
            log.error("Error building JWT: {}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, String> buildClientInfo(HttpServletRequest request, boolean includeAuthorization) {
        Map<String, String> clientInfo = new HashMap<>();
        if (request != null) {
            clientInfo.put("User-Agent", request.getHeader("User-Agent"));
            clientInfo.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
            clientInfo.put("X-Real-IP", request.getHeader("X-Real-IP"));
            if (includeAuthorization) {
                clientInfo.put("Authorization", request.getHeader("Authorization"));
            }
        }
        return clientInfo;
    }
}
