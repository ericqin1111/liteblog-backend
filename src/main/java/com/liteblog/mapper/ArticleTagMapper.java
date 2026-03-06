package com.liteblog.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArticleTagMapper {

    @Delete("DELETE FROM article_tag WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);

    @Delete("DELETE FROM article_tag WHERE tag_id = #{tagId}")
    int deleteByTagId(@Param("tagId") Long tagId);

    @Insert({
            "<script>",
            "INSERT IGNORE INTO article_tag(article_id, tag_id) VALUES",
            "<foreach collection='tagIds' item='tagId' separator=','>",
            "(#{articleId}, #{tagId})",
            "</foreach>",
            "</script>"
    })
    int batchInsertIgnore(@Param("articleId") Long articleId, @Param("tagIds") List<Long> tagIds);
}
