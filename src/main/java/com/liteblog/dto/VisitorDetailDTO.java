package com.liteblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitorDetailDTO {

    private String uri;

    private Long articleId;

    private String articleTitle;

    private LocalDateTime visitTime;
}
