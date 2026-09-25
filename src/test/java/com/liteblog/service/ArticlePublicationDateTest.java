package com.liteblog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.liteblog.dto.ArticleCreateRequest;
import com.liteblog.dto.ArticleUpdateRequest;
import com.liteblog.entity.Article;
import com.liteblog.mapper.ArticleMapper;
import com.liteblog.service.impl.ArticleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticlePublicationDateTest {
    @Mock private ArticleMapper mapper;
    @Mock private StringRedisTemplate redis;
    @Mock private TagService tags;
    private ArticleServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArticleServiceImpl(mapper, redis, tags);
    }

    @Test
    void canPublishWithHistoricalDateWithoutChangingCreationTimestamp() {
        when(mapper.insert(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            article.setId(1L);
            return 1;
        });
        ArticleCreateRequest request = new ArticleCreateRequest();
        request.setTitle("补发作品");
        request.setContent("过去写的作品");
        request.setStatus(1);
        request.setPublishedDate(LocalDate.of(2024, 2, 29));

        Article article = service.create(request);

        assertEquals(LocalDate.of(2024, 2, 29), article.getPublishedDate());
        assertEquals(1, article.getStatus());
        assertNull(article.getCreatedAt(), "Creation timestamp remains managed by the database");
    }

    @Test
    void oldClientsCanCreateWithoutProvidingDate() {
        when(mapper.insert(any(Article.class))).thenReturn(1);
        LocalDate before = LocalDate.now();
        Article article = service.create(new ArticleCreateRequest());
        assertFalse(article.getPublishedDate().isBefore(before));
        assertFalse(article.getPublishedDate().isAfter(LocalDate.now()));
    }

    @Test
    void editingPublishedDatePreservesCreationTimeAndStatus() {
        Article existing = existingArticle();
        LocalDateTime createdAt = existing.getCreatedAt();
        when(mapper.selectById(1L)).thenReturn(existing);
        when(mapper.updateById(any(Article.class))).thenReturn(1);
        ArticleUpdateRequest request = new ArticleUpdateRequest();
        request.setTitle("编辑作品");
        request.setContent("正文");
        request.setPublishedDate(LocalDate.of(2025, 6, 15));

        assertTrue(service.update(1L, request));

        ArgumentCaptor<Article> saved = ArgumentCaptor.forClass(Article.class);
        verify(mapper).updateById(saved.capture());
        assertEquals(LocalDate.of(2025, 6, 15), saved.getValue().getPublishedDate());
        assertEquals(createdAt, saved.getValue().getCreatedAt());
        assertEquals(1, saved.getValue().getStatus());
    }

    @Test
    void omittedDateAndStatusChangesPreserveSelectedDate() {
        Article existing = existingArticle();
        LocalDate original = existing.getPublishedDate();
        when(mapper.selectById(1L)).thenReturn(existing);
        when(mapper.updateById(any(Article.class))).thenReturn(1);

        assertTrue(service.update(1L, new ArticleUpdateRequest()));
        assertEquals(original, existing.getPublishedDate());
        assertTrue(service.updateStatus(1L, 0));
        assertTrue(service.updateStatus(1L, 1));
        assertEquals(original, existing.getPublishedDate());
    }

    @Test
    void dateInputAcceptsLeapDaysAndRejectsImpossibleDates() throws Exception {
        ObjectMapper json = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        assertEquals(LocalDate.of(2024, 2, 29), json.readValue(
                "{\"publishedDate\":\"2024-02-29\"}", ArticleCreateRequest.class).getPublishedDate());
        assertThrows(JsonProcessingException.class, () -> json.readValue(
                "{\"publishedDate\":\"2025-02-29\"}", ArticleCreateRequest.class));
    }

    private Article existingArticle() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(1);
        article.setCreatedAt(LocalDateTime.of(2026, 9, 20, 12, 30));
        article.setPublishedDate(LocalDate.of(2024, 2, 29));
        return article;
    }
}
