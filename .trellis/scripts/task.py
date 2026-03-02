#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
任务管理脚本
支持: create, start, finish, archive, init-context, add-context
"""

import os
import sys
import json
import argparse
from pathlib import Path
from datetime import datetime

# Windows 控制台编码处理
if sys.platform == 'win32':
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')

TASKS_DIR = Path(".trellis/tasks")
CURRENT_TASK_FILE = Path(".trellis/.current-task")

def create_task(title: str, slug: str = None):
    """创建新任务目录"""
    if not slug:
        # 从标题生成 slug
        slug = title.lower().replace(" ", "-").replace("_", "-")
        slug = "".join(c for c in slug if c.isalnum() or c == "-")

    task_dir = TASKS_DIR / slug
    if task_dir.exists():
        print(f"[错误] 任务目录已存在: {slug}")
        return None

    task_dir.mkdir(parents=True, exist_ok=True)

    # 创建 task.json
    task_data = {
        "title": title,
        "slug": slug,
        "status": "pending",
        "phase": "created",
        "created_at": datetime.now().isoformat(),
        "updated_at": datetime.now().isoformat()
    }
    (task_dir / "task.json").write_text(json.dumps(task_data, indent=2, ensure_ascii=False), encoding='utf-8')

    # 创建 prd.md 模板
    prd_content = f"""# {title}

## 目标
<!-- 描述这个任务要达成什么 -->

## 需求
- [ ] 需求1
- [ ] 需求2

## 验收标准
- [ ] 标准1
- [ ] 标准2

## 技术说明
<!-- 技术决策、约束、注意事项 -->

## 进度
- [ ] 创建任务
- [ ] 编写PRD
- [ ] 研究代码库
- [ ] 实现功能
- [ ] 代码检查
- [ ] 测试验证

---
创建时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
"""
    (task_dir / "prd.md").write_text(prd_content, encoding='utf-8')

    print(f"[完成] 任务已创建: {slug}")
    print(f"   目录: {task_dir}")
    return str(task_dir)

def start_task(slug: str):
    """开始任务"""
    task_dir = TASKS_DIR / slug
    if not task_dir.exists():
        print(f"[错误] 任务不存在: {slug}")
        return False

    # 更新 task.json
    task_json = task_dir / "task.json"
    task_data = json.loads(task_json.read_text(encoding='utf-8'))
    task_data["status"] = "in_progress"
    task_data["phase"] = "implementation"
    task_data["updated_at"] = datetime.now().isoformat()
    task_json.write_text(json.dumps(task_data, indent=2, ensure_ascii=False), encoding='utf-8')

    # 设置当前任务
    CURRENT_TASK_FILE.write_text(slug, encoding='utf-8')

    print(f"[完成] 任务已激活: {slug}")
    print(f"   标题: {task_data['title']}")
    return True

def finish_task(slug: str = None):
    """完成任务"""
    if not slug:
        if not CURRENT_TASK_FILE.exists():
            print("[错误] 没有活动任务")
            return False
        slug = CURRENT_TASK_FILE.read_text(encoding='utf-8').strip()

    task_dir = TASKS_DIR / slug
    if not task_dir.exists():
        print(f"[错误] 任务不存在: {slug}")
        return False

    # 更新 task.json
    task_json = task_dir / "task.json"
    task_data = json.loads(task_json.read_text(encoding='utf-8'))
    task_data["status"] = "completed"
    task_data["phase"] = "done"
    task_data["updated_at"] = datetime.now().isoformat()
    task_json.write_text(json.dumps(task_data, indent=2, ensure_ascii=False), encoding='utf-8')

    # 清除当前任务
    if CURRENT_TASK_FILE.exists():
        CURRENT_TASK_FILE.unlink()

    print(f"[完成] 任务已完成: {slug}")
    return True

def archive_task(slug: str):
    """归档任务"""
    task_dir = TASKS_DIR / slug
    if not task_dir.exists():
        print(f"[错误] 任务不存在: {slug}")
        return False

    # 创建归档目录
    archive_dir = TASKS_DIR / "_archived"
    archive_dir.mkdir(exist_ok=True)

    # 移动任务
    new_path = archive_dir / f"{slug}-{datetime.now().strftime('%Y%m%d%H%M%S')}"
    task_dir.rename(new_path)

    print(f"[完成] 任务已归档: {slug}")
    return True

def init_context(slug: str, task_type: str):
    """初始化上下文文件"""
    task_dir = TASKS_DIR / slug
    if not task_dir.exists():
        print(f"[错误] 任务不存在: {slug}")
        return False

    # 创建空的 jsonl 文件
    (task_dir / "implement.jsonl").write_text("", encoding='utf-8')
    (task_dir / "check.jsonl").write_text("", encoding='utf-8')

    print(f"[完成] 已初始化上下文: {slug}")
    print(f"   类型: {task_type}")
    return True

def add_context(slug: str, phase: str, file_path: str, reason: str):
    """添加上下文文件"""
    task_dir = TASKS_DIR / slug
    if not task_dir.exists():
        print(f"[错误] 任务不存在: {slug}")
        return False

    jsonl_file = task_dir / f"{phase}.jsonl"
    entry = {
        "path": file_path,
        "reason": reason,
        "added_at": datetime.now().isoformat()
    }
    with open(jsonl_file, "a", encoding="utf-8") as f:
        f.write(json.dumps(entry, ensure_ascii=False) + "\n")

    print(f"[完成] 已添加上下文: {file_path}")
    print(f"   阶段: {phase}")
    print(f"   原因: {reason}")
    return True

def main():
    parser = argparse.ArgumentParser(description="任务管理工具")
    subparsers = parser.add_subparsers(dest="command", required=True)

    # create 命令
    create_parser = subparsers.add_parser("create", help="创建新任务")
    create_parser.add_argument("title", help="任务标题")
    create_parser.add_argument("--slug", help="任务标识符")

    # start 命令
    start_parser = subparsers.add_parser("start", help="开始任务")
    start_parser.add_argument("slug", help="任务标识符")

    # finish 命令
    finish_parser = subparsers.add_parser("finish", help="完成任务")
    finish_parser.add_argument("slug", nargs="?", help="任务标识符")

    # archive 命令
    archive_parser = subparsers.add_parser("archive", help="归档任务")
    archive_parser.add_argument("slug", help="任务标识符")

    # init-context 命令
    init_parser = subparsers.add_parser("init-context", help="初始化上下文")
    init_parser.add_argument("slug", help="任务标识符")
    init_parser.add_argument("type", choices=["backend", "frontend", "fullstack"], help="任务类型")

    # add-context 命令
    add_parser = subparsers.add_parser("add-context", help="添加上下文")
    add_parser.add_argument("slug", help="任务标识符")
    add_parser.add_argument("phase", choices=["implement", "check"], help="阶段")
    add_parser.add_argument("path", help="文件路径")
    add_parser.add_argument("reason", help="添加原因")

    args = parser.parse_args()

    TASKS_DIR.mkdir(parents=True, exist_ok=True)

    if args.command == "create":
        create_task(args.title, args.slug)
    elif args.command == "start":
        start_task(args.slug)
    elif args.command == "finish":
        finish_task(args.slug)
    elif args.command == "archive":
        archive_task(args.slug)
    elif args.command == "init-context":
        init_context(args.slug, args.type)
    elif args.command == "add-context":
        add_context(args.slug, args.phase, args.path, args.reason)

if __name__ == "__main__":
    main()