
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JWTService jwtService;

    /**
     * Các API không cần xác thực JWT.
     * Các đường dẫn được khai báo tương đối với context path.
     */
    private static final List<String> WHITE_LIST_API = List.of(
            "/user/create",
            "/user/authenticate"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Bỏ qua preflight request của CORS
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                && request.getHeader("Origin") != null
                && request.getHeader("Access-Control-Request-Method") != null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (isWhiteListed(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("x-api-key");

        if (apiKey != null
                && !apiKey.isBlank()
                && jwtService.isValidApiKey(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractTokenFromRequest(request);

        try {
            if (token == null) {
                HttpResponseUtils.writeError(
                        response, request, ErrorCode.LOGIN
                );
                return;
            }

            if (jwtService.isTokenExpired(token)) {
                HttpResponseUtils.writeError(
                        response, request, ErrorCode.TOKEN_EXPIRED
                );
                return;
            }

            Principal principal = jwtService.extractPrincipal(token, request);

            if (principal == null) {
                HttpResponseUtils.writeError(
                        response, request, ErrorCode.UNAUTHORIZED
                );
                return;
            }

            request = new PrincipalRequestWrapper(request, principal);
            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            HttpResponseUtils.writeError(
                    response, request, ErrorCode.TOKEN_EXPIRED
            );
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            HttpResponseUtils.writeError(
                    response, request, ErrorCode.UNAUTHORIZED
            );
        } catch (AppException e) {
            HttpResponseUtils.writeError(
                    response, request,
                    e.getStatusCode(), e.getMessage()
            );
        }
    }

    private boolean isWhiteListed(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        // Ví dụ: /api/swagger-ui/swagger-ui.css
        // Sau chuẩn hóa: /swagger-ui/swagger-ui.css
        String path = requestUri.substring(contextPath.length());

        return WHITE_LIST_API.contains(path)
                || path.equals("/swagger-ui.html")
                || path.equals("/swagger-ui/index.html")
                || path.startsWith("/swagger-ui/")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/v3/api-docs.yaml");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
