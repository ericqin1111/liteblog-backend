package com.liteblog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liteblog.dto.WorkNoteSaveRequest;
import com.liteblog.entity.WorkNote;
import com.liteblog.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:work-notes;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.type=com.zaxxer.hikari.HikariDataSource",
        "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl",
        "logging.level.com.liteblog.mapper=warn"
})
@AutoConfigureMockMvc
class WorkNoteIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired JwtUtil jwt;
    @Autowired WorkNoteService service;
    @MockBean AccessLogService accessLogs;

    @BeforeEach
    void database() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS admin (id INT PRIMARY KEY, username VARCHAR(50), password VARCHAR(255), email VARCHAR(100), created_at TIMESTAMP)");
        // H2 uses CLOB for JSON text; production migration and JSON columns are verified against MySQL separately.
        jdbc.execute("CREATE TABLE IF NOT EXISTS work_note (id BIGINT AUTO_INCREMENT PRIMARY KEY, owner_id INT NOT NULL, title VARCHAR(200), main_problem CLOB, content CLOB, archived BOOLEAN, version BIGINT, created_at TIMESTAMP(6), updated_at TIMESTAMP(6))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS work_note_revision (id BIGINT AUTO_INCREMENT PRIMARY KEY, work_note_id BIGINT NOT NULL, title VARCHAR(200), main_problem CLOB, content CLOB, version BIGINT, created_at TIMESTAMP(6), UNIQUE(work_note_id, version))");
        jdbc.update("DELETE FROM work_note_revision");
        jdbc.update("DELETE FROM work_note");
        jdbc.update("DELETE FROM admin");
        jdbc.update("INSERT INTO admin (id, username) VALUES (1, 'writer'), (2, 'another')");
    }

    private JsonNode document(String text) throws Exception {
        var doc = json.createObjectNode().put("type", "doc");
        doc.putArray("content").addObject().put("type", "paragraph")
                .putArray("content").addObject().put("type", "text").put("text", text);
        return doc;
    }

    private WorkNoteSaveRequest request(String title, long version, boolean archived) throws Exception {
        return new WorkNoteSaveRequest(title, "先解决 A，再判断 B 是否值得继续", document("我自己的判断：" + title), archived, version);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String username) {
        return builder.contextPath("/api").header("Authorization", "Bearer " + jwt.generateAccessToken(username, "test"));
    }

    @Test
    void authenticationAndOwnerIsolationApplyToNotesAndRevisions() throws Exception {
        mvc.perform(get("/api/admin/work-notes").contextPath("/api")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/work-notes").contextPath("/api").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request("A", 0, false)))).andExpect(status().isUnauthorized());
        WorkNote note = service.create(1, request("私人工作", 0, false));
        service.save(1, note.getId(), request("第二版", 0, false));
        Long revision = service.revisions(1, note.getId()).get(0).getId();
        String base = "/api/admin/work-notes/" + note.getId();
        mvc.perform(auth(get(base), "writer")).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.ownerId").doesNotExist()).andExpect(jsonPath("$.data.content.type").value("doc"));
        mvc.perform(auth(get(base), "another")).andExpect(status().isNotFound());
        mvc.perform(auth(put(base), "another").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request("越权写入", 1, false)))).andExpect(status().isNotFound());
        mvc.perform(auth(get(base + "/revisions"), "another")).andExpect(status().isNotFound());
        mvc.perform(auth(get(base + "/revisions/" + revision), "another")).andExpect(status().isNotFound());
        mvc.perform(auth(post(base + "/revisions/" + revision + "/restore"), "another")
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":1}")).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/admin/work-notes"), "another")).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(auth(get("/api/admin/work-notes/context"), "deleted-admin")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/work-notes/" + note.getId()).contextPath("/api")).andExpect(status().isNotFound());
    }

    @Test
    void richContentRoundTripsAndStaleSavesCannotOverwriteIt() throws Exception {
        String body = """
            {"title":"开发记录","mainProblem":"回到 A","archived":false,"version":0,
             "content":{"type":"doc","content":[
               {"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"当前理解"}]},
               {"type":"paragraph","content":[{"type":"text","text":"验证方向 📝","marks":[{"type":"bold"},{"type":"highlight"}]}]},
               {"type":"taskList","content":[{"type":"taskItem","attrs":{"checked":false},"content":[{"type":"paragraph","content":[{"type":"text","text":"重新验证 A"}]}]}]},
               {"type":"codeBlock","attrs":{"language":null},"content":[{"type":"text","text":"const a = 1;"}]}]}}
            """;
        JsonNode created = json.readTree(mvc.perform(auth(post("/api/admin/work-notes"), "writer")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("data");
        long id = created.path("id").asLong();
        assertEquals(json.readTree(body).path("content"), service.get(1, id).getContent());
        service.save(1, id, request("新的理解", 0, false));
        mvc.perform(auth(put("/api/admin/work-notes/" + id), "writer").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409));
        assertEquals("新的理解", service.get(1, id).getTitle());
        assertEquals(1L, service.get(1, id).getVersion());
    }

    @Test
    void concurrentSavesHaveExactlyOneWinner() throws Exception {
        WorkNote note = service.create(1, request("原稿", 0, false));
        var one = request("方向一", 0, false);
        var two = request("方向二", 0, false);
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var results = java.util.List.of(one, two).stream().map(body -> executor.submit(() -> {
                start.await();
                try { service.save(1, note.getId(), body); return 200; }
                catch (ResponseStatusException ex) { return ex.getStatusCode().value(); }
            })).toList();
            start.countDown();
            var statuses = java.util.List.of(results.get(0).get(10, TimeUnit.SECONDS), results.get(1).get(10, TimeUnit.SECONDS));
            assertTrue(statuses.contains(200));
            assertTrue(statuses.contains(409));
            assertEquals(1L, service.get(1, note.getId()).getVersion());
            assertEquals(1, service.revisions(1, note.getId()).size());
        } finally { executor.shutdownNow(); }
    }

    @Test
    void restoringKeepsThePreRestoreContentAndRejectsStaleRestore() throws Exception {
        WorkNote first = service.create(1, request("最初的理解", 0, false));
        WorkNote second = service.save(1, first.getId(), request("后来的理解", 0, false));
        Long revisionId = service.revisions(1, first.getId()).get(0).getId();
        WorkNote third = service.restore(1, first.getId(), revisionId, second.getVersion());
        assertEquals("最初的理解", third.getTitle());
        assertEquals(2L, third.getVersion());
        var recovery = service.revisions(1, first.getId()).stream().filter(revision -> revision.getVersion() == 1L).findFirst().orElseThrow();
        assertEquals("后来的理解", service.revision(1, first.getId(), recovery.getId()).getTitle());
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.restore(1, first.getId(), revisionId, 1L)).getStatusCode().value());
    }

    @Test
    void snapshotsAreThrottledAndOnlyTwentyAreRetained() throws Exception {
        WorkNote note = service.create(1, request("0", 0, false));
        note = service.save(1, note.getId(), request("1", note.getVersion(), false));
        note = service.save(1, note.getId(), request("2", note.getVersion(), false));
        assertEquals(1, service.revisions(1, note.getId()).size());
        for (int i = 3; i <= 25; i++) {
            jdbc.update("UPDATE work_note_revision SET created_at = '2020-01-01 00:00:00'");
            note = service.save(1, note.getId(), request(String.valueOf(i), note.getVersion(), false));
        }
        assertEquals(20, service.revisions(1, note.getId()).size());
        assertEquals(20, jdbc.queryForObject("SELECT COUNT(*) FROM work_note_revision", Integer.class));
        assertEquals(25L, note.getVersion());
    }

    @Test
    void archiveSearchPaginationAndEmptyNotesWork() throws Exception {
        WorkNote note = service.create(1, request("需求 A", 0, false));
        service.create(1, request("需求 B", 0, false));
        service.create(2, request("需求 A 私人", 0, false));
        assertEquals(2, service.list(1, false, null, 1, 1).getTotal());
        assertEquals(1, service.list(1, false, "A", 1, 30).getTotal());
        assertNull(service.list(1, false, null, 1, 30).getRecords().get(0).getContent());
        service.save(1, note.getId(), request("需求 A", 0, true));
        assertEquals(1, service.list(1, true, null, 1, 30).getTotal());
        service.save(1, note.getId(), request("需求 A", 1, false));
        assertEquals(0, service.list(1, true, null, 1, 30).getTotal());
        WorkNote empty = service.create(1, new WorkNoteSaveRequest("", "", json.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}"), false, 0L));
        assertEquals("未命名工作", empty.getTitle());
        assertEquals(empty.getVersion(), service.save(1, empty.getId(), new WorkNoteSaveRequest("", "", empty.getContent(), false, 0L)).getVersion());
        mvc.perform(auth(get("/api/admin/work-notes?size=10000"), "writer")).andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedOrUnsafeContentIsRejectedWithoutSaving() throws Exception {
        for (String content : java.util.List.of(
                "{\"type\":\"doc\",\"content\":[{\"type\":\"script\"}]}",
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"x\",\"marks\":[{\"type\":\"link\",\"attrs\":{\"href\":\"javascript:alert(1)\"}}]}]}]}",
                "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"attrs\":{\"onclick\":\"alert(1)\"}}]}",
                "{\"type\":\"doc\",\"content\":[]}")) {
            var request = new WorkNoteSaveRequest("无效", "", json.readTree(content), false, 0L);
            mvc.perform(auth(post("/api/admin/work-notes"), "writer").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
        }
        assertEquals(0, service.list(1, false, null, 1, 30).getTotal());
        assertThrows(IllegalArgumentException.class, () -> service.create(1,
                new WorkNoteSaveRequest("过长", "", document("字".repeat(400000)), false, 0L)));
    }
}
