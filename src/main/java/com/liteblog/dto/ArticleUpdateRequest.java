package com.liteblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArticleUpdateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @Size(max = 50, message = "分类长度不能超过50")
    private String category;

    @Size(max = 255, message = "标签长度不能超过255")
    private String tags;

    private Integer status;
}
