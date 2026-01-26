package com.liteblog.config;

import com.liteblog.interceptor.AccessLogInterceptor;
import com.liteblog.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AccessLogInterceptor accessLogInterceptor;

    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * CORS 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 注册拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 访问日志拦截器（拦截所有请求）
        registry.addInterceptor(accessLogInterceptor)
                .addPathPatterns("/**");

        // 认证拦截器（仅拦截 /admin/** 路径）
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/auth/**", "/health");
    }
}
