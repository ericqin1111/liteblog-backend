package com.liteblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteblog.dto.ArticleCreateRequest;
import com.liteblog.dto.ArticleUpdateRequest;
import com.liteblog.entity.Article;
import com.liteblog.mapper.ArticleMapper;
import com.liteblog.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;

    @Autowired
    public ArticleServiceImpl(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    public Page<Article> listPublished(int page, int size, String category) {
        Page<Article> result = new Page<>(page, size);
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (StringUtils.hasText(category)) {
            wrapper.eq("category", category);
        }
        wrapper.orderByDesc("created_at");
        return articleMapper.selectPage(result, wrapper);
    }

    @Override
    public Article getPublishedById(Long id) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id).eq("status", 1);
        return articleMapper.selectOne(wrapper);
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
        return articleMapper.selectPage(result, wrapper);
    }

    @Override
    public Article getById(Long id) {
        return articleMapper.selectById(id);
    }

    @Override
    public Article create(ArticleCreateRequest request) {
        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setCategory(request.getCategory());
        article.setTags(request.getTags());
        article.setStatus(request.getStatus() == null ? 0 : request.getStatus());
        article.setViewCount(0);
        int inserted = articleMapper.insert(article);
        return inserted > 0 ? article : null;
    }

    @Override
    public boolean update(Long id, ArticleUpdateRequest request) {
        Article existing = articleMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        existing.setTitle(request.getTitle());
        existing.setContent(request.getContent());
        existing.setCategory(request.getCategory());
        existing.setTags(request.getTags());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        return articleMapper.updateById(existing) > 0;
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
    public boolean delete(Long id) {
        return articleMapper.deleteById(id) > 0;
    }
}
