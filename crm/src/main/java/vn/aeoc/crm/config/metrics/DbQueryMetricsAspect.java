//package vn.aeoc.crm.config.metrics;
//
//import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.Timer;
//import java.util.concurrent.TimeUnit;
//import lombok.RequiredArgsConstructor;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.springframework.stereotype.Component;
//
//@Aspect
//@Component
//@RequiredArgsConstructor
//public class DbQueryMetricsAspect {
//
//    private final MeterRegistry meterRegistry;
//
//    @Around("execution(* vn.aeoc.crm.repository..*(..))")
//    public Object measureRepositoryExecution(ProceedingJoinPoint joinPoint) throws Throwable {
//        long started = System.nanoTime();
//        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
//        String methodName = joinPoint.getSignature().getName();
//        String status = "success";
//
//        try {
//            return joinPoint.proceed();
//        } catch (Throwable throwable) {
//            status = "error";
//            throw throwable;
//        } finally {
//            long elapsedNs = System.nanoTime() - started;
//            Timer.builder("db.query.latency")
//                    .description("Repository query latency")
//                    .publishPercentiles(0.95, 0.99)
//                    .tag("repository", className)
//                    .tag("method", methodName)
//                    .tag("status", status)
//                    .register(meterRegistry)
//                    .record(elapsedNs, TimeUnit.NANOSECONDS);
//        }
//    }
//}
