package com.liteblog.controller;

import com.liteblog.dto.LoginRequest;
import com.liteblog.dto.LoginResponse;
import com.liteblog.dto.ResetPasswordRequest;
import com.liteblog.service.AdminService;
import com.liteblog.service.RefreshTokenService;
import com.liteblog.util.JwtUtil;
import com.liteblog.util.ResponseUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AdminService adminService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${jwt.refresh-cookie-same-site:Lax}")
    private String refreshCookieSameSite;

    @Value("${jwt.refresh-cookie-path:/api/auth}")
    private String refreshCookiePath;

    @Autowired
    public AuthController(AdminService adminService, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.adminService = adminService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseUtil<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                             HttpServletResponse servletResponse) {
        String sessionId = UUID.randomUUID().toString();
        LoginResponse response = adminService.login(request.getUsername(), request.getPassword(), sessionId);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseUtil.error(401, "用户名或密码错误"));
        }

        String refreshJti = UUID.randomUUID().toString();
        String refreshToken = jwtUtil.generateRefreshToken(response.getUsername(), sessionId, refreshJti);
        refreshTokenService.store(refreshJti, response.getUsername(), sessionId, jwtUtil.getRefreshExpiration());
        writeRefreshCookie(servletResponse, refreshToken, false);
        return ResponseEntity.ok(ResponseUtil.success("登录成功", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseUtil<LoginResponse>> refresh(HttpServletRequest request,
                                                               HttpServletResponse response) {
        String refreshToken = readCookie(request, REFRESH_COOKIE_NAME);
        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            writeRefreshCookie(response, "", true);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseUtil.error(401, "刷新令牌无效"));
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        String sessionId = jwtUtil.getSessionId(refreshToken);
        String currentJti = jwtUtil.getJti(refreshToken);
        if (currentJti == null || !refreshTokenService.isValid(currentJti, username, sessionId)) {
            writeRefreshCookie(response, "", true);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseUtil.error(401, "刷新令牌已失效"));
        }

        refreshTokenService.revoke(currentJti);
        String newJti = UUID.randomUUID().toString();
        String newRefreshToken = jwtUtil.generateRefreshToken(username, sessionId, newJti);
        refreshTokenService.store(newJti, username, sessionId, jwtUtil.getRefreshExpiration());
        writeRefreshCookie(response, newRefreshToken, false);

        String accessToken = jwtUtil.generateAccessToken(username, sessionId);
        LoginResponse result = new LoginResponse(accessToken, accessToken, username);
        return ResponseEntity.ok(ResponseUtil.success("刷新成功", result));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseUtil<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readCookie(request, REFRESH_COOKIE_NAME);
        if (refreshToken != null && jwtUtil.validateRefreshToken(refreshToken)) {
            String jti = jwtUtil.getJti(refreshToken);
            if (jti != null) {
                refreshTokenService.revoke(jti);
            }
        }
        writeRefreshCookie(response, "", true);
        return ResponseEntity.ok(ResponseUtil.success("退出成功", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResponseUtil<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean success = adminService.resetPassword(
                request.getUsername(),
                request.getEmail(),
                request.getNewPassword()
        );
        if (!success) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseUtil.error(400, "用户名或邮箱不匹配"));
        }
        return ResponseEntity.ok(ResponseUtil.success("密码重置成功", null));
    }

    private String readCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeRefreshCookie(HttpServletResponse response, String value, boolean clear) {
        Duration maxAge = clear ? Duration.ZERO : Duration.ofMillis(jwtUtil.getRefreshExpiration());
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(refreshCookiePath)
                .maxAge(maxAge)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
