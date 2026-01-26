package com.liteblog.controller;

import com.liteblog.dto.LoginRequest;
import com.liteblog.dto.LoginResponse;
import com.liteblog.service.AdminService;
import com.liteblog.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AdminService adminService;

    @Autowired
    public AuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseUtil<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = adminService.login(request.getUsername(), request.getPassword());
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseUtil.error(401, "用户名或密码错误"));
        }
        return ResponseEntity.ok(ResponseUtil.success("登录成功", response));
    }
}
