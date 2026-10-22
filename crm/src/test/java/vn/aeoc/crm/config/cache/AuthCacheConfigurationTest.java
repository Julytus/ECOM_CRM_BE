package vn.aeoc.crm.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import vn.aeoc.crm.service.cache.ThreeLayerCacheTemplate;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AuthCacheConfigurationTest {

    @Test
    void bothCaffeineCachesCanBeInjectedAlongsideThreeLayerTemplate() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                CacheInfraConfig.class,
                AuthCacheConfig.class,
                CacheTemplateProperties.class,
                ThreeLayerCacheTemplate.class,
                TestDependencies.class
        )) {
            Cache<String, Object> l1Cache = context.getBean("l1CaffeineCache", Cache.class);
            Cache<String, Object> authCache = context.getBean("authCache", Cache.class);

            assertNotNull(context.getBean(ThreeLayerCacheTemplate.class));
            assertNotSame(l1Cache, authCache);
        }
    }

    @Configuration
    static class TestDependencies {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }

        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> mockCacheRedisTemplate() {
            return (RedisTemplate<String, Object>) mock(RedisTemplate.class);
        }

        @Bean("cacheLoaderExecutor")
        Executor cacheLoaderExecutor() {
            return Runnable::run;
        }
    }
}
