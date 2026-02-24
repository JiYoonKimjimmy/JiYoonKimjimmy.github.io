---
layout: post
title : Claude Code Custom Slash 명령어 제작
date  : 2026-02-24
image : git-worktree-claude-commands.png
tags  : [git, worktree, claude, workflow, slash-commands]
---

# Claude Code Custom Slash 명령어: Git Worktree Workflow

Git worktree 기반 병렬 개발 워크플로우를 자동화하는 Claude Code 커스텀 슬래시 명령어 세트이다.

## 개요

| 명령어 | 설명 |
|--------|------|
| `/start-git-worktree` | Jira 티켓번호로 worktree 생성 |
| `/finish-git-worktree` | worktree를 현재 브랜치에 merge 후 제거 |

### 워크플로우

```
/start-git-worktree PROJ-123 add-login
        │
        ▼
  worktree에서 작업 & 커밋
        │
        ▼
  원본 프로젝트로 복귀
        │
        ▼
/finish-git-worktree
  → worktree 선택
  → merge + worktree 제거
```

## 설치

- 대상 경로: `~/.claude/commands/`
- 복사할 파일: `start-git-worktree.md`, `finish-git-worktree.md`

```
~/.claude/commands/
├── start-git-worktree.md
└── finish-git-worktree.md
```

---

## `/start-git-worktree`

Jira 티켓번호 기반으로 새 worktree와 feature 브랜치를 생성한다.

### 사용법

```
/start-git-worktree <Jira-ticket-number> [title]
```

### 예시

```
/start-git-worktree PROJ-123
/start-git-worktree PROJ-123 add-login-page
```

### 생성 규칙

- **브랜치명**: `feature/<Jira티켓>-feat/<yyyyMMdd>-<index>[-<title>]`
  - 동일 날짜 여러 번 생성 시 index 자동 증가 (`01`, `02`, ...)
- **Worktree 경로**: `../<project-name>-<Jira티켓>-<yyyyMMdd>-<index>[-<title>]`

```
feature/PROJ-123-feat/20260224-01
feature/PROJ-123-feat/20260224-01-add-login-page

../my-app-PROJ-123-20260224-01
../my-app-PROJ-123-20260224-01-add-login-page
```

### 동작 순서

1. 인자에서 Jira 티켓번호·title 분리
2. 프로젝트 디렉토리명 추출
3. 날짜 + 자동 index로 브랜치명 생성
4. `git worktree add -b <branch> <path>` 실행
5. 결과 요약 출력 (base branch, 브랜치명, worktree 경로)

### 허용 도구

- `Bash(git:*)`, `Bash(date:*)`, `Bash(basename:*)`, `Bash(ls:*)`

---

## `/finish-git-worktree`

선택한 worktree의 브랜치를 현재 브랜치에 merge한 뒤 worktree를 제거한다.

### 사용법

```
/finish-git-worktree
```

- 인자 없음
- 원본 프로젝트(main repo)에서 실행

### 동작 순서

1. **환경 검증** — worktree 목록 조회, 제거 대상 없으면 중단
2. **사용자 선택** — main repo 제외 worktree 목록 표시 후 선택
3. **Uncommitted changes 확인** — 변경사항 있으면 경고 후 중단
4. **Merge** — 선택 worktree 브랜치를 현재 브랜치에 merge (충돌 시 abort 후 중단)
5. **Worktree 제거** — `git worktree remove` 실행
6. **결과 요약** — merge target, merge된 브랜치, 제거 경로, 남은 worktree 출력

### 동작 정책

| 항목 | 정책 |
|------|------|
| Uncommitted changes | 경고 후 중단 |
| Remote push | 미실행 |
| Merge 방향 | worktree 브랜치 → 현재 브랜치 |
| Merge 충돌 | abort 후 중단 |
| 로컬 브랜치 | 유지 (삭제 안 함) |

### 허용 도구

- `Bash(git:*)`

---

## 전체 사용 예시

```bash
# 1. worktree 생성
$ claude
> /start-git-worktree PROJ-456 user-auth

# 출력: Base branch, 브랜치명, 경로

# 2. worktree로 이동 후 작업·커밋
$ cd ../my-app-PROJ-456-20260224-01-user-auth
$ claude
# ... 작업 및 커밋 ...

# 3. 원본으로 복귀 후 worktree 정리
$ cd ../my-app
$ claude
> /finish-git-worktree

# 출력: Worktree 목록 → 선택 → merge → 제거 → 결과 요약
```

---

## 명령어 파일 소스

### start-git-worktree.md

````markdown
---
description: Jira 티켓번호로 git worktree 생성
argument-hint: <Jira-ticket-number> [title]
allowed-tools: Bash(git:*), Bash(date:*), Bash(basename:*), Bash(ls:*)
---

# Git Worktree 생성

아래 정보를 참고하여 git worktree를 생성하라.

## 현재 환경

- 프로젝트 루트: `!git rev-parse --show-toplevel`
- 현재 브랜치 (base branch): `!git branch --show-current`
- 오늘 날짜: `!date +%Y%m%d`

## 실행 절차

### 1. 인자 파싱

`$ARGUMENTS`에서 첫 번째 단어를 **Jira 티켓번호**, 나머지를 **title**로 분리한다.

- 인자 비어있으면 에러 출력 후 중단: "사용법: /start-git-worktree <Jira-ticket-number> [title]"
- 예: `PROJ-123` → 티켓: `PROJ-123`, title: 없음
- 예: `PROJ-123 add-login-page` → 티켓: `PROJ-123`, title: `add-login-page`

### 2. 프로젝트 디렉토리명 추출

`basename $(git rev-parse --show-toplevel)` 로 프로젝트 디렉토리명 추출

### 3. 브랜치명 생성

형식: `feature/<Jira티켓>-feat/<yyyyMMdd>-<index>[-<title>]`

- `<yyyyMMdd>`: 오늘 날짜 (`date +%Y%m%d`)
- index: `git branch -a --list "feature/<Jira티켓>-feat/<yyyyMMdd>-*"` 조회 후 없으면 `01`, 있으면 max+1 (2자리 zero-pad)
- title 있으면 index 뒤에 `-<title>` 추가

### 4. Worktree 경로 결정

형식: `../<project-name>-<Jira티켓>-<yyyyMMdd>-<index>[-<title>]`

### 5. Worktree 생성 실행

```
git worktree add -b <branch-name> <worktree-path>
```

### 6. 결과 요약 출력

- Base branch, 브랜치명, Worktree 경로, `cd 명령어` 안내

## 중요 사항

- 각 단계를 Bash 도구로 실행
- 에러 시 즉시 사용자에게 알림
- 브랜치명·경로 오타 주의
````

### finish-git-worktree.md

````markdown
---
description: git worktree 목록에서 선택하여 merge 후 제거
allowed-tools: Bash(git:*)
---

# Git Worktree 정리 (Merge 후 제거)

사용자가 선택한 worktree를 현재 브랜치에 merge한 뒤 제거한다.

## 현재 환경

- Worktree 목록: `!git worktree list`
- 현재 브랜치: `!git branch --show-current`

## 실행 절차

### 1. 환경 검증

- main repo(첫 번째 항목) 제외 worktree 확인
- 제거 대상 없으면 "제거할 worktree가 없습니다." 출력 후 중단

### 2. Worktree 목록 표시 및 사용자 선택

- main repo 제외 worktree 목록 정리
- AskUserQuestion 도구로 제거할 worktree 선택 요청 (label: 브랜치명, description: 경로)

### 3. Uncommitted changes 확인

- `git -C <선택된-worktree-경로> status --porcelain` 실행
- 출력 있으면 "커밋되지 않은 변경사항이 있습니다. 먼저 커밋하거나 stash한 후 다시 실행하세요." 출력 후 중단

### 4. Merge

- `git merge <worktree-branch>` 실행
- 충돌 시 `git merge --abort` 후 "merge 충돌이 발생했습니다. 수동으로 merge를 진행하세요." 출력 후 중단

### 5. Worktree 제거

- `git worktree remove <선택된-worktree-경로>` 실행
- 에러 시 사용자에게 알리고 중단

### 6. 결과 요약 출력

- merge target, merge된 브랜치, 제거된 경로
- "로컬 브랜치는 유지됩니다. 필요 시 `git branch -d <브랜치명>`으로 삭제하세요."
- `git worktree list` 실행하여 남은 worktree 표시

## 중요 사항

- 각 단계를 Bash 도구로 실행
- remote push 미실행
- worktree 제거 후 로컬 브랜치 삭제 안 함
- 에러 시 즉시 사용자에게 알림
````
