package com.liteblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.liteblog.dto.LoginResponse;
import com.liteblog.entity.Admin;
import com.liteblog.mapper.AdminMapper;
import com.liteblog.service.AdminService;
import com.liteblog.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public AdminServiceImpl(AdminMapper adminMapper, JwtUtil jwtUtil) {
        this.adminMapper = adminMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginResponse login(String username, String password) {
        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", username));
        if (admin == null || !passwordEncoder.matches(password, admin.getPassword())) {
            return null;
        }

        String token = jwtUtil.generateToken(admin.getUsername());
        return new LoginResponse(token, admin.getUsername());
    }

    @Override
    public boolean resetPassword(String username, String email, String newPassword) {
        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>()
                .eq("username", username)
                .eq("email", email));
        if (admin == null) {
            return false;
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        return adminMapper.updateById(admin) > 0;
    }
}
