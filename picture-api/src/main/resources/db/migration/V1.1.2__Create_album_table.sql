-- 相册表
CREATE TABLE album
(
    id            bigint                             not null comment 'id'
    primary key,
    name          varchar(128)                       not null comment '相册名称',
    introduction  varchar(512)                       null comment '相册简介',
    category      varchar(64)                        null comment '分类',
    tags          varchar(512)                       null comment '标签（JSON 数组）',
    picCount      int                                null comment '图片数量',
    userId        bigint                             not null comment '创建用户 id',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime      datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    spaceId       bigint                             null comment '空间 id（为空表示公共空间）'
    )
    comment '相册' collate = utf8mb4_unicode_ci;