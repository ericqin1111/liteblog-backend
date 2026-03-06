package com.liteblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.liteblog.dto.AdminTagVO;
import com.liteblog.dto.ArticleTagRowDTO;
import com.liteblog.dto.TagVO;
import com.liteblog.entity.Tag;
import com.liteblog.mapper.ArticleTagMapper;
import com.liteblog.mapper.TagMapper;
import com.liteblog.service.TagService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final JdbcTemplate jdbcTemplate;

    public TagServiceImpl(TagMapper tagMapper, ArticleTagMapper articleTagMapper, JdbcTemplate jdbcTemplate) {
        this.tagMapper = tagMapper;
        this.articleTagMapper = articleTagMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TagVO> listAll() {
        QueryWrapper<Tag> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("name");
        return tagMapper.selectList(wrapper).stream()
                .map(tag -> new TagVO(tag.getId(), tag.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminTagVO> listAllForAdmin() {
        return tagMapper.selectAdminTags();
    }

    @Override
    @Transactional
    public Tag create(String rawName) {
        String name = normalize(rawName);
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("标签名不能为空");
        }

        Tag existing = findByName(name);
        if (existing != null) {
            return existing;
        }

        tagMapper.insertIgnoreByName(name);
        Tag created = findByName(name);
        if (created == null) {
            throw new IllegalStateException("创建标签失败");
        }
        return created;
    }

    @Override
    public boolean exists(Long id) {
        return id != null && tagMapper.selectById(id) != null;
    }

    @Override
    public long countArticlesByTagId(Long id) {
        if (id == null) {
            return 0;
        }
        return tagMapper.countArticleByTagId(id);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        articleTagMapper.deleteByTagId(id);
        return tagMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public void replaceArticleTags(Long articleId, List<Long> tagIds) {
        List<Long> cleaned = tagIds == null ? Collections.emptyList() : tagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!cleaned.isEmpty()) {
            QueryWrapper<Tag> wrapper = new QueryWrapper<>();
            wrapper.in("id", cleaned);
            long count = tagMapper.selectCount(wrapper);
            if (count != cleaned.size()) {
                throw new IllegalArgumentException("标签不存在或已删除");
            }
        }

        articleTagMapper.deleteByArticleId(articleId);
        if (!cleaned.isEmpty()) {
            articleTagMapper.batchInsertIgnore(articleId, cleaned);
        }
    }

    @Override
    public Map<Long, List<TagVO>> mapTagsByArticleIds(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = articleIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ArticleTagRowDTO> rows = tagMapper.selectTagsByArticleIds(ids);
        Map<Long, List<TagVO>> result = new LinkedHashMap<>();
        for (ArticleTagRowDTO row : rows) {
            result.computeIfAbsent(row.getArticleId(), k -> new ArrayList<>())
                    .add(new TagVO(row.getId(), row.getName()));
        }
        return result;
    }

    @Override
    @Transactional
    public int migrateLegacyTags() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, tags FROM article WHERE tags IS NOT NULL AND TRIM(tags) <> ''"
        );
        int linkedCount = 0;
        for (Map<String, Object> row : rows) {
            Long articleId = ((Number) row.get("id")).longValue();
            String rawTags = Objects.toString(row.get("tags"), "");
            Set<Long> tagIds = new LinkedHashSet<>();
            for (String piece : rawTags.split(",")) {
                String name = normalize(piece);
                if (!StringUtils.hasText(name)) {
                    continue;
                }
                Tag tag = create(name);
                tagIds.add(tag.getId());
            }
            if (!tagIds.isEmpty()) {
                linkedCount += articleTagMapper.batchInsertIgnore(articleId, new ArrayList<>(tagIds));
            }
        }
        return linkedCount;
    }

    private Tag findByName(String name) {
        QueryWrapper<Tag> wrapper = new QueryWrapper<>();
        wrapper.eq("name", name).last("LIMIT 1");
        return tagMapper.selectOne(wrapper);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
