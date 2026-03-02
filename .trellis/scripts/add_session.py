#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
会话记录脚本
将开发会话追加到 journal 文件
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

JOURNAL_DIR = Path(".trellis/journal")
INDEX_FILE = JOURNAL_DIR / "index.md"
MAX_LINES = 2000


def get_current_journal_file():
    """获取当前 journal 文件"""
    JOURNAL_DIR.mkdir(parents=True, exist_ok=True)

    # 查找最新的 journal 文件
    journal_files = sorted(JOURNAL_DIR.glob("journal-*.md"))
    if not journal_files:
        return JOURNAL_DIR / "journal-1.md"

    latest = journal_files[-1]
    lines = len(latest.read_text(encoding='utf-8').splitlines())
    if lines >= MAX_LINES:
        # 创建新文件
        next_num = int(latest.stem.split("-")[1]) + 1
        return JOURNAL_DIR / f"journal-{next_num}.md"

    return latest


def update_index(title: str, journal_file: Path, commits: list):
    """更新索引文件"""
    if not INDEX_FILE.exists():
        # 创建初始索引
        index_content = """# 开发日志索引

## 统计

| 指标 | 值 |
|------|-----|
| 总会话数 | 0 |
| 最后活动 | - |

## 历史

| 日期 | 会话 | 文件 |
|------|------|------|
"""
        INDEX_FILE.write_text(index_content, encoding='utf-8')

    # 读取当前索引
    content = INDEX_FILE.read_text(encoding='utf-8')
    lines = content.splitlines()

    # 更新统计
    total_sessions = 0
    for line in lines:
        if "总会话数" in line:
            parts = line.split("|")
            if len(parts) >= 3:
                total_sessions = int(parts[2].strip()) + 1
            break

    # 更新内容
    new_content = []
    for line in lines:
        if "总会话数" in line:
            parts = line.split("|")
            parts[2] = f" {total_sessions} "
            line = "|".join(parts)
        elif "最后活动" in line:
            parts = line.split("|")
            parts[2] = f" {datetime.now().strftime('%Y-%m-%d %H:%M')} "
            line = "|".join(parts)
        new_content.append(line)

    # 添加历史记录
    date_str = datetime.now().strftime('%Y-%m-%d')
    journal_name = journal_file.name
    commit_str = ", ".join(commits[:3]) if commits else "-"
    history_entry = f"| {date_str} | {title} | {journal_name} ({commit_str}) |"
    new_content.append(history_entry)

    INDEX_FILE.write_text("\n".join(new_content), encoding='utf-8')


def add_session(title: str, commits: str, summary: str = None, content: str = None):
    """添加会话记录"""
    journal_file = get_current_journal_file()

    # 解析 commits
    commit_list = [c.strip() for c in commits.split(",") if c.strip()]

    # 构建会话内容
    session_content = f"""
---

## {title}

**时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
**提交**: {', '.join(commit_list) if commit_list else '-'}

"""

    if summary:
        session_content += f"**摘要**: {summary}\n\n"

    if content:
        session_content += content + "\n"

    # 追加到 journal 文件
    with open(journal_file, "a", encoding="utf-8") as f:
        f.write(session_content)

    # 更新索引
    update_index(title, journal_file, commit_list)

    print(f"[完成] 会话已记录")
    print(f"   标题: {title}")
    print(f"   文件: {journal_file}")
    print(f"   提交: {', '.join(commit_list) if commit_list else '无'}")


def main():
    parser = argparse.ArgumentParser(description="记录开发会话")
    parser.add_argument("--title", required=True, help="会话标题")
    parser.add_argument("--commit", default="", help="提交哈希 (逗号分隔)")
    parser.add_argument("--summary", default="", help="简短摘要")

    args = parser.parse_args()

    # 从 stdin 读取详细内容
    content = None
    if not sys.stdin.isatty():
        content = sys.stdin.read().strip()

    add_session(args.title, args.commit, args.summary, content)


if __name__ == "__main__":
    main()