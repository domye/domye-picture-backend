# Journal - domye (Part 1)

> AI development session journal
> Started: 2026-03-03

---



## Session 1: 更新 README 文档

**Date**: 2026-03-08
**Task**: 更新 README 文档

### Summary

更新 README 文档以反映项目当前架构：Spring Boot 3.3.6、多模块结构、完整技术栈版本表、新增功能特性说明

### Main Changes

(Add details)

### Git Commits

| Hash | Message |
|------|---------|
| `c9857a5` | (see git log) |

### Testing

- [OK] (Add test results)

### Status

[OK] **Completed**

### Next Steps

- None - task complete


## Session 2: 优化活跃度排行榜功能

**Date**: 2026-03-08
**Task**: 优化活跃度排行榜功能

### Summary

(Add summary)

### Main Changes

## 功能增强

| 功能 | 说明 |
|------|------|
| 排行榜类型 | 新增周榜、总榜（原有日榜、月榜） |
| 活跃度维度 | 扩展评论(+5)、点赞(+2)、收藏(+3)、分享(+4) |
| 我的排名 | 新增 `/rank/myRank` 接口 |
| 评论活跃度 | 评论时自动更新用户活跃度 |

## 性能优化

- 用户信息多级缓存（本地缓存 + Redis）
- 排行榜本地缓存（30秒 TTL）
- 修复日期计算 bug（`Date today` 实例变量问题）

## 新增文件

- `ActivityScoreType.java` - 活跃行为类型枚举
- `UserRankVO.java` - 用户排名 VO
- `RankCacheService.java` - 排行榜缓存服务

## API 变更

```
GET /rank/UserActivityScore?value=1&size=10
  - value: 1=日榜, 2=周榜, 3=月榜, 4=总榜

GET /rank/myRank (需登录)
  - 返回当前用户在各榜单的排名和分数
```


### Git Commits

| Hash | Message |
|------|---------|
| `5212de8` | (see git log) |

### Testing

- [OK] (Add test results)

### Status

[OK] **Completed**

### Next Steps

- None - task complete


## Session 3: 用S3兼容存储替代腾讯云COS

**Date**: 2026-03-13
**Task**: 用S3兼容存储替代腾讯云COS

### Summary

腾讯云COS迁移到S3兼容存储(RustFS)，使用Thumbnailator处理图片压缩和缩略图

### Main Changes

(Add details)

### Git Commits

| Hash | Message |
|------|---------|
| `e2bd131` | (see git log) |

### Testing

- [OK] (Add test results)

### Status

[OK] **Completed**

### Next Steps

- None - task complete


## Session 4: 修复图片缓存未更新问题

**Date**: 2026-03-13
**Task**: 修复图片缓存未更新问题

### Summary

修复图片上传/编辑后列表缓存未刷新的问题

**问题**: 图片数据变更后，分页列表缓存未清除，导致前端显示旧数据

**解决方案**:
- 使用 TransactionSynchronizationManager 在事务提交后清除缓存
- 避免事务内清除缓存导致的读写竞争问题
- 同时清除 Redis 和 Caffeine 双级缓存

**修改方法**:
- persistPictureData() - 上传图片后清除缓存
- editPicture() - 编辑图片信息后清除缓存
- updatePicture() - 管理员更新图片后清除缓存
- deletePicture() - 删除图片后清除缓存

### Main Changes

(Add details)

### Git Commits

| Hash | Message |
|------|---------|
| `092d8d6` | (see git log) |

### Testing

- [OK] (Add test results)

### Status

[OK] **Completed**

### Next Steps

- None - task complete


## Session 5: 缓存优化：序列化统一、Key前缀规范、配置外部化

**Date**: 2026-03-13
**Task**: 缓存优化：序列化统一、Key前缀规范、配置外部化

### Summary

修复缓存模块的三个问题：1.序列化统一使用JSON 2.Key前缀使用常量 3.Redisson配置外部化

### Main Changes

(Add details)

### Git Commits

| Hash | Message |
|------|---------|
| `456294e` | (see git log) |

### Testing

- [OK] (Add test results)

### Status

[OK] **Completed**

### Next Steps

- None - task complete
