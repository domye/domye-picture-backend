#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
获取当前会话上下文
显示: 开发者身份、Git状态、当前任务、活动任务列表
"""

import os
import sys
import json
import subprocess
from pathlib import Path
from datetime import datetime

# Windows 控制台编码处理
if sys.platform == 'win32':
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    if hasattr(sys.stderr, 'reconfigure'):
        sys.stderr.reconfigure(encoding='utf-8', errors='replace')

def run_git_command(cmd):
    """运行 git 命令并返回输出"""
    try:
        # Windows 下使用 gbk 编码读取 git 输出
        encoding = 'gbk' if sys.platform == 'win32' else 'utf-8'
        result = subprocess.run(
            cmd, shell=True, capture_output=True, cwd=os.getcwd()
        )
        output = result.stdout.decode(encoding, errors='replace').strip()
        return output
    except Exception as e:
        return ""

def get_git_status():
    """获取 Git 状态"""
    branch = run_git_command("git branch --show-current")
    status = run_git_command("git status --short")
    ahead = run_git_command("git rev-list --count @{upstream}..HEAD 2>nul")
    behind = run_git_command("git rev-list --count HEAD..@{upstream} 2>nul")

    return {
        "branch": branch or "unknown",
        "status": status or "clean",
        "ahead": ahead.strip("'") if ahead else "0",
        "behind": behind.strip("'") if behind else "0"
    }

def get_current_task():
    """获取当前活动任务"""
    current_task_file = Path(".trellis/.current-task")
    if current_task_file.exists():
        task_slug = current_task_file.read_text(encoding='utf-8').strip()
        task_dir = Path(f".trellis/tasks/{task_slug}")
        if task_dir.exists():
            task_json = task_dir / "task.json"
            if task_json.exists():
                task_data = json.loads(task_json.read_text(encoding='utf-8'))
                return {
                    "slug": task_slug,
                    "title": task_data.get("title", "Unknown"),
                    "phase": task_data.get("phase", "unknown"),
                    "status": task_data.get("status", "unknown")
                }
    return None

def list_active_tasks():
    """列出所有活动任务"""
    tasks_dir = Path(".trellis/tasks")
    if not tasks_dir.exists():
        return []

    tasks = []
    for task_dir in tasks_dir.iterdir():
        if task_dir.is_dir():
            task_json = task_dir / "task.json"
            if task_json.exists():
                task_data = json.loads(task_json.read_text(encoding='utf-8'))
                status = task_data.get("status", "unknown")
                if status in ["pending", "in_progress"]:
                    tasks.append({
                        "slug": task_dir.name,
                        "title": task_data.get("title", "Unknown"),
                        "status": status
                    })
    return tasks

def main():
    print("=" * 50)
    print("Trellis 会话上下文")
    print("=" * 50)
    print()

    # Git 状态
    git = get_git_status()
    print("[Git] 状态")
    print(f"   分支: {git['branch']}")
    status_display = git['status'][:100] + '...' if len(git['status']) > 100 else git['status']
    print(f"   状态: {status_display}")
    print(f"   领先/落后: +{git['ahead']} / -{git['behind']}")
    print()

    # 当前任务
    current_task = get_current_task()
    print("[任务] 当前任务")
    if current_task:
        print(f"   {current_task['title']} ({current_task['slug']})")
        print(f"   阶段: {current_task['phase']}")
        print(f"   状态: {current_task['status']}")
    else:
        print("   无活动任务")
    print()

    # 活动任务列表
    tasks = list_active_tasks()
    print(f"[任务列表] 活动任务 ({len(tasks)})")
    if tasks:
        for task in tasks:
            print(f"   - [{task['status']}] {task['title']}")
    else:
        print("   无活动任务")
    print()

    print(f"[时间] 会话时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 50)

if __name__ == "__main__":
    main()