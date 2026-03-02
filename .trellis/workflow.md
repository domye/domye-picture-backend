# 开发工作流指南

## 核心原则

### 1. 先读后写 (Read Before Write)
- 实现功能前，先阅读相关代码规范文件
- 参考现有代码模式，保持一致性
- 不确定的实现方式，先搜索项目中类似的代码

### 2. 遵循规范 (Follow Standards)
- 代码风格遵循 `spec/backend/` 或 `spec/frontend/` 中的规范
- 测试遵循 `spec/unit-test/` 中的规范
- 思维方式遵循 `spec/guides/` 中的指南

### 3. 任务驱动 (Task-Driven)
- 复杂任务使用任务工作流：创建任务 → 写PRD → 研究 → 实现 → 检查
- 任务目录位于 `.trellis/tasks/`
- 每个任务有独立的 PRD 文档跟踪需求

---

## 文件系统结构

```
.trellis/
├── workflow.md           # 本文件 - 工作流指南
├── scripts/
│   ├── get_context.py   # 获取当前会话上下文
│   └── task.py          # 任务管理脚本
├── spec/
│   ├── backend/         # 后端开发规范
│   ├── frontend/        # 前端开发规范
│   ├── guides/          # 思维指南
│   └── unit-test/       # 测试规范
├── tasks/               # 任务目录
│   └── <task-slug>/     # 具体任务
│       ├── prd.md       # 需求文档
│       ├── task.json    # 任务状态
│       ├── implement.jsonl  # 实现阶段上下文
│       └── check.jsonl  # 检查阶段上下文
└── .current-task        # 当前活动任务标记
```

---

## 开发流程

### 简单任务流程
```
确认理解 → 直接实现 → 检查代码 → 完成
```

适用于：
- 单文件修改
- 明确的小改动
- 不涉及架构变更

### 复杂任务流程
```
Brainstorm → 创建任务 → 写PRD → 研究 → 配置上下文 → 实现 → 检查 → 完成
```

适用于：
- 多文件修改
- 新功能开发
- 涉及架构决策
- 需求不明确

---

## 命名规范

### 任务目录命名
- 使用小写字母和连字符
- 格式: `<type>-<feature>-<detail>`
- 示例: `feat-notification-system`, `fix-login-error`

### 分支命名
- feature: `feat/<feature-name>`
- fix: `fix/<bug-description>`
- refactor: `refactor/<description>`

---

## 代码规范索引

| 类型 | 路径 | 说明 |
|------|------|------|
| 后端 | `spec/backend/index.md` | Spring Boot 后端开发规范 |
| 前端 | `spec/frontend/index.md` | 前端开发规范 |
| 测试 | `spec/unit-test/index.md` | 单元测试规范 |
| 指南 | `spec/guides/index.md` | 思维和决策指南 |

---

## 最佳实践

1. **每次开始前运行 `/trellis:start`** - 获取当前状态和上下文
2. **复杂任务先 Brainstorm** - 明确需求再动手
3. **完成后运行 `/trellis:finish-work`** - 提交前检查清单
4. **记录会话运行 `/trellis:record-session`** - 记录开发过程

---

*最后更新: 2026-03-02*