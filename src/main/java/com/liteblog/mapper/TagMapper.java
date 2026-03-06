package com.liteblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liteblog.dto.AdminTagVO;
import com.liteblog.dto.ArticleTagRowDTO;
import com.liteblog.entity.Tag;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select({
            "<script>",
            "SELECT at.article_id AS articleId, t.id AS id, t.name AS name",
            "FROM article_tag at",
            "JOIN tag t ON at.tag_id = t.id",
            "WHERE at.article_id IN",
            "<foreach collection='articleIds' item='articleId' open='(' separator=',' close=')'>",
            "#{articleId}",
            "</foreach>",
            "ORDER BY t.name ASC",
            "</script>"
    })
    List<ArticleTagRowDTO> selectTagsByArticleIds(@Param("articleIds") List<Long> articleIds);

    @Insert("INSERT IGNORE INTO tag(name) VALUES(#{name})")
    int insertIgnoreByName(@Param("name") String name);

    @Select({
            "SELECT t.id AS id, t.name AS name, COUNT(at.article_id) AS articleCount",
            "FROM tag t",
            "LEFT JOIN article_tag at ON at.tag_id = t.id",
            "GROUP BY t.id, t.name",
            "ORDER BY t.name ASC"
    })
    List<AdminTagVO> selectAdminTags();

    @Select("SELECT COUNT(1) FROM article_tag WHERE tag_id = #{tagId}")
    long countArticleByTagId(@Param("tagId") Long tagId);

}
