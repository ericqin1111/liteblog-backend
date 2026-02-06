package com.liteblog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteblog.dto.ArticleCreateRequest;
import com.liteblog.dto.ArticleStatusRequest;
import com.liteblog.dto.ArticleUpdateRequest;
import com.liteblog.entity.Article;
import com.liteblog.service.ArticleService;
import com.liteblog.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ArticleController {

    private final ArticleService articleService;

    @Autowired
    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/articles")
    public ResponseUtil<?> listPublished(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category) {
        Page<Article> result = articleService.listPublished(page, size, category);
        return ResponseUtil.page(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<ResponseUtil<Article>> getPublished(@PathVariable Long id) {
        Article article = articleService.getPublishedById(id);
        if (article == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseUtil.error(404, "文章不存在或未发布"));
        }
        return ResponseEntity.ok(ResponseUtil.success(article));
    }

    @GetMapping("/admin/articles")
    public ResponseUtil<?> listAdmin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        Page<Article> result = articleService.listAdmin(page, size, category, status);
        return ResponseUtil.page(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @GetMapping("/admin/articles/{id}")
    public ResponseEntity<ResponseUtil<Article>> getAdminById(@PathVariable Long id) {
        Article article = articleService.getById(id);
        if (article == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseUtil.error(404, "文章不存在"));
        }
        return ResponseEntity.ok(ResponseUtil.success(article));
    }

    @PostMapping("/admin/articles")
    public ResponseEntity<ResponseUtil<Article>> create(@Valid @RequestBody ArticleCreateRequest request) {
        Article article = articleService.create(request);
        if (article == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseUtil.error(500, "创建失败"));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.success("创建成功", article));
    }

    @PutMapping("/admin/articles/{id}")
    public ResponseEntity<ResponseUtil<?>> update(@PathVariable Long id, @Valid @RequestBody ArticleUpdateRequest request) {
        boolean updated = articleService.update(id, request);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseUtil.error(404, "文章不存在"));
        }
        return ResponseEntity.ok(ResponseUtil.success("更新成功", null));
    }

    @PutMapping("/admin/articles/{id}/status")
    public ResponseEntity<ResponseUtil<?>> updateStatus(@PathVariable Long id, @Valid @RequestBody ArticleStatusRequest request) {
        boolean updated = articleService.updateStatus(id, request.getStatus());
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseUtil.error(404, "文章不存在"));
        }
        return ResponseEntity.ok(ResponseUtil.success("状态更新成功", null));
    }

    @DeleteMapping("/admin/articles/{id}")
    public ResponseEntity<ResponseUtil<?>> delete(@PathVariable Long id) {
        boolean deleted = articleService.delete(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseUtil.error(404, "文章不存在"));
        }
        return ResponseEntity.ok(ResponseUtil.success("删除成功", null));
    }
}
