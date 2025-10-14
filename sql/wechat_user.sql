
-- 为user表添加微信相关字段
ALTER TABLE user 
ADD COLUMN wxOpenId VARCHAR(128) COMMENT '微信OpenID',
ADD COLUMN wxUnionId VARCHAR(128) COMMENT '微信UnionID';
