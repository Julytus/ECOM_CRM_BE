package vn.aeoc.crm.config.web;

//import io.micrometer.core.instrument.Counter;
//import io.micrometer.core.instrument.MeterRegistry;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import vn.aeoc.crm.config.property.RateLimitProperties;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class SoftRateLimitInterceptor implements HandlerInterceptor {

    private static final String[] PROTECTED_PATHS = {
            "/chat/search",
            "/message/search",
            "/report/",
            "/invoice/getRevenue",
            "/invoice/getRevenueByHourToday"
    };

    private final RateLimitProperties properties;
//    private final MeterRegistry meterRegistry;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            Object handler
    ) {
        if (!properties.isEnabled()) {
            return true;
        }

        String path = request.getRequestURI();
        if (!isProtectedPath(path)) {
            return true;
        }

        String key = request.getRemoteAddr() + "::" + path;
        long now = Instant.now().toEpochMilli();
        long windowMs = properties.getWindow().toMillis();

        WindowCounter counter = counters.compute(key, (k, old) -> {
            if (old == null || now - old.windowStartMs >= windowMs) {
                return new WindowCounter(now, 1);
            }
            old.count++;
            return old;
        });

        if (counter != null && counter.count > properties.getMaxRequests()) {
//            Counter.builder("http.ratelimit.rejected")
//                    .tag("path", normalizePath(path))
//                    .register(meterRegistry)
//                    .increment();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests, please retry in a moment");
        }

        return true;
    }

    private boolean isProtectedPath(String path) {
        for (String protectedPath : PROTECTED_PATHS) {
            if (path.startsWith(protectedPath)) {
                return true;
            }
        }
        return false;
    }

    private String normalizePath(String path) {
        return path == null ? "unknown" : path.replaceAll("/\\d+", "/{id}");
    }

    private static class WindowCounter {
        private long windowStartMs;
        private int count;

        private WindowCounter(long windowStartMs, int count) {
            this.windowStartMs = windowStartMs;
            this.count = count;
        }
    }
}
