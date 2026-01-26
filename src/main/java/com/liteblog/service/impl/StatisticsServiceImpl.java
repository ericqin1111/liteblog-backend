package com.liteblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteblog.dto.OverviewStats;
import com.liteblog.dto.PopularArticleDTO;
import com.liteblog.dto.RecentVisitDTO;
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
        Long pv = accessLogMapper.countTotalPv();
        Long uv = accessLogMapper.countDistinctUv();
        Long today = accessLogMapper.countTodayVisits();
        Long totalArticles = articleMapper.selectCount(new QueryWrapper<>());

        OverviewStats stats = new OverviewStats();
        stats.setPv(pv);
        stats.setUv(uv);
        stats.setToday(today);
        stats.setTotalArticles(totalArticles);
        stats.setTotalPv(pv);
        stats.setTotalUv(uv);
        stats.setTodayVisits(today);
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
