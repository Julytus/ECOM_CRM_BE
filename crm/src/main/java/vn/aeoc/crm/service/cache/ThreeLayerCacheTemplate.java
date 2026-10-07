package vn.aeoc.crm.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import vn.aeoc.crm.config.cache.CacheTemplateProperties;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreeLayerCacheTemplate {

    private static final String LOCK_PREFIX = "cache:lock:";

    private final Cache<String, Object> l1Cache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheTemplateProperties properties;

    /**
     * Coalesce concurrent requests in the same JVM instance.
     */
    private final ConcurrentHashMap<String, CompletableFuture<Object>> inFlight = new ConcurrentHashMap<>();

    /*
     * Get data from L1 -> Redis -> DB loader with default Redis TTL.
     */
    public <T> T getOrLoad(String cacheKey, Supplier<T> dbLoader) {
        return getOrLoad(cacheKey, properties.getRedisTtl(), dbLoader);
    }

    /*
     * Get data from L1 -> Redis -> DB loader with custom Redis TTL.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String cacheKey, Duration redisTtl, Supplier<T> dbLoader) {
        Objects.requireNonNull(cacheKey, "cacheKey must not be null");
        Objects.requireNonNull(redisTtl, "redisTtl must not be null");
        Objects.requireNonNull(dbLoader, "dbLoader must not be null");

        Object l1Value = l1Cache.getIfPresent(cacheKey);
        if (l1Value != null) {
            return (T) l1Value;
        }

        Object l2Value = redisTemplate.opsForValue().get(cacheKey);
        if (l2Value != null) {
            l1Cache.put(cacheKey, l2Value);
            return (T) l2Value;
        }

        CompletableFuture<Object> future = inFlight.computeIfAbsent(cacheKey, key ->
                CompletableFuture.supplyAsync(() -> loadWithDistributedLock(key, redisTtl, dbLoader))
        );

        try {
            Object loaded = future.get(properties.getSingleFlightTimeout().toMillis(), TimeUnit.MILLISECONDS);
            return (T) loaded;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted while loading cache key: " + cacheKey, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Failed to load cache key: " + cacheKey, cause);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timed out loading cache key: " + cacheKey, e);
        } finally {
            inFlight.remove(cacheKey, future);
        }
    }

    /*
     * Remove one key from L1 and Redis.
     */
    public void evict(String cacheKey) {
        l1Cache.invalidate(cacheKey);
        redisTemplate.delete(cacheKey);
    }

    /*
     * Remove many keys from L1 and Redis.
     */
    public void evictAll(List<String> cacheKeys) {
        if (cacheKeys == null || cacheKeys.isEmpty()) {
            return;
        }
        l1Cache.invalidateAll(cacheKeys);
        redisTemplate.delete(cacheKeys);
    }

    public long increment(String key) {
        Objects.requireNonNull(key, "key must not be null");
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        Long value = ops.increment(key);
        if (value == null) {
            throw new IllegalStateException("Cannot increment cache key: " + key);
        }
        l1Cache.invalidate(key);
        return value;
    }

    public long getLong(String key, long defaultValue) {
        Objects.requireNonNull(key, "key must not be null");

        Object l1Value = l1Cache.getIfPresent(key);
        if (l1Value instanceof Number num) {
            return num.longValue();
        }

        Object l2Value = redisTemplate.opsForValue().get(key);
        if (l2Value instanceof Number num) {
            l1Cache.put(key, num.longValue());
            return num.longValue();
        }

        l1Cache.put(key, defaultValue);
        return defaultValue;
    }

    /*
     * Acquire distributed lock to avoid DB stampede across multiple instances.
     */
    private <T> Object loadWithDistributedLock(String cacheKey, Duration redisTtl, Supplier<T> dbLoader) {
        String lockKey = LOCK_PREFIX + cacheKey;

        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", properties.getLockTtl());
        if (Boolean.TRUE.equals(locked)) {
            try {
                Object checkAgain = redisTemplate.opsForValue().get(cacheKey);
                if (checkAgain != null) {
                    l1Cache.put(cacheKey, checkAgain);
                    return checkAgain;
                }

                T dbValue = dbLoader.get();
                if (dbValue != null) {
                    redisTemplate.opsForValue().set(cacheKey, dbValue, redisTtl);
                    l1Cache.put(cacheKey, dbValue);
                }
                return dbValue;
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        Object waitedValue = waitForRedis(cacheKey);
        if (waitedValue != null) {
            l1Cache.put(cacheKey, waitedValue);
            return waitedValue;
        }

        log.warn("Cache lock contention timeout for key={}, fallback to direct DB read", cacheKey);
        T dbValue = dbLoader.get();
        if (dbValue != null) {
            redisTemplate.opsForValue().set(cacheKey, dbValue, redisTtl);
            l1Cache.put(cacheKey, dbValue);
        }
        return dbValue;
    }

    /*
     * Wait for winner thread/instance to publish value into Redis.
     */
    private Object waitForRedis(String cacheKey) {
        long timeoutMs = properties.getWaitTimeout().toMillis();
        long pollMs = properties.getWaitPollInterval().toMillis();
        long startedAt = System.currentTimeMillis();

        while (System.currentTimeMillis() - startedAt < timeoutMs) {
            Object value = redisTemplate.opsForValue().get(cacheKey);
            if (value != null) {
                return value;
            }

            try {
                Thread.sleep(Math.max(5, pollMs));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
        }
        return null;
    }
}
