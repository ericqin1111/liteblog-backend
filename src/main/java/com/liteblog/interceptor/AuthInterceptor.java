package com.liteblog.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liteblog.util.JwtUtil;
import com.liteblog.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 请求直接放行
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || !jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            ResponseUtil<Object> result = ResponseUtil.error(401, "未授权，请先登录");
            response.getWriter().write(new ObjectMapper().writeValueAsString(result));
            return false;
        }

        // 将用户名存入请求属性，供 Controller 使用
        String username = jwtUtil.getUsernameFromToken(token);
        request.setAttribute("username", username);

        return true;
    }
}
