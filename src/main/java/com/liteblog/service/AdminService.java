package com.liteblog.service;

import com.liteblog.dto.LoginResponse;

public interface AdminService {

    LoginResponse login(String username, String password);

    boolean resetPassword(String username, String email, String newPassword);
}
