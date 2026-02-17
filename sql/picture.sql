create table picture
(
    id            bigint auto_increment comment 'id'
        primary key,
    url           varchar(512)                       not null comment '图片 url',
    name          varchar(128)                       not null comment '图片名称',
    introduction  varchar(512)                       null comment '简介',
    category      varchar(64)                        null comment '分类',
    tags          varchar(512)                       null comment '标签（JSON 数组）',
    picSize       bigint                             null comment '图片体积',
    picWidth      int                                null comment '图片宽度',
    picHeight     int                                null comment '图片高度',
    picScale      double                             null comment '图片宽高比例',
    picFormat     varchar(32)                        null comment '图片格式',
    userId        bigint                             not null comment '创建用户 id',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime      datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    reviewStatus  int      default 0                 not null comment '审核状态：0-待审核; 1-通过; 2-拒绝',
    reviewMessage varchar(512)                       null comment '审核信息',
    reviewerId    bigint                             null comment '审核人 ID',
    reviewTime    datetime                           null comment '审核时间',
    thumbnailUrl  varchar(512)                       null comment '缩略图 url',
    spaceId       bigint                             null comment '空间 id（为空表示公共空间）',
    picColor      varchar(16)                        null comment '图片主色调'
)
    comment '图片' collate = utf8mb4_unicode_ci;

create index idx_category
    on picture (category);

create index idx_introduction
    on picture (introduction);

create index idx_name
    on picture (name);

create index idx_reviewStatus
    on picture (reviewStatus);

create index idx_spaceId
    on picture (spaceId);

create index idx_spaceId_reviewStatus
    on picture (spaceId, reviewStatus);

create index idx_tags
    on picture (tags);

create index idx_userId
    on picture (userId);

