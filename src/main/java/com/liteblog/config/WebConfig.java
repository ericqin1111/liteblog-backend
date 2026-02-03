package com.liteblog.config;

  import com.liteblog.interceptor.AccessLogInterceptor;
  import com.liteblog.interceptor.AuthInterceptor;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.web.cors.CorsConfiguration;
  import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
  import org.springframework.web.filter.CorsFilter;
  import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
  import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

  import java.util.Arrays;

  @Configuration
  public class WebConfig implements WebMvcConfigurer {

      @Autowired
      private AccessLogInterceptor accessLogInterceptor;

      @Autowired
      private AuthInterceptor authInterceptor;

      @Bean
      public CorsFilter corsFilter() {
          CorsConfiguration config = new CorsConfiguration();
          config.setAllowedOriginPatterns(Arrays.asList(
              "https://rootcserlog.me",
              "https://www.rootcserlog.me",
              "http://localhost:*",
              "http://127.0.0.1:*"
          ));
          config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS",
  "PATCH"));
          config.setAllowedHeaders(Arrays.asList("*"));
          config.setAllowCredentials(true);
          config.setMaxAge(3600L);

          UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
          source.registerCorsConfiguration("/**", config);
          return new CorsFilter(source);
      }

      @Override
      public void addInterceptors(InterceptorRegistry registry) {
          registry.addInterceptor(accessLogInterceptor)
                  .addPathPatterns("/**");
          registry.addInterceptor(authInterceptor)
                  .addPathPatterns("/admin/**")
                  .excludePathPatterns("/auth/**", "/health");
      }
  }
