package com.liteblog.service.impl;

import com.liteblog.entity.AccessLog;
import com.liteblog.mapper.AccessLogMapper;
import com.liteblog.service.AccessLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessLogServiceImpl implements AccessLogService {

    private final AccessLogMapper accessLogMapper;

    @Autowired
    public AccessLogServiceImpl(AccessLogMapper accessLogMapper) {
        this.accessLogMapper = accessLogMapper;
    }

    @Override
    public void save(AccessLog log) {
        accessLogMapper.insert(log);
    }
}
