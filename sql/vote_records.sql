create table vote_records
(
    id         bigint auto_increment
        primary key,
    activityId bigint                             not null comment '活动ID',
    optionId   bigint                             not null comment '选项ID',
    userId     bigint                             not null comment '用户ID',
    clientIp   varchar(50)                        null comment '客户端IP',
    userAgent  varchar(500)                       null comment '用户代理',
    deviceId   varchar(100)                       null comment '设备ID',
    voteTime   datetime default CURRENT_TIMESTAMP null,
    constraint uk_activity_user
        unique (activityId, userId)
)
    comment '投票记录表';

create index idx_activity_option
    on vote_records (activityId, optionId);

create index idx_ip_time
    on vote_records (clientIp, voteTime);

create index idx_user_time
    on vote_records (userId, voteTime);

