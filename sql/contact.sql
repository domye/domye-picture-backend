create table contact
(
    id               bigint auto_increment comment 'id'
        primary key,
    userId           bigint                                 not null comment '用户id（发起者）',
    contactUserId    bigint                                 not null comment '联系人用户id',
    status           tinyint      default 0                 not null comment '状态：0-待确认，1-已通过，2-已拒绝',
    createTime       datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime       datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_userId_contactUserId
        unique (userId, contactUserId)
)
    comment '联系人' collate = utf8mb4_unicode_ci;

create index idx_userId
    on contact (userId);

create index idx_contactUserId
    on contact (contactUserId);