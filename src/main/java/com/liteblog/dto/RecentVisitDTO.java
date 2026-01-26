package com.liteblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecentVisitDTO {

    private Long id;

    private String uri;

    private String ip;

    private LocalDateTime time;

    private Long articleId;
}
