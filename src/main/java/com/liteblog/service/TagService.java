package com.liteblog.service;

import com.liteblog.dto.TagVO;
import com.liteblog.entity.Tag;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TagService {

    List<TagVO> listAll();

    Tag create(String rawName);

    boolean delete(Long id);

    void replaceArticleTags(Long articleId, List<Long> tagIds);

    Map<Long, List<TagVO>> mapTagsByArticleIds(Collection<Long> articleIds);

    int migrateLegacyTags();
}
