create table vote_activities
(
    id              bigint auto_increment
        primary key,
    title           varchar(200)                       not null comment '活动标题',
    description     text                               null comment '活动描述',
    startTime       datetime                           not null comment '开始时间',
    endTime         datetime                           not null comment '结束时间',
    status          tinyint  default 1                 null comment '状态：1-进行中 2-已结束 3-已暂停',
    maxVotesPerUser int      default 1                 null comment '每用户最大投票数',
    totalVotes      bigint   default 0                 null comment '总投票数',
    createTime      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    createUser      bigint                             null comment '创建者'
)
    comment '投票活动表';

create index idx_created_at
    on vote_activities (createTime);

create index idx_status_time
    on vote_activities (status, startTime, endTime);

