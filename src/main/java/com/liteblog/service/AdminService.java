package com.liteblog.service;

import com.liteblog.dto.LoginResponse;

public interface AdminService {

    LoginResponse login(String username, String password);
}
