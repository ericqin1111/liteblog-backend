package com.liteblog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteblog.dto.DailyTrendDTO;
import com.liteblog.dto.IpRecordDTO;
import com.liteblog.dto.OverviewStats;
import com.liteblog.dto.PopularArticleDTO;
import com.liteblog.dto.RecentVisitDTO;
import com.liteblog.dto.VisitorDetailDTO;
import com.liteblog.dto.VisitorSummaryDTO;

import java.util.List;

public interface StatisticsService {

    OverviewStats getOverview();

    List<PopularArticleDTO> getPopularArticles(int limit);

    Page<RecentVisitDTO> getRecentVisits(int page, int size);

    Page<IpRecordDTO> getIpRecords(int page, int size);

    List<DailyTrendDTO> getDailyTrend(int days);

    Page<VisitorSummaryDTO> getRecentVisitors(int page, int size);

    List<VisitorDetailDTO> getVisitorDetail(String ip, int limit);
}
