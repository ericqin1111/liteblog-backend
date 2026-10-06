-- Run on existing databases before deploying work notes. Safe to rerun.
USE liteblog;

CREATE TABLE IF NOT EXISTS work_note (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_id INT NOT NULL,
    title VARCHAR(200) NOT NULL DEFAULT '未命名工作',
    main_problem TEXT NOT NULL,
    content JSON NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_work_note_owner_list (owner_id, archived, updated_at, id),
    CONSTRAINT fk_work_note_owner FOREIGN KEY (owner_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员私人工作稿';

CREATE TABLE IF NOT EXISTS work_note_revision (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    work_note_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    main_problem TEXT NOT NULL,
    content JSON NOT NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_work_note_revision (work_note_id, version),
    CONSTRAINT fk_work_note_revision FOREIGN KEY (work_note_id) REFERENCES work_note(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作稿恢复快照';
