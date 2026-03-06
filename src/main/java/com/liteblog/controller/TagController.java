package com.liteblog.controller;

import com.liteblog.dto.AdminTagVO;
import com.liteblog.dto.TagCreateRequest;
import com.liteblog.dto.TagVO;
import com.liteblog.entity.Tag;
import com.liteblog.service.TagService;
import com.liteblog.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/tags")
    public ResponseUtil<List<TagVO>> listTags() {
        return ResponseUtil.success(tagService.listAll());
    }

    @GetMapping("/admin/tags")
    public ResponseUtil<List<AdminTagVO>> listAdminTags() {
        return ResponseUtil.success(tagService.listAllForAdmin());
    }

    @PostMapping("/admin/tags")
    public ResponseEntity<ResponseUtil<TagVO>> createTag(@Valid @RequestBody TagCreateRequest request) {
        try {
            Tag tag = tagService.create(request.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseUtil.success("创建成功", new TagVO(tag.getId(), tag.getName())));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ResponseUtil.error(400, ex.getMessage()));
        }
    }

    @DeleteMapping("/admin/tags/{id}")
    public ResponseEntity<ResponseUtil<?>> deleteTag(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "false") boolean force) {
        if (!tagService.exists(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseUtil.error(404, "标签不存在"));
        }

        long articleCount = tagService.countArticlesByTagId(id);
        if (!force && articleCount > 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("articleCount", articleCount);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ResponseUtil<>(409, "标签仍关联文章，请确认后强制删除", data));
        }

        boolean deleted = tagService.delete(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseUtil.error(404, "标签不存在"));
        }
        return ResponseEntity.ok(ResponseUtil.success("删除成功", null));
    }

    @PostMapping("/admin/migrate/tags")
    public ResponseUtil<Map<String, Integer>> migrateLegacyTags() {
        int linked = tagService.migrateLegacyTags();
        Map<String, Integer> data = new HashMap<>();
        data.put("linked", linked);
        return ResponseUtil.success("迁移完成", data);
    }
}
