create table comment_mention
(
    id              bigint auto_increment comment 'id'
        primary key,
    comment_id      bigint                                 not null comment '评论 id',
    mentioned_user_id bigint                               not null comment '被提及用户 id',
    is_read         tinyint      default 0                 not null comment '是否已读 0-未读 1-已读',
    create_time     datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    constraint uk_comment_mentioned
        unique (comment_id, mentioned_user_id)
)
    comment '评论@提及表' collate = utf8mb4_unicode_ci;

create index idx_comment_id
    on comment_mention (comment_id);

create index idx_mentioned_user_id
    on comment_mention (mentioned_user_id);

alter table comment_mention
    add constraint fk_comment_mention_comment
        foreign key (comment_id) references comments (commentId) on delete cascade;
