package com.liteblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteblog.dto.DailyTrendDTO;
import com.liteblog.dto.IpRecordDTO;
import com.liteblog.dto.OverviewStats;
import com.liteblog.dto.PopularArticleDTO;
import com.liteblog.dto.RecentVisitDTO;
import com.liteblog.dto.VisitorDetailDTO;
import com.liteblog.dto.VisitorSummaryDTO;
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

    @GetMapping("/ip-records")
    public ResponseUtil<?> ipRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 20 : Math.min(size, 100);
        Page<IpRecordDTO> result = statisticsService.getIpRecords(safePage, safeSize);
        return ResponseUtil.page(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @GetMapping("/daily-trend")
    public ResponseUtil<List<DailyTrendDTO>> dailyTrend(@RequestParam(defaultValue = "7") int days) {
        int safeDays = days < 1 ? 7 : Math.min(days, 90);
        return ResponseUtil.success(statisticsService.getDailyTrend(safeDays));
    }

    @GetMapping("/visitors")
    public ResponseUtil<?> recentVisitors(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 20 : Math.min(size, 100);
        Page<VisitorSummaryDTO> result = statisticsService.getRecentVisitors(safePage, safeSize);
        return ResponseUtil.page(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @GetMapping("/visitor-detail")
    public ResponseUtil<List<VisitorDetailDTO>> visitorDetail(
            @RequestParam String ip,
            @RequestParam(defaultValue = "50") int limit) {
        int safeLimit = limit < 1 ? 50 : Math.min(limit, 200);
        return ResponseUtil.success(statisticsService.getVisitorDetail(ip, safeLimit));
    }
}
