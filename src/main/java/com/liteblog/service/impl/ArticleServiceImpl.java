package com.liteblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteblog.dto.ArticleCreateRequest;
import com.liteblog.dto.ArticleUpdateRequest;
import com.liteblog.dto.TagVO;
import com.liteblog.entity.Article;
import com.liteblog.mapper.ArticleMapper;
import com.liteblog.service.ArticleService;
import com.liteblog.service.TagService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleServiceImpl implements ArticleService {

    private static final Duration VIEW_DEDUP_TTL = Duration.ofHours(1);

    private final ArticleMapper articleMapper;
    private final StringRedisTemplate redisTemplate;
    private final TagService tagService;

    public ArticleServiceImpl(ArticleMapper articleMapper, StringRedisTemplate redisTemplate, TagService tagService) {
        this.articleMapper = articleMapper;
        this.redisTemplate = redisTemplate;
        this.tagService = tagService;
    }

    @Override
    public Page<Article> listPublished(int page, int size, String category, String keyword, String sort, Long tagId) {
        Page<Article> result = new Page<>(page, size);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (StringUtils.hasText(category)) {
            wrapper.eq("category", category);
        }
        if (tagId != null) {
            wrapper.apply(
                    "EXISTS (SELECT 1 FROM article_tag at WHERE at.article_id = article.id AND at.tag_id = {0})",
                    tagId
            );
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("title", keyword)
                    .or()
                    .like("category", keyword)
                    .or()
                    .apply(
                            "EXISTS (SELECT 1 FROM article_tag at JOIN tag t ON at.tag_id = t.id " +
                                    "WHERE at.article_id = article.id AND t.name LIKE {0})",
                            "%" + keyword + "%"
                    ));
        }
        if ("popular".equals(sort)) {
            wrapper.orderByDesc("view_count");
        } else {
            wrapper.orderByDesc("created_at");
        }
        Page<Article> pageResult = articleMapper.selectPage(result, wrapper);
        hydrateTags(pageResult.getRecords());
        return pageResult;
    }

    @Override
    public List<String> listPublishedCategories() {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT category")
                .eq("status", 1)
                .isNotNull("category")
                .ne("category", "")
                .orderByAsc("category");
        return articleMapper.selectObjs(wrapper).stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public Article getPublishedById(Long id) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id).eq("status", 1).last("LIMIT 1");
        Article article = articleMapper.selectOne(wrapper);
        if (article == null) {
            return null;
        }
        hydrateTags(Collections.singletonList(article));

        String ip = getClientIp();
        if (!StringUtils.hasText(ip)) {
            return article;
        }

        String dedupKey = String.format("view:dedup:%d:%s", id, ip);
        try {
            Boolean firstVisit = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", VIEW_DEDUP_TTL);
            if (Boolean.TRUE.equals(firstVisit)) {
                int updated = articleMapper.update(
                        null,
                        new UpdateWrapper<Article>()
                                .eq("id", id)
                                .eq("status", 1)
                                .setSql("view_count = view_count + 1")
                );
                if (updated > 0) {
                    article.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to update article view count with dedup. articleId={}, ip={}", id, ip, ex);
        }

        return article;
    }

    @Override
    public Page<Article> listAdmin(int page, int size, String category, Integer status) {
        Page<Article> result = new Page<>(page, size);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(category)) {
            wrapper.eq("category", category);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_at");
        Page<Article> pageResult = articleMapper.selectPage(result, wrapper);
        hydrateTags(pageResult.getRecords());
        return pageResult;
    }

    @Override
    public Article getById(Long id) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            hydrateTags(Collections.singletonList(article));
        }
        return article;
    }

    @Override
    @Transactional
    public Article create(ArticleCreateRequest request) {
        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setCategory(request.getCategory());
        article.setStatus(request.getStatus() == null ? 0 : request.getStatus());
        article.setViewCount(0);
        int inserted = articleMapper.insert(article);
        if (inserted <= 0) {
            return null;
        }
        tagService.replaceArticleTags(article.getId(), request.getTagIds());
        hydrateTags(Collections.singletonList(article));
        return article;
    }

    @Override
    @Transactional
    public boolean update(Long id, ArticleUpdateRequest request) {
        Article existing = articleMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        existing.setTitle(request.getTitle());
        existing.setContent(request.getContent());
        existing.setCategory(request.getCategory());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        boolean updated = articleMapper.updateById(existing) > 0;
        if (!updated) {
            return false;
        }
        tagService.replaceArticleTags(id, request.getTagIds());
        return true;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        Article existing = articleMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        existing.setStatus(status);
        return articleMapper.updateById(existing) > 0;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        tagService.replaceArticleTags(id, Collections.emptyList());
        return articleMapper.deleteById(id) > 0;
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            for (String part : forwarded.split(",")) {
                String candidate = part.trim();
                if (!candidate.isEmpty() && !"unknown".equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp) && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private void hydrateTags(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        List<Long> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());
        Map<Long, List<TagVO>> tagsByArticleId = tagService.mapTagsByArticleIds(articleIds);
        for (Article article : articles) {
            List<TagVO> tags = tagsByArticleId.getOrDefault(article.getId(), Collections.emptyList());
            article.setTags(tags);
            article.setTagIds(tags.stream().map(TagVO::getId).collect(Collectors.toList()));
        }
    }
}
