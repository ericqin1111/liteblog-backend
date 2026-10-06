package com.liteblog.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liteblog.dto.WorkNoteSaveRequest;
import com.liteblog.entity.*;
import com.liteblog.mapper.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class WorkNoteService {
    private final WorkNoteMapper notes;
    private final WorkNoteRevisionMapper revisions;
    private final AdminMapper admins;
    private final WorkNoteContentValidator validator;

    public WorkNoteService(WorkNoteMapper notes, WorkNoteRevisionMapper revisions, AdminMapper admins,
                           WorkNoteContentValidator validator) {
        this.notes = notes;
        this.revisions = revisions;
        this.admins = admins;
        this.validator = validator;
    }

    public Integer owner(String username) {
        Admin admin = admins.selectOne(new QueryWrapper<Admin>().eq("username", username));
        if (admin == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请重新登录");
        return admin.getId();
    }

    public Page<WorkNote> list(Integer ownerId, boolean archived, String keyword, int page, int size) {
        if (page < 1 || size < 1 || size > 100 || (keyword != null && keyword.length() > 200)) {
            throw new IllegalArgumentException("列表参数不正确");
        }
        return notes.selectPage(new Page<>(page, size), new QueryWrapper<WorkNote>()
                .select("id", "title", "main_problem", "archived", "version", "created_at", "updated_at")
                .eq("owner_id", ownerId).eq("archived", archived)
                .like(keyword != null && !keyword.isBlank(), "title", keyword)
                .orderByDesc("updated_at", "id"));
    }

    public WorkNote get(Integer ownerId, Long id) {
        WorkNote note = notes.selectOne(new QueryWrapper<WorkNote>().eq("id", id).eq("owner_id", ownerId));
        if (note == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工作稿不存在");
        return note;
    }

    @Transactional
    public WorkNote create(Integer ownerId, WorkNoteSaveRequest request) {
        validator.validate(request.content());
        WorkNote note = new WorkNote();
        note.setOwnerId(ownerId);
        apply(note, request);
        note.setVersion(0L);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(note.getCreatedAt());
        notes.insert(note);
        return note;
    }

    @Transactional
    public WorkNote save(Integer ownerId, Long id, WorkNoteSaveRequest request) {
        validator.validate(request.content());
        WorkNote previous = get(ownerId, id);
        checkVersion(previous, request.version());
        WorkNote next = new WorkNote();
        next.setId(id);
        next.setOwnerId(ownerId);
        next.setVersion(previous.getVersion());
        next.setCreatedAt(previous.getCreatedAt());
        apply(next, request);
        if (sameContent(previous, next) && Objects.equals(previous.getArchived(), next.getArchived())) return previous;
        next.setUpdatedAt(LocalDateTime.now());
        if (notes.replace(next) != 1) throw conflict();
        snapshot(previous, !Objects.equals(previous.getArchived(), next.getArchived()));
        next.setVersion(previous.getVersion() + 1);
        return next;
    }

    public List<WorkNoteRevision> revisions(Integer ownerId, Long id) {
        get(ownerId, id);
        return revisions.selectList(new QueryWrapper<WorkNoteRevision>()
                .select("id", "work_note_id", "title", "version", "created_at")
                .eq("work_note_id", id).orderByDesc("version").last("LIMIT 20"));
    }

    public WorkNoteRevision revision(Integer ownerId, Long id, Long revisionId) {
        get(ownerId, id);
        WorkNoteRevision result = revisions.selectOne(new QueryWrapper<WorkNoteRevision>()
                .eq("id", revisionId).eq("work_note_id", id));
        if (result == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "历史快照不存在");
        return result;
    }

    @Transactional
    public WorkNote restore(Integer ownerId, Long id, Long revisionId, Long version) {
        WorkNote previous = get(ownerId, id);
        checkVersion(previous, version);
        WorkNoteRevision revision = revision(ownerId, id, revisionId);
        validator.validate(revision.getContent());
        WorkNote next = new WorkNote();
        next.setId(id);
        next.setOwnerId(ownerId);
        next.setTitle(revision.getTitle());
        next.setMainProblem(revision.getMainProblem());
        next.setContent(revision.getContent());
        next.setArchived(previous.getArchived());
        next.setVersion(version);
        next.setCreatedAt(previous.getCreatedAt());
        next.setUpdatedAt(LocalDateTime.now());
        if (notes.replace(next) != 1) throw conflict();
        snapshot(previous, true);
        next.setVersion(version + 1);
        return next;
    }

    private void apply(WorkNote note, WorkNoteSaveRequest request) {
        note.setTitle(request.title().isBlank() ? "未命名工作" : request.title().trim());
        note.setMainProblem(request.mainProblem());
        note.setContent(request.content());
        note.setArchived(request.archived());
    }

    private boolean sameContent(WorkNote a, WorkNote b) {
        return Objects.equals(a.getTitle(), b.getTitle()) && Objects.equals(a.getMainProblem(), b.getMainProblem())
                && Objects.equals(a.getContent(), b.getContent());
    }

    private void checkVersion(WorkNote note, Long version) {
        if (!Objects.equals(note.getVersion(), version)) throw conflict();
    }

    private ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "工作稿已在其他页面更新，本地内容已保留，请处理后继续");
    }

    private void snapshot(WorkNote previous, boolean force) {
        WorkNoteRevision latest = revisions.selectOne(new QueryWrapper<WorkNoteRevision>()
                .eq("work_note_id", previous.getId()).orderByDesc("version").last("LIMIT 1"));
        if (latest != null && (Objects.equals(latest.getVersion(), previous.getVersion())
                || (!force && latest.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5))))) return;
        WorkNoteRevision revision = new WorkNoteRevision();
        revision.setWorkNoteId(previous.getId());
        revision.setTitle(previous.getTitle());
        revision.setMainProblem(previous.getMainProblem());
        revision.setContent(previous.getContent());
        revision.setVersion(previous.getVersion());
        revision.setCreatedAt(LocalDateTime.now());
        revisions.insert(revision);
        List<Object> keep = revisions.selectObjs(new QueryWrapper<WorkNoteRevision>()
                .select("id").eq("work_note_id", previous.getId()).orderByDesc("version").last("LIMIT 20"));
        if (!keep.isEmpty()) revisions.delete(new QueryWrapper<WorkNoteRevision>()
                .eq("work_note_id", previous.getId()).notIn("id", keep));
    }
}
