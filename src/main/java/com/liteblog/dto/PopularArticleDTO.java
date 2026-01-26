package com.liteblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PopularArticleDTO {

    private Long id;

    private String title;

    private Long viewCount;

    private LocalDateTime createdAt;
}
