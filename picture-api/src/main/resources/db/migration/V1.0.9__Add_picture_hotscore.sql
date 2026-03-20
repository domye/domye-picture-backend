-- 为 picture 表添加热度分数字段
ALTER TABLE picture ADD COLUMN IF NOT EXISTS hotScore INT DEFAULT 0 COMMENT '热度分数' AFTER spaceId;

-- 创建热度排序索引（如果不存在）
-- 注意：MySQL 不支持 IF NOT EXISTS 用于 CREATE INDEX，需要使用存储过程或忽略错误
-- 这里使用条件创建的方式
SET @exist_idx := (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                   AND table_name = 'picture'
                   AND index_name = 'idx_picture_hotscore');
SET @sql := IF(@exist_idx = 0,
    'CREATE INDEX idx_picture_hotscore ON picture(hotScore DESC, id DESC)',
    'SELECT ''Index idx_picture_hotscore already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;