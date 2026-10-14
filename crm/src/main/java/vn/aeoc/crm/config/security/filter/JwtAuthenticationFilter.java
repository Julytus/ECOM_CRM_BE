package vn.aeoc.crm.config.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.aeoc.base.config.web.PrincipalRequestWrapper;
import vn.aeoc.base.constant.ErrorCode;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.util.HttpResponseUtils;
import vn.aeoc.crm.service.auth.JWTService;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JWTService jwtService;

    // Danh sách các API không cần kiểm tra token
    private static final List<String> WHITE_LIST_API = Arrays.asList(
            "/user/create",
            "/user/authenticate",
            "/v3/api-docs",
            "/v3/api-docs.yaml",
            "/swagger-ui.html"
            // Thêm các đường dẫn khác vào đây nếu cần
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (isWhiteListed(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("x-api-key");
        if (apiKey != null && !apiKey.isBlank() && jwtService.isValidApiKey(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractTokenFromRequest(request);

        try {
            if (token == null) {
                HttpResponseUtils.writeError(response, request, ErrorCode.LOGIN);
                return;
            }

            if (jwtService.isTokenExpired(token)) {
                HttpResponseUtils.writeError(response, request, ErrorCode.TOKEN_EXPIRED);
                return;
            }

            Principal principal = jwtService.extractPrincipal(token, request);
            if (principal == null) {
                HttpResponseUtils.writeError(response, request, ErrorCode.UNAUTHORIZED);
                return;
            }
            request = new PrincipalRequestWrapper(request, principal);

            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            HttpResponseUtils.writeError(response, request, ErrorCode.TOKEN_EXPIRED);
            return;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            HttpResponseUtils.writeError(response, request, ErrorCode.UNAUTHORIZED);
            return;
        } catch (AppException e) {
            HttpResponseUtils.writeError(response, request, e.getStatusCode(), e.getMessage());
            return;
        }
    }

    private boolean isWhiteListed(String requestUri) {
        return WHITE_LIST_API.contains(requestUri)
                || requestUri.startsWith("/swagger-ui/")
                || requestUri.startsWith("/v3/api-docs/");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
