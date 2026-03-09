package com.liteblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteblog.dto.DailyTrendDTO;
import com.liteblog.dto.IpRecordDTO;
import com.liteblog.dto.OverviewStats;
import com.liteblog.dto.PopularArticleDTO;
import com.liteblog.dto.RecentVisitDTO;
import com.liteblog.dto.VisitorDetailDTO;
import com.liteblog.dto.VisitorSummaryDTO;
import com.liteblog.entity.AccessLog;
import com.liteblog.mapper.AccessLogMapper;
import com.liteblog.mapper.ArticleMapper;
import com.liteblog.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final AccessLogMapper accessLogMapper;
    private final ArticleMapper articleMapper;

    @Autowired
    public StatisticsServiceImpl(AccessLogMapper accessLogMapper, ArticleMapper articleMapper) {
        this.accessLogMapper = accessLogMapper;
        this.articleMapper = articleMapper;
    }

    @Override
    public OverviewStats getOverview() {
        Long todayPv = accessLogMapper.countTodayPv();
        Long todayUv = accessLogMapper.countTodayUv();
        Long weekUv = accessLogMapper.countWeekUv();
        Long totalArticles = articleMapper.selectCount(new QueryWrapper<>());

        OverviewStats stats = new OverviewStats();
        stats.setTodayPv(todayPv);
        stats.setTodayUv(todayUv);
        stats.setWeekUv(weekUv);
        stats.setTotalArticles(totalArticles);
        return stats;
    }

    @Override
    public List<PopularArticleDTO> getPopularArticles(int limit) {
        return accessLogMapper.selectPopularArticles(limit);
    }

    @Override
    public Page<RecentVisitDTO> getRecentVisits(int page, int size) {
        Page<AccessLog> result = accessLogMapper.selectPage(
                new Page<>(page, size),
                new QueryWrapper<AccessLog>().orderByDesc("access_time")
        );

        List<RecentVisitDTO> records = result.getRecords().stream()
                .map(this::mapToRecentVisit)
                .collect(Collectors.toList());

        Page<RecentVisitDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(records);
        return dtoPage;
    }

    @Override
    public Page<IpRecordDTO> getIpRecords(int page, int size) {
        long offset = (long) (page - 1) * size;
        List<IpRecordDTO> records = accessLogMapper.selectIpRecords(offset, size);
        long total = accessLogMapper.countIpRecordGroups();

        Page<IpRecordDTO> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    @Override
    public List<DailyTrendDTO> getDailyTrend(int days) {
        // 传入 days-1 使 SQL 的 INTERVAL N DAY 查询恰好覆盖近 days 天（含今天）
        return accessLogMapper.selectDailyTrend(Math.max(days - 1, 0));
    }

    @Override
    public Page<VisitorSummaryDTO> getRecentVisitors(int page, int size) {
        long offset = (long) (page - 1) * size;
        List<VisitorSummaryDTO> records = accessLogMapper.selectRecentVisitors(offset, size);
        long total = accessLogMapper.countVisitors();
        Page<VisitorSummaryDTO> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    @Override
    public List<VisitorDetailDTO> getVisitorDetail(String ip, int limit) {
        return accessLogMapper.selectVisitorDetail(ip, limit);
    }

    private RecentVisitDTO mapToRecentVisit(AccessLog log) {
        RecentVisitDTO dto = new RecentVisitDTO();
        dto.setId(log.getId());
        dto.setUri(log.getUri());
        dto.setIp(log.getIpAddress());
        dto.setTime(log.getAccessTime());
        dto.setArticleId(log.getArticleId());
        return dto;
    }
}
