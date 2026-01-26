package com.liteblog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteblog.dto.OverviewStats;
import com.liteblog.dto.PopularArticleDTO;
import com.liteblog.dto.RecentVisitDTO;

import java.util.List;

public interface StatisticsService {

    OverviewStats getOverview();

    List<PopularArticleDTO> getPopularArticles(int limit);

    Page<RecentVisitDTO> getRecentVisits(int page, int size);
}
