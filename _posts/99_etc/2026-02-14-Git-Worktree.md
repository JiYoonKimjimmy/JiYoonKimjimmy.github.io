---
layout: post
title : Git Worktree - 하나의 저장소에 여러 작업 디렉토리 관리하기
date  : 2026-02-14
image : git-worktree.png
tags  : [git, worktree, 브랜치, 워크플로우]
---

## Git Worktree란?

`Git Worktree`는 **하나의 Git 저장소에서 여러 작업 디렉토리(Working Tree)를 동시에 사용**하는 기능이다. Git 2.5부터 제공되며, `.git` 객체와 히스토리를 공유하면서 각 워크트리마다 서로 다른 브랜치를 체크아웃할 수 있다.

### 기존 방식의 한계

| 방식 | 문제점 |
|------|--------|
| `git stash` / `checkout` | stash/unstash 반복, 변경사항 유실 위험 |
| WIP 커밋 | 불필요한 히스토리 생성 |
| 저장소 복제 (`clone`) | `.git` 전체 복제로 디스크 낭비, fetch/pull 중복 |

### Worktree 장점

- 브랜치 전환 없이 **병렬 작업** 가능
- `.git` 공유로 **디스크 절약**
- 작업별 독립 디렉토리로 **IDE·터미널 컨텍스트 유지**

---

## 기본 명령어

### 1. add - 워크트리 추가

**기존 브랜치 체크아웃:**

```bash
git worktree add <경로> <브랜치명>
git worktree add ../project-hotfix main
```

**새 브랜치 생성 후 추가:**

```bash
git worktree add -b feature/new-api ../project-feature main
```

**Detached HEAD (실험/테스트):**

```bash
git worktree add --detach ../project-experiment HEAD
```

### 2. list - 목록 확인

```bash
git worktree list
git worktree list -v              # locked, prunable 등 상세
git worktree list --porcelain     # 스크립트용
```

### 3. remove - 워크트리 제거

```bash
git worktree remove ../project-hotfix
git worktree remove --force ../project-hotfix   # 변경사항 있어도 강제 제거
```

### 4. move - 경로 변경

```bash
git worktree move ../project-hotfix /new/path/project-hotfix
```

### 5. prune / repair - 정리·복구

```bash
git worktree prune        # 삭제된 워크트리 메타데이터 정리
git worktree prune -n    # dry-run
git worktree repair      # 경로 변경 후 링크 복구 (Git 2.50+)
```

### 6. lock / unlock

외장/네트워크 드라이브에 둔 워크트리는 마운트 해제 시 prune 대상이 된다. `lock`으로 보호한다.

```bash
git worktree lock ../project-on-usb --reason "USB drive"
git worktree unlock ../project-on-usb
```

---

## 실무 활용 시나리오

### 긴급 핫픽스

리팩토링 중 프로덕션 버그 발생 시, 기존 작업을 두고 별도 워크트리에서 수정한다.

```bash
git worktree add -b emergency-fix ../temp-hotfix main
cd ../temp-hotfix
# ... 수정 및 커밋 ...
git push origin emergency-fix
cd -
git worktree remove ../temp-hotfix
```

### PR 코드 리뷰

리뷰할 브랜치를 별도 디렉토리에서 체크아웃해 실행·테스트한다.

```bash
git worktree add ../pr-review origin/feature/team-member-pr
```

### 멀티 브랜치 병렬 개발

기능별로 독립 디렉토리를 두고 컨텍스트 스위칭 없이 작업한다.

```bash
git worktree add -b feature/auth ../project-auth main
git worktree add -b feature/payment ../project-payment main
```

### 실험용 (Detached HEAD)

```bash
git worktree add --detach ../experiment HEAD
```

---

## 주의사항

1. **동일 브랜치 중복 체크아웃 불가** — 한 브랜치는 하나의 워크트리에서만 사용 가능
2. **서브모듈** — 워크트리 관련 동작이 실험적. [공식 문서](https://git-scm.com/docs/git-worktree#BUGS) 참고
3. **메인 워크트리 제거 불가**
4. **상대 경로** — 저장소 이전 시: `git config worktree.useRelativePaths true`

---

## 명령어 요약

| 명령어 | 용도 |
|--------|------|
| `git worktree add <경로> [브랜치]` | 워크트리 추가 |
| `git worktree add -b <새브랜치> <경로> <기반브랜치>` | 새 브랜치 생성 후 추가 |
| `git worktree list` | 목록 조회 |
| `git worktree remove <경로>` | 제거 |
| `git worktree prune` | 삭제된 워크트리 메타데이터 정리 |
| `git worktree repair` | 링크 복구 |
