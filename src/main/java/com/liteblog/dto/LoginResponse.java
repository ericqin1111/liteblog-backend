package com.liteblog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * 兼容旧前端字段
     */
    private String token;

    /**
     * 新字段：访问令牌
     */
    private String accessToken;

    private String username;
}
