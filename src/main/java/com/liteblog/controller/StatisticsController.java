package com.liteblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteblog.dto.OverviewStats;
import com.liteblog.dto.PopularArticleDTO;
import com.liteblog.dto.RecentVisitDTO;
import com.liteblog.service.StatisticsService;
import com.liteblog.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Autowired
    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/overview")
    public ResponseUtil<OverviewStats> overview() {
        return ResponseUtil.success(statisticsService.getOverview());
    }

    @GetMapping("/popular-articles")
    public ResponseUtil<List<PopularArticleDTO>> popularArticles(@RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return ResponseUtil.success(statisticsService.getPopularArticles(safeLimit));
    }

    @GetMapping("/recent-visits")
    public ResponseUtil<?> recentVisits(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 10 : Math.min(size, 50);
        Page<RecentVisitDTO> result = statisticsService.getRecentVisits(safePage, safeSize);
        return ResponseUtil.page(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }
}
