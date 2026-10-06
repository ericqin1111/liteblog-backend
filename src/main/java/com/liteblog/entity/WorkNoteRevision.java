package com.liteblog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "work_note_revision", autoResultMap = true)
public class WorkNoteRevision {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workNoteId;
    private String title;
    private String mainProblem;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode content;
    private Long version;
    private LocalDateTime createdAt;
}
