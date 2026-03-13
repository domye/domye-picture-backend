# 用 S3 替代腾讯云 COS 存储

## Goal
将图片存储从腾讯云 COS 迁移到 S3 兼容存储（RUSTFS），移除 COS 依赖，使用应用层处理图片压缩和缩略图。

## Requirements
1. 使用 S3Manager 替代 CosManager 进行文件存储
2. 应用层实现图片处理（压缩转 webp、生成缩略图）
3. 完全移除腾讯云 COS SDK 依赖
4. 更新所有使用 COS 的代码

## Acceptance Criteria
- [ ] 添加 Thumbnailator 图片处理库依赖
- [ ] FileManager 使用 S3Manager 上传文件
- [ ] 实现应用层图片压缩（转 webp，质量 80%）
- [ ] 实现应用层缩略图生成（128x128）
- [ ] PictureServiceImpl 使用 S3Manager 删除文件
- [ ] 移除 CosManager 和 CosClientConfig
- [ ] 移除腾讯云 COS SDK 依赖
- [ ] 编译通过，无错误

## Technical Notes
- S3 服务：RUSTFS（S3 兼容）
- 图片处理库：Thumbnailator（轻量、简单）
- 压缩格式：webp，质量 80%
- 缩略图尺寸：128x128