package com.liteblog.controller;

import com.liteblog.dto.WorkNoteSaveRequest;
import com.liteblog.dto.WorkNoteVersionRequest;
import com.liteblog.service.WorkNoteService;
import com.liteblog.util.ResponseUtil;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/admin/work-notes")
public class WorkNoteController {
    private final WorkNoteService service;

    public WorkNoteController(WorkNoteService service) { this.service = service; }

    @ModelAttribute
    public void preventCaching(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }

    @GetMapping("/context")
    public ResponseUtil<?> context(@RequestAttribute String username) {
        return ResponseUtil.success(Map.of("ownerId", service.owner(username)));
    }

    @GetMapping
    public ResponseUtil<?> list(@RequestAttribute String username,
                                @RequestParam(defaultValue = "false") boolean archived,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "30") int size) {
        var result = service.list(service.owner(username), archived, keyword, page, size);
        return ResponseUtil.page(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @GetMapping("/{id}")
    public ResponseUtil<?> get(@RequestAttribute String username, @PathVariable Long id) {
        return ResponseUtil.success(service.get(service.owner(username), id));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestAttribute String username, @Valid @RequestBody WorkNoteSaveRequest request) {
        return ResponseEntity.status(201).body(ResponseUtil.success(service.create(service.owner(username), request)));
    }

    @PutMapping("/{id}")
    public ResponseUtil<?> save(@RequestAttribute String username, @PathVariable Long id,
                               @Valid @RequestBody WorkNoteSaveRequest request) {
        return ResponseUtil.success(service.save(service.owner(username), id, request));
    }

    @GetMapping("/{id}/revisions")
    public ResponseUtil<?> revisions(@RequestAttribute String username, @PathVariable Long id) {
        return ResponseUtil.success(service.revisions(service.owner(username), id));
    }

    @GetMapping("/{id}/revisions/{revisionId}")
    public ResponseUtil<?> revision(@RequestAttribute String username, @PathVariable Long id, @PathVariable Long revisionId) {
        return ResponseUtil.success(service.revision(service.owner(username), id, revisionId));
    }

    @PostMapping("/{id}/revisions/{revisionId}/restore")
    public ResponseUtil<?> restore(@RequestAttribute String username, @PathVariable Long id, @PathVariable Long revisionId,
                                  @Valid @RequestBody WorkNoteVersionRequest request) {
        return ResponseUtil.success(service.restore(service.owner(username), id, revisionId, request.version()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> statusError(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ResponseUtil.error(ex.getStatusCode().value(), ex.getReason()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ResponseUtil.error(400, ex.getMessage()));
    }
}
