package vn.aeoc.crm.util;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.config.cache.CacheTemplateProperties;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QueryExecutorThreadTest {

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
    void cacheLoaderRunsOnInjectedDbExecutor() {
        ThreadPoolTaskExecutor executor = newExecutor();
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

            assertDbThread(loaded);
        } finally {
            executor.shutdown();
        }
    }

    private ThreadPoolTaskExecutor newExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("test-db-");
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
