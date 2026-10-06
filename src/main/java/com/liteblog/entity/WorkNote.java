package com.liteblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "work_note", autoResultMap = true)
public class WorkNote {
    @TableId(type = IdType.AUTO)
    private Long id;
    @JsonIgnore
    private Integer ownerId;
    private String title;
    private String mainProblem;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode content;
    private Boolean archived;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
