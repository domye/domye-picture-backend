-- 为 picture 表添加热度分数字段
ALTER TABLE picture ADD COLUMN hotScore INT DEFAULT 0 COMMENT '热度分数' AFTER spaceId;

-- 创建热度排序索引（仅对审核通过且公开的图片）
-- 使用条件索引优化查询性能
CREATE INDEX idx_picture_hotscore ON picture(hotScore DESC, id DESC);

-- 初始化热度分数（可选：根据现有评论数计算初始热度）
-- UPDATE picture p SET hotScore = (
--     SELECT COUNT(*) * 5 FROM comments c WHERE c.pictureid = p.id AND c.parentid IS NULL
-- ) WHERE reviewStatus = 1;