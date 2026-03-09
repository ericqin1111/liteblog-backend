package com.liteblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitorSummaryDTO {

    private String ipAddress;

    private LocalDateTime lastVisit;

    private Long visitCount;

    private Long articleCount;
}
