package com.liteblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("access_log")
public class AccessLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ipAddress;

    private String uri;

    private String userAgent;

    private Long articleId;

    private LocalDateTime accessTime;
}
