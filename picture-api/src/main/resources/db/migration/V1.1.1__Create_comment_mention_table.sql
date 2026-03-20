-- 评论@提及表
CREATE TABLE IF NOT EXISTS comment_mention
(
    id                bigint auto_increment comment 'id'
        primary key,
    commentId        bigint                                 not null comment '评论 id',
    mentionedUserId bigint                               not null comment '被提及用户 id',
    isRead           tinyint      default 0                 not null comment '是否已读 0-未读 1-已读',
    createTime       datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_comment_mentioned
        unique (commentId, mentionedUserId)
)
    comment '评论@提及表' collate = utf8mb4_unicode_ci;

-- 索引
create index idx_commentId
    on comment_mention (commentId);

create index idx_mentionedUserId
    on comment_mention (mentionedUserId);

-- 外键约束（在 comments 表存在后添加）
ALTER TABLE comment_mention
    ADD CONSTRAINT fk_comment_mention_comment
        FOREIGN KEY (commentId) REFERENCES comments (commentId) ON DELETE CASCADE;