-- 评论表
CREATE TABLE IF NOT EXISTS comments
(
    commentId   bigint auto_increment comment '评论 id'
        primary key,
    pictureId   bigint                                 not null comment '图片 id',
    userId      bigint                                 not null comment '评论用户 id',
    content     text                                   not null comment '评论内容',
    parentId    bigint       default null              null comment '父评论 id（为空表示一级评论）',
    replyUserId bigint       default null              null comment '回复的用户 id',
    likeCount   int          default 0                 not null comment '点赞数',
    createTime  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint      default 0                 not null comment '是否删除',
    constraint fk_comments_parent
        foreign key (parentId) references comments (commentId) on delete cascade
)
    comment '评论表' collate = utf8mb4_unicode_ci;

-- 评论内容表（用于存储长文本，支持懒加载）
CREATE TABLE IF NOT EXISTS comments_content
(
    commentId bigint       not null comment '评论 id'
        primary key,
    content   text         not null comment '评论内容',
    constraint fk_comments_content_comment
        foreign key (commentId) references comments (commentId) on delete cascade
)
    comment '评论内容表' collate = utf8mb4_unicode_ci;

-- 索引
create index idx_comments_pictureId
    on comments (pictureId);

create index idx_comments_userId
    on comments (userId);

create index idx_comments_parentId
    on comments (parentId);

create index idx_comments_createTime
    on comments (createTime);