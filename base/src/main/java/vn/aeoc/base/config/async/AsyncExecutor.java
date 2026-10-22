package vn.aeoc.base.config.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncExecutor {

    @Bean("cacheLoaderExecutor")
    public Executor cacheLoaderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("cache-load-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("dbExecutor")
    public Executor dbExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setAllowCoreThreadTimeOut(true);

        executor.setThreadNamePrefix("db-exec-");

        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();

        return executor;
    }

}
