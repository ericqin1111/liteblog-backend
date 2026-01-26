package com.liteblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liteblog.entity.AccessLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {
}
