package vn.aeoc.crm.util;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import vn.aeoc.base.config.async.AsyncExecutor;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.config.cache.CacheTemplateProperties;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QueryExecutorThreadTest {

    @Test
    void dbExecutorIsPrimaryTaskExecutor() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AsyncExecutor.class)) {
            assertSame(context.getBean("dbExecutor"), context.getBean(TaskExecutor.class));
        }
    }

    @Test
    void parallel3To5RunSuppliersOnDbExecutor() {
        ThreadPoolTaskExecutor executor = newExecutor();
        try {
            QueryExecutor queryExecutor = new QueryExecutor(executor);

            var three = queryExecutor.parallel3(this::threadName, this::threadName, this::threadName);
            var four = queryExecutor.parallel4(
                    this::threadName, this::threadName, this::threadName, this::threadName);
            var five = queryExecutor.parallel5(
                    this::threadName, this::threadName, this::threadName,
                    this::threadName, this::threadName);

            assertDbThread(three.v1());
            assertDbThread(three.v2());
            assertDbThread(three.v3());
            assertDbThread(four.v1());
            assertDbThread(four.v2());
            assertDbThread(four.v3());
            assertDbThread(four.v4());
            assertDbThread(five.v1());
            assertDbThread(five.v2());
            assertDbThread(five.v3());
            assertDbThread(five.v4());
            assertDbThread(five.v5());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void cacheLoaderRunsOnInjectedCacheLoaderExecutor() {
        ThreadPoolTaskExecutor executor = newExecutor("test-cache-load-");
        try {
            Cache<String, Object> l1Cache = mock(Cache.class);
            RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
            ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
            CacheTemplateProperties properties = new CacheTemplateProperties();
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("cache-key")).thenReturn(null);
            when(valueOperations.setIfAbsent(eq("cache:lock:cache-key"), eq("1"), any(Duration.class)))
                    .thenReturn(true);

            ThreeLayerCacheTemplate cache = new ThreeLayerCacheTemplate(
                    l1Cache, redisTemplate, properties, executor);
            String loaded = cache.getOrLoad("cache-key", this::threadName);

            assertTrue(loaded.startsWith("test-cache-load-"),
                    () -> "Expected cache loader thread, got: " + loaded);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void concurrentCacheLoadersCanUseDbExecutorWithoutDeadlock() throws Exception {
        ThreadPoolTaskExecutor dbExecutor = newExecutor();
        ThreadPoolTaskExecutor cacheLoaderExecutor = newExecutor("test-cache-load-");
        ExecutorService callers = Executors.newFixedThreadPool(16);
        try {
            Cache<String, Object> l1Cache = mock(Cache.class);
            RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
            ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);
            when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                    .thenReturn(true);

            QueryExecutor queryExecutor = new QueryExecutor(dbExecutor);
            ThreeLayerCacheTemplate cache = new ThreeLayerCacheTemplate(
                    l1Cache, redisTemplate, new CacheTemplateProperties(), cacheLoaderExecutor);
            CountDownLatch allLoadersStarted = new CountDownLatch(16);
            List<Future<String>> results = new ArrayList<>();

            for (int i = 0; i < 16; i++) {
                String cacheKey = "cache-key-" + i;
                results.add(callers.submit(() -> cache.getOrLoad(cacheKey, () -> {
                    allLoadersStarted.countDown();
                    try {
                        assertTrue(allLoadersStarted.await(3, TimeUnit.SECONDS),
                                "All 16 cache loaders should start concurrently");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                    var queryThreads = queryExecutor.parallel2(this::threadName, this::threadName);
                    return queryThreads.v1() + "," + queryThreads.v2();
                })));
            }

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            for (Future<String> result : results) {
                long remaining = deadline - System.nanoTime();
                assertTrue(remaining > 0, "Cache loads and nested queries should finish within 3 seconds");
                String queryThreads = result.get(remaining, TimeUnit.NANOSECONDS);
                for (String threadName : queryThreads.split(",")) {
                    assertDbThread(threadName);
                }
            }
        } finally {
            callers.shutdownNow();
            cacheLoaderExecutor.shutdown();
            dbExecutor.shutdown();
        }
    }

    private ThreadPoolTaskExecutor newExecutor() {
        return newExecutor("test-db-");
    }

    private ThreadPoolTaskExecutor newExecutor(String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }

    private String threadName() {
        return Thread.currentThread().getName();
    }

    private void assertDbThread(String name) {
        assertTrue(name.startsWith("test-db-"), () -> "Expected test-db thread, got: " + name);
    }
}
