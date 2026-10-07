//package vn.aeoc.crm.config.web;
//
//import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.Timer;
//import java.util.concurrent.TimeUnit;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.HandlerInterceptor;
//
//@Component
//@RequiredArgsConstructor
//public class ApiMetricsInterceptor implements HandlerInterceptor {
//
//    private static final String START_TIME_ATTR = "apiMetricsStartNs";
//
//    private final MeterRegistry meterRegistry;
//
//    @Override
//    public boolean preHandle(
//            jakarta.servlet.http.HttpServletRequest request,
//            jakarta.servlet.http.HttpServletResponse response,
//            Object handler
//    ) {
//        request.setAttribute(START_TIME_ATTR, System.nanoTime());
//        return true;
//    }
//
//    @Override
//    public void afterCompletion(
//            jakarta.servlet.http.HttpServletRequest request,
//            jakarta.servlet.http.HttpServletResponse response,
//            Object handler,
//            Exception ex
//    ) {
//        Object startObj = request.getAttribute(START_TIME_ATTR);
//        if (!(startObj instanceof Long startedNs)) {
//            return;
//        }
//
//        long elapsedNs = System.nanoTime() - startedNs;
//        String path = normalizePath(request.getRequestURI());
//        String method = request.getMethod();
//        String status = String.valueOf(response.getStatus());
//
//        Timer.builder("http.api.latency")
//                .description("API latency by endpoint")
//                .publishPercentiles(0.95, 0.99)
//                .tag("path", path)
//                .tag("method", method)
//                .tag("status", status)
//                .register(meterRegistry)
//                .record(elapsedNs, TimeUnit.NANOSECONDS);
//    }
//
//    private String normalizePath(String path) {
//        return path == null ? "unknown" : path.replaceAll("/\\d+", "/{id}");
//    }
//}
