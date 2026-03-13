# 将 SpaceUserController 业务逻辑下沉到 Service

## Goal
将 Controller 层的业务逻辑迁移到 Service 层，使 Controller 只负责请求接收和响应返回。

## Requirements
1. 移除 Controller 中的重复权限检查（`@SaSpaceCheckPermission` 已处理）
2. 将以下业务逻辑下沉到 `SpaceUserService`:
   - `addSpaceUser`: 管理员检查、联系人关系验证
   - `deleteSpaceUser`: 存在性检查、自身操作检查、删除操作
   - `getSpaceUser`: 参数校验、存在性检查
   - `listSpaceUser`: 参数校验、查询和转换
   - `editSpaceUser`: 存在性检查、自身操作检查、更新操作
3. Service 方法应在接口 `SpaceUserService` 中定义
4. 保持原有 API 行为不变

## Acceptance Criteria
- [ ] Controller 方法只保留参数接收和 Service 调用
- [ ] 所有业务逻辑在 Service 层实现
- [ ] 编译通过，无错误
- [ ] 遵循项目现有代码风格

## Technical Notes
- 参考项目中其他 Controller 的简洁风格
- 使用 `Throw.throwIf()` 进行参数校验和异常处理
- Service 方法命名遵循项目约定