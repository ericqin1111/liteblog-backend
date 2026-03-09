package com.liteblog.controller;

import com.liteblog.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
public class UploadController {

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final DateTimeFormatter DATE_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${blog.upload-dir:./uploads}")
    private String uploadDir;

    @PostMapping("/admin/upload/image")
    public ResponseEntity<ResponseUtil<?>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ResponseUtil.error(400, "请选择要上传的图片"));
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            return ResponseEntity.badRequest().body(ResponseUtil.error(400, "图片大小不能超过 5MB"));
        }

        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "";
        String extension = getExtension(originalName);
        if (!StringUtils.hasText(extension) || !ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest().body(ResponseUtil.error(400, "仅支持 jpg/jpeg/png/gif/webp 格式"));
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(ResponseUtil.error(400, "仅支持图片文件上传"));
        }

        String dateSegment = LocalDate.now().format(DATE_DIR_FORMAT);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetDir = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(dateSegment);
        Path targetPath = targetDir.resolve(fileName);

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseUtil.error(500, "图片上传失败"));
        }

        String normalizedPath = dateSegment.replace("\\", "/") + "/" + fileName;
        String url = "/uploads/" + normalizedPath;
        return ResponseEntity.ok(ResponseUtil.success("上传成功", Map.of(
                "url", url,
                "name", fileName
        )));
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase();
    }
}
