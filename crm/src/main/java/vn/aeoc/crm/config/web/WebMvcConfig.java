package vn.aeoc.crm.config.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

//    private final ApiMetricsInterceptor apiMetricsInterceptor;
    private final SoftRateLimitInterceptor softRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(apiMetricsInterceptor).addPathPatterns("/**");
        registry.addInterceptor(softRateLimitInterceptor).addPathPatterns("/**");
    }
}
