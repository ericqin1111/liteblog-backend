-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS liteblog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE liteblog;

-- 文章表
CREATE TABLE IF NOT EXISTS article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(255) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT 'Markdown 原文',
    category VARCHAR(50) DEFAULT NULL COMMENT '分类',
    tags VARCHAR(255) DEFAULT NULL COMMENT '标签（逗号分隔）',
    status TINYINT DEFAULT 0 COMMENT '状态：0=草稿，1=已发布',
    view_count INT DEFAULT 0 COMMENT '阅读量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- 访问日志表
CREATE TABLE IF NOT EXISTS access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    ip_address VARCHAR(45) NOT NULL COMMENT 'IP 地址',
    uri VARCHAR(255) NOT NULL COMMENT '访问路径',
    user_agent TEXT COMMENT 'User-Agent',
    article_id BIGINT DEFAULT NULL COMMENT '文章 ID（若访问文章详情）',
    access_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    INDEX idx_ip (ip_address),
    INDEX idx_access_time (access_time),
    INDEX idx_article_id (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问日志表';

-- 管理员表
CREATE TABLE IF NOT EXISTS admin (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密）',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- 插入默认管理员账号
-- 用户名: admin
-- 密码: admin123
-- 邮箱: admin@example.com
INSERT INTO admin (username, password, email) VALUES
('admin', '$2a$10$7OrbgiKRXA33vZILx7v67ewFVZe5UQuJWUId2g6rWLZB5r11Ezy76', 'admin@example.com')
ON DUPLICATE KEY UPDATE username = username;

-- 插入测试文章数据
INSERT INTO article (title, content, category, tags, status, view_count) VALUES
('欢迎使用 Lite-Blog', '# 欢迎使用 Lite-Blog\n\n这是一个基于 Spring Boot + Vue 3 的个人博客系统。\n\n## 功能特性\n\n- Markdown 写作\n- 文章分类与标签\n- 访问统计\n- 容器化部署\n\n开始你的博客之旅吧！', '公告', 'Lite-Blog,欢迎', 1, 0),
('Spring Boot 3.x 快速入门', '# Spring Boot 3.x 快速入门\n\n## 环境要求\n\n- JDK 17+\n- Maven 3.8+\n\n## 创建项目\n\n使用 Spring Initializr 创建项目...\n\n```java\n@SpringBootApplication\npublic class Application {\n    public static void main(String[] args) {\n        SpringApplication.run(Application.class, args);\n    }\n}\n```', '技术', 'Java,Spring Boot', 1, 0),
('Vue 3 Composition API 实践', '# Vue 3 Composition API 实践\n\n## setup 函数\n\n```javascript\nimport { ref, computed } from ''vue''\n\nexport default {\n  setup() {\n    const count = ref(0)\n    const double = computed(() => count.value * 2)\n    \n    return { count, double }\n  }\n}\n```', '技术', 'Vue,JavaScript', 1, 0),
('Docker 部署指南', '# Docker 部署指南\n\n## Dockerfile 编写\n\n```dockerfile\nFROM openjdk:17-jdk-alpine\nCOPY target/*.jar app.jar\nEXPOSE 8080\nENTRYPOINT ["java", "-jar", "app.jar"]\n```\n\n## 构建镜像\n\n```bash\ndocker build -t myblog .\n```', '运维', 'Docker,部署', 0, 0)
ON DUPLICATE KEY UPDATE title = title;
