package com.liteblog.mapper;

import com.liteblog.entity.Admin;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class MapperSmokeTest {

    @Autowired
    private AdminMapper adminMapper;

    @Test
    void shouldLoadDefaultAdmin() {
        Admin admin = adminMapper.selectById(1);
        Assertions.assertNotNull(admin, "Expected default admin to exist; run schema.sql before testing.");
    }
}
