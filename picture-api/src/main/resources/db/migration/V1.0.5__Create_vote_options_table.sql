-- 投票选项表
CREATE TABLE IF NOT EXISTS vote_options
(
    id         bigint auto_increment
        primary key,
    activityId bigint                             not null comment '活动ID',
    optionText varchar(500)                       not null comment '选项内容',
    voteCount  bigint   default 0                 null comment '得票数',
    createTime datetime default CURRENT_TIMESTAMP null
)
    comment '投票选项表';

create index idx_activity_id
    on vote_options (activityId);