package com.liteblog.dto;

import lombok.Data;

@Data
public class OverviewStats {

    private Long todayPv;

    private Long totalArticles;

    private Long todayUv;

    private Long weekUv;
}
