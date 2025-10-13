create table space
(
    id         bigint auto_increment comment 'id'
        primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    spaceType  int      default 0                 not null comment '空间类型：0-私有 1-团队'
)
    comment '空间' collate = utf8mb4_unicode_ci;

create index idx_spaceLevel
    on space (spaceLevel);

create index idx_spaceName
    on space (spaceName);

create index idx_spaceType
    on space (spaceType);

create index idx_userId
    on space (userId);

