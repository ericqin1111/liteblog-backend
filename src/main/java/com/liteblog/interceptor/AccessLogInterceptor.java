package com.liteblog.interceptor;

import com.liteblog.entity.AccessLog;
import com.liteblog.service.AccessLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Pattern ARTICLE_ID_PATTERN = Pattern.compile("/api/articles/(\\d+)");

    private final AccessLogService accessLogService;
    private final Executor accessLogExecutor;

    @Autowired
    public AccessLogInterceptor(AccessLogService accessLogService,
                                @Qualifier("accessLogExecutor") Executor accessLogExecutor) {
        this.accessLogService = accessLogService;
        this.accessLogExecutor = accessLogExecutor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = getClientIp(request);
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/error")) {
            return true;
        }
        if (uri != null && uri.startsWith("/api/health")) {
            return true;
        }
        String userAgent = request.getHeader("User-Agent");
        Long articleId = resolveArticleId(uri);

        log.info("Access: IP={}, URI={}, UserAgent={}", ip, uri, userAgent);

        AccessLog accessLog = new AccessLog();
        accessLog.setIpAddress(ip);
        accessLog.setUri(uri);
        accessLog.setUserAgent(userAgent);
        accessLog.setArticleId(articleId);
        accessLog.setAccessTime(LocalDateTime.now());

        accessLogExecutor.execute(() -> {
            try {
                accessLogService.save(accessLog);
            } catch (Exception ex) {
                log.warn("Failed to persist access log", ex);
            }
        });

        return true;
    }

    private Long resolveArticleId(String uri) {
        Matcher matcher = ARTICLE_ID_PATTERN.matcher(uri);
        if (matcher.matches()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
