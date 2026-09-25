-- Existing databases: run before deploying the backend with publishedDate support.
-- Safe to rerun. Only fills missing dates; preserves dates already edited by users.
USE liteblog;

SET @has_published_date = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'article'
      AND column_name = 'published_date'
);
SET @migration_sql = IF(
    @has_published_date = 0,
    'ALTER TABLE article ADD COLUMN published_date DATE DEFAULT NULL COMMENT ''发布日期'' AFTER view_count',
    'SELECT 1'
);
PREPARE migration_statement FROM @migration_sql;
EXECUTE migration_statement;
DEALLOCATE PREPARE migration_statement;

UPDATE article SET published_date = DATE(created_at) WHERE published_date IS NULL;
