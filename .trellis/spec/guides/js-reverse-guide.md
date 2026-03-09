# JS-Reverse MCP 使用指南

> **JS-Reverse** 是一个强大的 JavaScript 逆向工程 MCP 服务器，提供浏览器调试、代码分析、网络监控等功能。

---

## 工具分类概览

| 类别 | 主要工具 | 用途 |
|------|----------|------|
| 页面管理 | `list_pages`, `select_page`, `new_page`, `navigate_page` | 管理浏览器页面 |
| 脚本分析 | `list_scripts`, `get_script_source`, `find_in_script`, `search_in_sources` | 查看/搜索 JS 代码 |
| 断点调试 | `set_breakpoint`, `set_breakpoint_on_text`, `pause`, `resume`, `step_*` | 断点调试代码 |
| 函数追踪 | `hook_function`, `trace_function`, `unhook_function` | 监控函数调用 |
| 网络监控 | `list_network_requests`, `get_network_request`, `break_on_xhr` | 分析网络请求 |
| WebSocket | `list_websocket_connections`, `get_websocket_messages`, `analyze_websocket_messages` | 分析 WebSocket 通信 |
| 代码执行 | `evaluate_script`, `inspect_object` | 执行 JS 代码 |
| DOM 监控 | `monitor_events`, `take_screenshot` | 监控 DOM 事件 |

---

## 常见使用场景

### 场景 1: 分析加密参数生成逻辑

**目标**: 找到 API 请求中加密参数的生成位置

**工作流**:
```
1. list_network_requests()          # 查看网络请求
2. get_network_request(reqid)       # 获取目标请求详情
3. get_request_initiator(requestId) # 获取请求调用栈
4. search_in_sources(query)         # 搜索加密函数
5. set_breakpoint_on_text(text)     # 设置断点
6. 触发请求，等待断点命中
7. get_paused_info()                # 查看变量和调用栈
8. step_into() / step_over()        # 单步调试
```

**示例**:
```markdown
# 找 sign 参数生成位置
1. list_network_requests(resourceTypes=["xhr", "fetch"])
2. get_request_initiator(requestId) → 看到调用栈
3. search_in_sources("sign", excludeMinified=false)
4. set_breakpoint_on_text("encrypt")
5. 触发请求
6. get_paused_info(includeScopes=true) → 查看参数值
```

### 场景 2: 分析 WebSocket 消息格式

**目标**: 理解 WebSocket 消息结构（特别是二进制/protobuf）

**工作流**:
```
1. list_websocket_connections()           # 列出 WS 连接
2. analyze_websocket_messages(wsid)       # 分析消息类型分组
3. get_websocket_messages(wsid, groupId="A") # 查看特定类型消息
4. get_websocket_message(wsid, frameIndex)   # 获取完整消息内容
```

**示例**:
```markdown
# 分析直播流消息
1. list_websocket_connections(urlFilter="live")
2. analyze_websocket_messages(wsid)
   → 返回: Group A (心跳), Group B (弹幕), Group C (礼物)
3. get_websocket_messages(wsid, groupId="B", show_content=true)
```

### 场景 3: Hook 函数监控调用

**目标**: 监控特定函数的调用参数和返回值

**工作流**:
```
1. hook_function(target, logArgs=true, logResult=true)
2. 触发函数调用
3. list_console_messages()  # 查看日志输出
4. unhook_function(hookId)   # 移除 hook
```

**示例**:
```markdown
# Hook fetch 请求
1. hook_function("fetch", logStack=true)
2. hook_function("XMLHttpRequest.prototype.open")
3. hook_function("window.encrypt", logArgs=true, logResult=true)
4. 触发操作
5. list_console_messages(types=["log"])
```

### 场景 4: 调试混淆代码

**目标**: 在压缩/混淆代码中设置断点

**工作流**:
```
1. list_scripts(filter="app")              # 找到目标脚本
2. get_script_source(scriptId)             # 查看源码
3. find_in_script(scriptId, query)         # 精确定位代码位置
4. set_breakpoint(url, lineNumber, condition) # 条件断点
```

**示例**:
```markdown
# 在单行压缩代码中定位
1. get_script_source(scriptId, offset=0, length=5000)
2. find_in_script(scriptId, "password", contextChars=200)
   → 返回: {lineNumber, columnNumber, context}
3. set_breakpoint(url, lineNumber, condition="password.length > 6")
```

### 场景 5: 分析对象结构

**目标**: 深入理解复杂对象的结构和方法

**工作流**:
```
1. inspect_object(expression, depth=3, showMethods=true)
2. 或使用 evaluate_script() 执行自定义代码
```

**示例**:
```markdown
# 分析全局配置对象
inspect_object("window.__APP_CONFIG__", depth=2)

# 分析 Vue/React 实例
inspect_object("__VUE_APP__.$store.state", depth=3, showPrototype=true)
```

---

## 工具详细说明

### 页面管理

| 工具 | 说明 | 参数 |
|------|------|------|
| `list_pages` | 列出所有打开的页面 | 无 |
| `select_page` | 选择目标页面 | `pageIdx` |
| `new_page` | 打开新页面 | `url`, `timeout` |
| `navigate_page` | 页面导航 | `type`: url/back/forward/reload, `url` |
| `list_frames` | 列出所有 iframe | 无 |
| `select_frame` | 选择 iframe 上下文 | `frameIdx` |

### 脚本分析

| 工具 | 说明 | 参数 |
|------|------|------|
| `list_scripts` | 列出所有脚本 | `filter` (可选过滤) |
| `get_script_source` | 获取脚本源码 | `scriptId`, `startLine`/`endLine` 或 `offset`/`length` |
| `find_in_script` | 在脚本中搜索 | `scriptId`, `query`, `contextChars`, `occurrence` |
| `search_in_sources` | 全局搜索 | `query`, `isRegex`, `excludeMinified`, `maxResults` |

### 断点调试

| 工具 | 说明 | 参数 |
|------|------|------|
| `set_breakpoint` | 设置行断点 | `url`, `lineNumber`, `columnNumber`, `condition` |
| `set_breakpoint_on_text` | 按文本设置断点 | `text`, `urlFilter`, `condition`, `occurrence` |
| `list_breakpoints` | 列出所有断点 | 无 |
| `remove_breakpoint` | 移除断点 | `breakpointId` |
| `pause` | 暂停执行 | 无 |
| `resume` | 继续执行 | 无 |
| `step_over` | 单步跳过 | 无 |
| `step_into` | 单步进入 | 无 |
| `step_out` | 单步跳出 | 无 |
| `get_paused_info` | 获取暂停状态 | `includeScopes`, `maxScopeDepth` |
| `evaluate_on_callframe` | 在调用栈中求值 | `expression`, `frameIndex` |

### 函数追踪

| 工具 | 说明 | 参数 |
|------|------|------|
| `hook_function` | Hook 函数 | `target`, `logArgs`, `logResult`, `logStack` |
| `trace_function` | 追踪函数调用 | `functionName`, `logArgs`, `pause` |
| `list_hooks` | 列出所有 Hook | 无 |
| `unhook_function` | 移除 Hook | `hookId` |

**Hook vs Trace 区别**:
- `hook_function`: 实际修改函数，适合全局监控
- `trace_function`: 使用 logpoint，不暂停执行，适合追踪内部函数

### 网络监控

| 工具 | 说明 | 参数 |
|------|------|------|
| `list_network_requests` | 列出网络请求 | `resourceTypes`, `pageIdx`, `pageSize` |
| `get_network_request` | 获取请求详情 | `reqid` (可选) |
| `get_request_initiator` | 获取请求调用栈 | `requestId` |
| `break_on_xhr` | XHR 断点 | `url` |
| `remove_xhr_breakpoint` | 移除 XHR 断点 | `url` |

### WebSocket

| 工具 | 说明 | 参数 |
|------|------|------|
| `list_websocket_connections` | 列出 WS 连接 | `urlFilter`, `includePreservedConnections` |
| `analyze_websocket_messages` | 分析消息分组 | `wsid`, `direction` |
| `get_websocket_messages` | 获取消息列表 | `wsid`, `groupId`, `direction`, `show_content` |
| `get_websocket_message` | 获取单条消息 | `wsid`, `frameIndex` |

**重要**: 对于二进制消息，先用 `analyze_websocket_messages` 分组，再按 `groupId` 筛选。

### 代码执行与对象检查

| 工具 | 说明 | 参数 |
|------|------|------|
| `evaluate_script` | 执行 JS 代码 | `function` (函数体字符串) |
| `inspect_object` | 深度检查对象 | `expression`, `depth`, `showMethods`, `showPrototype` |
| `get_storage` | 获取存储数据 | `type`: all/cookies/localStorage/sessionStorage, `filter` |

### DOM 与事件

| 工具 | 说明 | 参数 |
|------|------|------|
| `monitor_events` | 监控 DOM 事件 | `selector`, `events`, `monitorId` |
| `stop_monitor` | 停止监控 | `monitorId` |
| `take_screenshot` | 截图 | `filePath`, `fullPage`, `format`, `quality` |

### 控制台

| 工具 | 说明 | 参数 |
|------|------|------|
| `list_console_messages` | 列出控制台消息 | `types`, `pageIdx`, `pageSize` |
| `get_console_message` | 获取消息详情 | `msgid` |

---

## 最佳实践

### 1. 定位加密函数

```markdown
# 推荐流程
1. 先用 get_request_initiator 找到请求发起位置
2. 用 search_in_sources 搜索关键字符串
3. 用 set_breakpoint_on_text 设置断点（支持混淆代码）
4. 触发请求，用 get_paused_info 查看变量
5. 用 step_into 进入函数内部
```

### 2. 分析大型脚本

```markdown
# 对于单行压缩代码
1. get_script_source(scriptId, offset=0, length=2000)  # 分段读取
2. find_in_script(scriptId, "targetFunc", contextChars=500)  # 精确定位
3. 使用 set_breakpoint_on_text 自动定位断点
```

### 3. 复杂对象分析

```markdown
# 深度分析
inspect_object("obj", depth=3, showMethods=true, showPrototype=true)

# 执行复杂查询
evaluate_script("() => { return Object.keys(window).filter(k => k.includes('app')) }")
```

### 4. 条件断点

```markdown
# 只在特定条件下断点
set_breakpoint(url, lineNumber, condition="userId === '123'")
set_breakpoint_on_text("submit", condition="form.valid === true")
```

---

## 注意事项

### [!] 重要限制

1. **Minified 代码处理**: 单行代码使用 `offset`/`length` 而非 `startLine`/`endLine`
2. **WebSocket 二进制消息**: 必须先用 `analyze_websocket_messages` 分组
3. **跨 iframe 操作**: 用 `select_frame` 切换上下文
4. **Hook 清理**: 用完后记得 `unhook_function`，避免影响页面行为

### [!] 性能建议

1. **大文件搜索**: 使用 `excludeMinified=true` 过滤压缩文件
2. **网络请求**: 使用 `resourceTypes` 过滤，避免返回过多数据
3. **WebSocket**: 使用 `groupId` 筛选，避免加载所有消息

### [!] 调试技巧

1. **找不到断点位置**: 用 `find_in_script` 精确定位
2. **断点不命中**: 检查是否在正确的 frame 上下文
3. **变量看不到**: 确保 `includeScopes=true` 并增加 `maxScopeDepth`

---

## 常见问题

### Q: 如何找到某个 API 的加密逻辑？

```markdown
1. list_network_requests(resourceTypes=["xhr", "fetch"])
2. 找到目标请求，记录 reqid
3. get_request_initiator(reqid) → 获取调用栈
4. 根据调用栈定位到代码位置
5. set_breakpoint_on_text("加密函数名或关键代码")
6. 重新触发请求
7. get_paused_info() 分析参数
```

### Q: 如何分析全局变量？

```markdown
# 方法 1: 直接检查
inspect_object("window.myGlobalVar", depth=2)

# 方法 2: 列出所有匹配变量
evaluate_script("() => Object.keys(window).filter(k => k.includes('Config'))")
```

### Q: 如何分析 WebSocket Protobuf？

```markdown
1. analyze_websocket_messages(wsid) → 按指纹分组
2. get_websocket_messages(wsid, groupId="X", show_content=true)
3. 根据消息模式推测 protobuf 结构
```

---

## 工具选择速查表

| 目标 | 推荐工具 |
|------|----------|
| 找 API 调用位置 | `get_request_initiator` |
| 搜索代码字符串 | `search_in_sources` / `find_in_script` |
| 监控函数调用 | `hook_function` / `trace_function` |
| 分析变量值 | `get_paused_info` / `evaluate_on_callframe` |
| 分析对象结构 | `inspect_object` |
| 分析网络请求 | `list_network_requests` / `get_network_request` |
| 分析 WebSocket | `analyze_websocket_messages` / `get_websocket_messages` |
| 在混淆代码中断点 | `set_breakpoint_on_text` |
| 单步调试 | `step_into` / `step_over` / `step_out` |

---

**核心原则**: 先定位（调用栈/搜索），再断点，最后分析（变量/对象）。