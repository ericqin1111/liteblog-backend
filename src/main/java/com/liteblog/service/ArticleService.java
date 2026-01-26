package com.liteblog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteblog.dto.ArticleCreateRequest;
import com.liteblog.dto.ArticleUpdateRequest;
import com.liteblog.entity.Article;

public interface ArticleService {

    Page<Article> listPublished(int page, int size, String category);

    Article getPublishedById(Long id);

    Article create(ArticleCreateRequest request);

    boolean update(Long id, ArticleUpdateRequest request);

    boolean updateStatus(Long id, Integer status);

    boolean delete(Long id);
}
