package com.liteblog.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Only the editor's supported document schema is accepted; arbitrary HTML is never stored/rendered. */
@Component
public class WorkNoteContentValidator {
    private static final Set<String> BLOCKS = Set.of("paragraph", "heading", "blockquote", "codeBlock",
            "bulletList", "orderedList", "taskList", "horizontalRule");
    private static final Set<String> MARKS = Set.of("bold", "italic", "strike", "underline", "code", "highlight", "link");

    public void validate(JsonNode document) {
        require(document != null && document.isObject() && "doc".equals(document.path("type").asText()));
        require(document.toString().getBytes(StandardCharsets.UTF_8).length <= 1_000_000);
        visit(document, 0, new int[]{0});
    }

    private void visit(JsonNode node, int depth, int[] count) {
        require(depth <= 40 && ++count[0] <= 30000 && node.isObject());
        fields(node, Set.of("type", "attrs", "content", "text", "marks"));
        String type = node.path("type").asText();
        require(BLOCKS.contains(type) || Set.of("doc", "text", "hardBreak", "listItem", "taskItem").contains(type));
        if ("text".equals(type)) {
            require(node.path("text").isTextual() && !node.path("text").asText().isEmpty() && !node.has("content"));
        } else {
            require(!node.has("text"));
        }
        attributes(type, node.get("attrs"));
        JsonNode marks = node.get("marks");
        if (marks != null) {
            require(marks.isArray() && marks.size() <= 7 && Set.of("text", "hardBreak").contains(type));
            for (JsonNode mark : marks) {
                fields(mark, Set.of("type", "attrs"));
                String markType = mark.path("type").asText();
                require(MARKS.contains(markType));
                attributes(markType, mark.get("attrs"));
            }
        }
        JsonNode children = node.get("content");
        if (children != null) {
            require(children.isArray());
            for (JsonNode child : children) {
                String childType = child.path("type").asText();
                boolean allowed = switch (type) {
                    case "doc", "blockquote", "listItem", "taskItem" -> BLOCKS.contains(childType);
                    case "paragraph", "heading" -> Set.of("text", "hardBreak").contains(childType);
                    case "codeBlock" -> "text".equals(childType) && !child.has("marks");
                    case "bulletList", "orderedList" -> "listItem".equals(childType);
                    case "taskList" -> "taskItem".equals(childType);
                    default -> false;
                };
                require(allowed);
                visit(child, depth + 1, count);
            }
        }
        if (Set.of("doc", "blockquote", "bulletList", "orderedList", "taskList", "listItem", "taskItem").contains(type)) {
            require(children != null && !children.isEmpty());
        }
        if (Set.of("listItem", "taskItem").contains(type)) {
            require("paragraph".equals(children.get(0).path("type").asText()));
        }
    }

    private void attributes(String type, JsonNode attrs) {
        if (attrs == null || attrs.isNull()) return;
        Set<String> allowed = switch (type) {
            case "heading" -> Set.of("level");
            case "orderedList" -> Set.of("start", "type");
            case "codeBlock" -> Set.of("language");
            case "taskItem" -> Set.of("checked");
            case "link" -> Set.of("href", "target", "rel", "class", "title");
            case "highlight" -> Set.of("color");
            default -> Set.of();
        };
        fields(attrs, allowed);
        switch (type) {
            case "heading" -> require(attrs.path("level").isInt() && attrs.path("level").asInt() >= 1 && attrs.path("level").asInt() <= 3);
            case "orderedList" -> {
                require(!attrs.has("start") || (attrs.path("start").isIntegralNumber() && attrs.path("start").asLong() >= 1 && attrs.path("start").asLong() <= Integer.MAX_VALUE));
                require(!attrs.hasNonNull("type") || Set.of("1", "a", "A", "i", "I").contains(attrs.path("type").asText()));
            }
            case "taskItem" -> require(attrs.path("checked").isBoolean());
            case "link" -> {
                String href = attrs.path("href").asText();
                require(href.length() <= 2048 && !href.matches("(?s).*\\s.*"));
                try {
                    String scheme = URI.create(href).getScheme();
                    require(scheme != null && Set.of("http", "https", "mailto").contains(scheme.toLowerCase(java.util.Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    throw invalid();
                }
                require(!attrs.hasNonNull("target") || Set.of("_blank", "_self").contains(attrs.path("target").asText()));
            }
            case "highlight" -> require(!attrs.hasNonNull("color")); // One fixed highlight color in this editor.
            default -> { }
        }
        for (JsonNode value : attrs) {
            require(value.isValueNode() && (!value.isTextual() || value.asText().length() <= 2048));
        }
    }

    private void fields(JsonNode node, Set<String> allowed) {
        require(node.isObject());
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) require(allowed.contains(fields.next().getKey()));
    }

    private void require(boolean valid) {
        if (!valid) throw invalid();
    }

    private IllegalArgumentException invalid() {
        return new IllegalArgumentException("工作稿格式不受支持，或内容超过 1 MB，请调整后重试");
    }
}
