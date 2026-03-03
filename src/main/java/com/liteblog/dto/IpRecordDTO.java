package com.liteblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpRecordDTO {

    private String ipAddress;

    private Long totalVisits;

    private Long articleCount;

    private LocalDateTime firstVisit;

    private LocalDateTime lastVisit;
}
