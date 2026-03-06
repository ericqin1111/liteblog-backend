package com.liteblog.service;

import com.liteblog.dto.AdminTagVO;
import com.liteblog.dto.TagVO;
import com.liteblog.entity.Tag;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TagService {

    List<TagVO> listAll();

    List<AdminTagVO> listAllForAdmin();

    Tag create(String rawName);

    boolean exists(Long id);

    long countArticlesByTagId(Long id);

    boolean delete(Long id);

    void replaceArticleTags(Long articleId, List<Long> tagIds);

    Map<Long, List<TagVO>> mapTagsByArticleIds(Collection<Long> articleIds);

    int migrateLegacyTags();
}
