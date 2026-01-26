package com.liteblog.dto;

import lombok.Data;

@Data
public class OverviewStats {

    private Long pv;

    private Long uv;

    private Long today;

    private Long totalArticles;

    private Long totalPv;

    private Long totalUv;

    private Long todayVisits;
}
