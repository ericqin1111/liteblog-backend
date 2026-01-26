package com.liteblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liteblog.dto.PopularArticleDTO;
import com.liteblog.entity.AccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AccessLogMapper extends BaseMapper<AccessLog> {

    @Select("SELECT COUNT(*) FROM access_log")
    Long countTotalPv();

    @Select("SELECT COUNT(DISTINCT ip_address) FROM access_log")
    Long countDistinctUv();

    @Select("""
            SELECT COUNT(*)
            FROM access_log
            WHERE access_time >= CURDATE()
              AND access_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
            """)
    Long countTodayVisits();

    @Select("""
            SELECT al.article_id AS id,
                   a.title AS title,
                   COUNT(*) AS view_count,
                   a.created_at AS created_at
            FROM access_log al
            LEFT JOIN article a ON al.article_id = a.id
            WHERE al.article_id IS NOT NULL
            GROUP BY al.article_id, a.title, a.created_at
            ORDER BY view_count DESC
            LIMIT #{limit}
            """)
    List<PopularArticleDTO> selectPopularArticles(@Param("limit") int limit);
}
