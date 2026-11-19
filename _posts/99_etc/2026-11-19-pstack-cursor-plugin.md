---
layout: post
title : 깊게, 적게, 검증해서 — pstack(Cursor 플러그인) 스터디 정리
date  : 2026-11-19
image : pstack-cursor-plugin.svg
tags  : [claude, cursor, pstack, ai, agent, workflow]
---

## 목차

1. [pstack 이란](#1-pstack-이란)
2. [구성 한눈에 보기](#2-구성-한눈에-보기)
3. [poteto-mode 동작 방식](#3-poteto-mode-동작-방식)
4. [플레이북 22개](#4-플레이북-22개)
5. [스킬 34개](#5-스킬-34개)
6. [원칙 24개](#6-원칙-24개)
7. [기본 모델 구성](#7-기본-모델-구성)
8. [자율성 규칙과 계획에 대한 입장](#8-자율성-규칙과-계획에-대한-입장)
9. [Claude Code 에서 쓰는 법](#9-claude-code-에서-쓰는-법)
10. [내 개발 흐름에 적용한다면](#10-내-개발-흐름에-적용한다면)
11. [요약](#11-요약)

---

> 이 글은 Lauren Tan(poteto)이 만든 Cursor 플러그인 [pstack](https://github.com/cursor/plugins/tree/main/pstack)의 README, Claude Code 어댑터 문서(`docs/harness/claude-code.md`), 각 스킬의 `SKILL.md`를 읽고 나중에 찾아보기 좋게 정리한 것이다.
>
> 기준은 Claude Code 에 설치한 **v0.15.43**, 확인 시점은 **2026-11-18** 이다. 스킬 개수나 기본 모델은 버전이 바뀌면 달라질 수 있으므로, 갱신한 뒤에는 설치본을 다시 확인하는 편이 좋다.

## 1. pstack 이란

pstack 은 **코딩 에이전트가 따를 작업 절차(플레이북)와 판단 기준(원칙)을 스킬로 묶은 플러그인**이다. `cursor/plugins` 저장소의 `pstack` 폴더에서 관리되고, 라이선스는 MIT 다. Claude Code 용 생성본도 함께 배포되어 Claude Code 에도 설치할 수 있다.

만든 이유는 README 첫 문단에 그대로 나와 있다.

> there's a growing sense that ai writes too much slop code. i agree.
>
> if you want to go fast, go deep first.
>
> pstack helps you write less, but higher quality code.

정리하면 다음과 같다.

| 항목 | 내용 |
|---|---|
| 문제의식 | AI 가 품질 낮은 코드(slop)를 너무 많이 쓴다. 품질 없는 처리량은 목표가 아니다. |
| 표어 | **go deep first** — 빨리 가려면 먼저 깊게 간다. |
| 목표 | **write less** — 코드 줄 수를 늘리는 게 아니라, 더 적고 더 좋은 코드를 쓴다. |
| 기대 효과 | **fearless parallelism** — 에이전트 하나를 깊게 믿을 수 있으면 여러 에이전트를 안심하고 병렬로 돌릴 수 있다. |
| 다중 모델 | 모델마다 강점이 다르므로 리뷰·설계 같은 단계는 여러 모델을 함께 쓴다. |

## 2. 구성 한눈에 보기

| 구성 요소 | 개수 | 역할 |
|---|---|---|
| `poteto-mode` | 1 | 진입점. 요청을 플레이북에 대응시키고 다른 스킬을 호출한다. |
| 플레이북 | 22 | 버그 수정, 기능, 리팩토링, 성능, 조사, 출시 등 작업 유형별 절차 (`skills/poteto-mode/playbooks/`) |
| 스킬 | 34 | `how`, `why`, `architect`, `blast-radius`, `interrogate`, `arena`, `swarm`, `tdd`, `unslop` 등 (`poteto-mode` 포함) |
| 원칙 | 24 | `principle-*` 스킬. core·architecture·verification·delegation·meta 5개 그룹 |
| 서브에이전트 | 2 | `poteto-agent`(위임 작업용), `comment-sicko`(주석 리뷰어) |
| 훅 | 0 | 설치본에 `hooks/` 폴더도, `plugin.json` 의 훅 항목도 없다. |

번들 밖에 의존하는 것도 있다. `/deslop`, `control-cli`, `control-ui` 는 `cursor-team-kit` 플러그인에 있고, `/create-skill` 은 Cursor 내장 기능이다.

## 3. poteto-mode 동작 방식

`/poteto-mode` 는 사용자가 직접 입력해야 실행되는 스킬이다(`disable-model-invocation: true`). 실행되면 다음 순서로 일한다.

| 순서 | 동작 |
|---|---|
| 1 | 첫 응답에서 사용자의 목표와, 그 목표를 만족하는 **가장 작은 변경**을 다시 말한 뒤 진행한다. |
| 2 | 요청을 22개 플레이북 중 하나에 대응시킨다. 맞는 게 없거나 크고 여러 영역에 걸친 작업이면 `figure-it-out` 스킬로 보낸다. |
| 3 | 플레이북 단계를 할 일 목록에 그대로 복사한다. |
| 4 | 단계마다 필요한 스킬(`why`, `architect`, `arena`, `swarm`, `interrogate`, `unslop`, `tdd` 등)을 호출한다. |
| 5 | 답변에 **결정을 바꾼 원칙과, 그 원칙 때문에 바뀐 선택**을 밝힌다. 이번 세션에 원문(`SKILL.md`)을 읽은 원칙만 인용할 수 있다. |

`SKILL.md` 의 고정 규칙(Non-negotiables) 중 스킬 호출 기준은 이렇다.

| 상황 | 쓰는 스킬 |
|---|---|
| 코드를 쓴다 | 데이터 형태부터 정한다. 함수 경계를 넘으면 `architect` |
| 여러 갈래로 나눠 처리 | `swarm` |
| 여러 후보를 겨루게 함 | `arena` |
| 설계에 이견이 있음 | `interrogate` |
| 실행해서 결론 낼 수 있는 선택지 | 묻지 않고 `prototype` 플레이북으로 시험 |
| 글(답변 포함) | `unslop` |
| 문서, PR 설명, 커밋 메시지 | `technical-writing` |
| 커밋 전 / 리뷰 전 | `/deslop`(cursor-team-kit) / `no-comments` |
| 측정한 숫자 | `benchmark-checklist` |
| 길거나 사람 없이 도는 작업 | `show-me-your-work` |

답변 문체 규칙도 있다. 짧은 평서문을 쓰고, 주장마다 근거나 표시(측정함·추론함·추측)를 붙이고, **직접 돌려볼 수 있는 확인을 사람에게 넘기지 않는다.**

서브에이전트는 기본으로 새로 띄운다. 수정 라운드나 재시도도 이전 결과를 넘겨받은 새 서브에이전트에 맡기고, 기존 서브에이전트를 이어 쓰는 건 그 안에만 있는 상태(체크아웃, 커밋 안 한 변경, 실행 중인 프로세스)가 필요할 때뿐이다.

## 4. 플레이북 22개

파일은 `skills/poteto-mode/playbooks/<제목>.md` 에 있다. 각 플레이북은 "You own ..." 한 줄로 이 작업에서 책임질 대상을 먼저 못 박는다.

| 분류 | 플레이북 | 언제 / 핵심 |
|---|---|---|
| 조사 | Investigation | 읽기 전용. 코드 변경이 아니라 근거를 단 설명이나 권고를 낸다. |
| 조사 | Runtime forensics | 실행 중인 프로세스를 직접 계측해 원인을 찾는다(CPU 프로파일, 힙 스냅샷 등). 수정은 하지 않는다. |
| 조사 | Trace forensics | 이미 떠 둔 트레이스·덤프를 읽어 진단한다. 다시 실행하지 않는다. |
| 수정 | Bug fix | 출시하는 모든 줄이 실행 증거로 이어져야 한다. "도움이 될지도 모르는" 방어 코드는 가설일 뿐 출시하지 않는다. |
| 수정 | Perf issue | 모든 수정을 측정에 묶는다. 소스를 읽는 걸로 측정을 대신하지 않는다. |
| 수정 | Hillclimb | 측정 가능한 지표 하나를 목표까지 반복 개선. 변경 하나, 측정 하나, 유지 또는 되돌리기. |
| 구현 | Feature | 설계를 책임지고 구현은 위임한다. 서브시스템 추적 → `architect` → 구현 → 검증 |
| 구현 | Refactoring | 구조는 바꾸고 동작은 바꾸지 않는다. 버그나 빠진 기능이 나오면 따로 떼어낸다. |
| 구현 | Prototype | 버리는 실험용. 코드 품질보다 속도, 계획 없음. 올바른 설계를 싸게 고르는 게 목적이다. |
| 구현 | Visual parity | 기준 스크린샷과 픽셀 단위로 같게 만든다. 눈이 아니라 이미지 diff 로 검증한다. |
| 구현 | Authoring a skill | 스킬 작성·수정. 결정을 바꾸지 않는 문장은 지운다. |
| 실험 | Eval | 실험 설계를 책임진다. 블라인드 조건을 지켜 계획 → 실행 → 종합 |
| 출시 | Opening a PR | 다른 플레이북 끝에서 호출. worktree, 작은 커밋, `Why`/`What changed`/`Blast Radius`/`Verification` 섹션 |
| 출시 | Babysit | PR 상태 관리. 충돌 → 리뷰 스레드 → CI 순. rebase·force-push 는 하지 않고 보고만 한다. |
| 출시 | Shipping | 검증된 PR 만, 아래부터 하나씩 머지한다. 코드를 쓰지 않은 에이전트의 판정만 인정한다. |
| 자동 운영 | Autonomous run | 종료 조건을 검사 가능한 식으로 먼저 정하고, 멈추지 않고 그 조건까지 진행한다. |
| 자동 운영 | Autopilot-full | PR 마다 담당 에이전트가 머지까지 끌고 가고, 코드를 쓰지 않은 swarm 이 검증한다. |
| 자동 운영 | Autopilot-stack | Autopilot-full 과 같지만 머지하지 않고 "검증 완료"에서 멈춘다. |
| 세션 | Session pickup | 이전 세션 기록을 읽고 이어서 한다. 끝난 일을 처음부터 다시 검증하지 않는다. |
| 세션 | Pause safely | 명시 요청 시에만. 안전한 지점에서 멈추고 `wip:` 커밋과 재개 메모를 남긴다. |
| 계획 | Multi-phase plan | 계획 문서 자체가 결과물이다. 구현하지 않는다. 한두 파일짜리 변경이면 계획을 건너뛴다. |
| 정리 | Worktree cleanup | 쓰지 않는 worktree·시뮬레이터 정리. 커밋 안 한 작업이 있으면 멈추고 확인받는다. |

## 5. 스킬 34개

`poteto-mode` 가 단계마다 알아서 부르는 스킬이 많지만, 직접 부를 때 찾아보기 쉽도록 용도별로 나눴다.

| 분류 | 스킬 | 언제 쓰나 |
|---|---|---|
| 진입·안내 | `poteto-mode` | 사소하지 않은 작업의 기본 진입점 |
| 진입·안내 | `poteto-help` | 처음이거나 어떤 스킬·플레이북·원칙이 맞는지 모를 때. 입력할 프롬프트를 만들어 준다. |
| 진입·안내 | `in-your-own-words` | 시작 전에 내 목표와 문제를 에이전트 말로 다시 듣고 싶을 때 |
| 진입·안내 | `sharpen` | 보내기 전 프롬프트를 "더 크게가 아니라 더 깊게" 다시 쓰고 싶을 때. 작업 자체는 하지 않는다. |
| 이해 | `how` | 서브시스템이 어떻게 동작하는지, 코드가 어디에 있어야 하는지 |
| 이해 | `why` | 왜 이렇게 만들었는지. 코드가 아니라 커밋·PR 기록으로 답하고, 추론은 따로 표시한다. |
| 이해 | `teach` | 요약이 아니라 진짜 이해가 필요할 때. `how` + `why` 를 엮어 그림과 함께 설명 |
| 이해 | `recall` | 작업 재개 시 내 채팅 기록과 공유 기록에서 최근 맥락을 복원 |
| 소통 | `ketchup` | 내 마지막 메시지 이후 바뀐 것과 내가 결정할 항목을 쉬운 말로 정리 |
| 소통 | `ketchup-video` | `ketchup` 내용을 자막 슬라이드 영상으로 |
| 소통 | `bro` | 방금 답변을 전문 용어 없이 다시 말해 달라고 할 때 |
| 설계 | `architect` | 함수 경계를 넘는 코드를 쓰기 전에 호출 방식·타입·모듈 구조를 먼저 정할 때 |
| 설계 | `figure-it-out` | 맞는 플레이북이 없을 때. 큰 마이그레이션 등을 위한 감사 가능한 플레이북을 설계 |
| 설계 | `statechart` | TLA+ 모델을 검사하고 Mermaid 상태 다이어그램으로 그려, 규칙을 깨는 상태까지의 최단 경로를 보여 줄 때 |
| 병렬 | `arena` | 같은 작업을 N번 병렬로 시도하고 가장 좋은 부분을 골라 합칠 때 |
| 병렬 | `swarm` | 서로 다른 조각을 N명이 나눠 처리하고 보고서 하나로 받을 때 |
| 검증·리뷰 | `blast-radius` | 작아 보이는 변경이 무엇을 깨뜨릴 수 있는지, 안전한 근거를 실제 코드 실행으로 증명할 때 |
| 검증·리뷰 | `interrogate` | 여러 모델에게 diff 를 깨뜨려 보게 하고, 실행으로 증명된 지적만 남길 때 |
| 검증·리뷰 | `tdd` | 값싼 로컬 테스트 경로가 있는 버그 수정. 실패 테스트부터 쓴다. 테스트 경로가 비싸면 쓰지 않는다. |
| 검증·리뷰 | `benchmark-checklist` | 벤치마크나 속도 개선·회귀 수치를 보고·판단하기 전에 그 숫자를 검증할 때 |
| 검증·리뷰 | `no-comments` | 리뷰 전 주석 정리. Comment Sicko 를 띄우고 받아들인 지적을 고친다. |
| 검증·리뷰 | `create-verification-skill` | 앱 동작을 스크립트로 증명할 방법이 없을 때, 사용자처럼 앱을 조작하는 프로젝트 전용 검증 스킬 생성 |
| 검증·리뷰 | `maintain-verification-skill` | 검증 스킬의 기능 목록이 실제 앱과 어긋났을 때 주기적으로 바로잡기 |
| 검증·리뷰 | `create-formal-verification-skill` | 테스트로 닿지 않는 경우(레이스, 재시도, 상태 기계, 파서)를 형식 검증하는 스킬 생성 |
| 검증·리뷰 | `maintain-formal-verification-skill` | 형식 검증 대상 코드가 바뀐 뒤 모델·속성을 코드에 맞추고 다시 실행 |
| 기록 | `show-me-your-work` | 길거나 사람 없이 도는 작업에서 결정 기록을 TSV 로 남길 때 |
| 개선 | `correct` | 같은 실수를 반복 지적할 때. 아키텍처 → 타입·lint·CI → 테스트 → 문서 순으로 높은 수준에서 막는다. |
| 개선 | `reflect` | 긴 작업이 끝난 뒤 배운 점을 기존 스킬 수정으로 남길 때 |
| 개선 | `automate-me` | 내 작업 방식으로 나만의 `<이름>-mode` 스킬 초안을 만들 때 |
| 글쓰기 | `unslop` | 글에서 AI 티를 걷어낼 때 |
| 글쓰기 | `technical-writing` | 문서, RFC, README, PR 설명, 커밋 메시지 작성·리뷰 기준 |
| 언어 | `typescript-best-practices` | `.ts`/`.tsx` 를 읽거나 고칠 때 |
| 기타 | `make-bot-ui` | 웹훅으로 Grok Bot 을 깨우는 버튼·대시보드 UI 를 만들 때 |
| 설정 | `setup-pstack` | 역할별 모델과 추론 수준을 정할 때 |

## 6. 원칙 24개

원칙도 각각 `principle-<이름>` 스킬이다. `poteto-mode` 는 원칙을 적용하려면 해당 `SKILL.md` 를 끝까지 읽으라고 한다.

### core (10)

| 원칙 | 언제 적용하나 |
|---|---|
| `laziness-protocol` | 리팩토링, diff 크기 판단, 추상화를 더하고 싶을 때. 삭제 쪽으로, 가장 작은 변경으로 |
| `foundational-thinking` | 로직을 쓰기 전. 핵심 타입과 자료구조부터 바로 잡는다. |
| `redesign-from-first-principles` | 기존 설계에 새 요구사항을 넣을 때. 처음부터 그 요구사항이 있었던 것처럼 다시 설계 |
| `attack-the-premise` | 같은 전제의 수정이 두 번 이상 실패했을 때. 전제를 적고 그것부터 직접 검증 |
| `subtract-before-you-add` | 추가·리팩토링 전에 죽은 코드, 중복 검증부터 덜어낸다. |
| `minimize-reader-load` | 따라가기 어려운 코드. 호출 한 곳뿐인 래퍼를 접고 가변 범위를 줄인다. |
| `outcome-oriented-execution` | 단계가 나뉜 재작성·마이그레이션. 중간 호환 코드를 남기지 말고 목표 구조로 수렴 |
| `experience-first` | 제품·UX·범위 결정. 구현 편의보다 사용자 경험, 거친 기능 여럿보다 다듬은 기능 몇 개 |
| `exhaust-the-design-space` | 선례 없는 UI·아키텍처 결정. 2~3개 시제품을 만들어 비교 |
| `build-the-lever` | 사소하지 않은 모든 일. 손으로 하지 말고 그 일을 하거나 증명하는 도구(codemod, 스크립트)를 만든다. |

### architecture (6)

| 원칙 | 언제 적용하나 |
|---|---|
| `model-the-domain` | 상태가 있는 로직, 분기가 많은 코드. 흩어진 조건문 대신 상태 기계·타입 모델·테이블로 |
| `boundary-discipline` | 검증·에러 처리·어댑터. 방어 코드는 시스템 경계에 모으고 내부 타입은 믿는다. |
| `type-system-discipline` | 타입·시그니처 설계. 잘못된 상태를 표현할 수 없게 만든다. |
| `make-operations-idempotent` | 크래시·재시작·재시도 속에서 도는 명령과 루프. 같은 최종 상태로 수렴하게 |
| `migrate-callers-then-delete-legacy-apis` | 새 내부 API 도입. 호출부 이전과 옛 API 삭제를 한 번에 |
| `separate-before-serializing-shared-state` | 여러 주체가 같은 파일·브랜치·키에 쓸 때. 공유부터 없앤다. |

### verification (5)

| 원칙 | 언제 적용하나 |
|---|---|
| `prove-it-works` | 완료 선언 전. 실제 결과물로 검증한다. "컴파일된다"는 근거가 아니다. |
| `fix-root-causes` | 디버깅. 증상을 처음 잘못된 지점까지 거슬러 올라가 거기서 고친다. |
| `sequence-verifiable-units` | 여러 단계 작업, 커밋·PR 쌓기. 각 단위가 검증 가능한 상태로 끝나게 나눈다. |
| `test-behavior-not-implementation` | 테스트 작성·수정. 사용자가 관찰할 수 있는 동작을 검증한다. |
| `explain-the-number` | 측정 전, 그리고 측정한 숫자를 믿거나 보고하기 전. 무엇이 그 숫자를 제한하는지 찾는다. |

### delegation (2) · meta (1)

| 그룹 | 원칙 | 언제 적용하나 |
|---|---|---|
| delegation | `guard-the-context-window` | 컨텍스트가 찰 때. 대량 작업은 서브에이전트로, 메인에는 요약만 |
| delegation | `never-block-on-the-human` | 되돌릴 수 있는 일에 "해도 될까요?"를 묻고 싶을 때. 진행하고 결과를 보여 준다. |
| meta | `encode-lessons-in-structure` | 같은 지시를 두 번째 쓰고 있을 때. 글 대신 lint·검사·스크립트로 남긴다. |

## 7. 기본 모델 구성

역할마다 기본 모델이 정해져 있다. `/setup-pstack` 으로 바꿀 수 있다.

| 역할 | 기본 모델 (Cursor 표기) | Claude Code 대응 |
|---|---|---|
| 코드·문장·판단 | `claude-opus-5-5-xhigh-fast` | `opus xhigh` |
| 단순 기계적 수정, 검증 (`swarm workers`) | `claude-opus-5-5-high-fast` | `opus high` |
| 리뷰·설계 패널 | Fable 5.2 high / Opus 5.5 high / Gemini 4 Carbon high 각 1명 | `fable high` / `opus high` / 세션 모델(`inherit-parent`) |

추가 규칙은 다음과 같다.

- **Fable 5.2 는 예외적으로만** 쓴다. 패널 밖에서는 사용자가 이 작업에 지명했거나, 같은 지시로 Opus 5.5 xhigh 가 두 번 실패했을 때만 쓴다. 어렵다는 이유만으로는 안 된다.
- **어떤 역할도 xhigh 를 넘지 않는다.**
- 검증은 항상 `swarm workers` 모델로 돈다.
- Claude Code 에서는 Gemini 처럼 Claude 가 아닌 모델은 `model` 을 생략해 세션 모델로 실행한다.

## 8. 자율성 규칙과 계획에 대한 입장

### 묻지 않고 진행 vs 멈추는 작업

| 구분 | 작업 |
|---|---|
| **묻지 않고 진행** | 되돌릴 수 있는 작업 전부, MCP 도구 사용, 외부 작업(팀 채팅, 티켓 갱신, eval 시작) |
| **항상 멈춤** | 되돌릴 수 없는 쓰기 — 공유 브랜치 force-push, 배포, 데이터 삭제, 고객 메시지 |
| **세션 중 덮어쓰기** | "멈추지 마", "자러 간다", "끝날 때까지 돌려" 같은 말이 있으면 계속 진행 |

여기에 **"No is an acceptable answer"** 규칙이 붙는다. 무언가를 할지 물었거나 범위 추가를 제안받았을 때, 가치가 없으면 거절하거나 반대하라고 적혀 있다. 동의를 기본값으로 두지 않는다.

### 계획에 대한 입장

README 에 "why are there no planning skills?" 라는 절이 따로 있다.

> i don't believe in planning. the best spec is code.

계획 기능은 `poteto-mode` 의 Multi-phase plan 플레이북에 있지만 **기본값이 아니다.** 그 플레이북조차 한두 파일짜리 변경이면 계획을 건너뛰라고 하고, 열린 질문은 계획서를 쓰기 전에 시제품으로 결론 내라고 한다.

## 9. Claude Code 에서 쓰는 법

### 설치와 갱신

```bash
/plugin marketplace add cursor/plugins
/plugin install pstack@cursor-plugins
```

| 항목 | 내용 |
|---|---|
| 명령 접두사 | 플러그인 명령에 `/pstack:` 이 붙는다. `/pstack:poteto-mode`, `/pstack:why`, `/pstack:blast-radius` |
| 서브에이전트 이름 | `pstack:poteto-agent`, `pstack:comment-sicko` |
| 자동 업데이트 | 서드파티 마켓플레이스라 **기본으로 꺼져 있다.** 설치한 버전에 머문다. |
| 자동 업데이트 켜기 | `/plugin` → **marketplaces** 탭 → `cursor-plugins` 선택 → **enable auto-update** |
| 수동 갱신 | `claude plugin update pstack@cursor-plugins` |

### setup-pstack

`/pstack:setup-pstack` 은 사용 가능한 모델을 감지하고 역할별 모델을 **`~/.claude/rules/pstack-models.md`** 에 쓴다. Claude Code 는 `~/.claude/rules/` 의 `.md` 파일을 새 세션마다 전부 불러오므로, 이 파일은 **모든 프로젝트의 모든 새 세션에 로드된다.** 실행하면 파일 전체를 덮어쓰고, 역할 설정을 `CLAUDE.md` 에 넣지는 않는다.

### Cursor 이름 대응

pstack 스킬 본문은 Cursor 이름으로 쓰여 있고, `poteto-mode` 는 Claude Code 에서 어댑터 문서를 세션마다 한 번 읽고 시작한다.

| Cursor | Claude Code |
|---|---|
| `Task` | `Agent` |
| `AskQuestion` | `AskUserQuestion` |
| 다른 pstack 스킬 실행 | Skill 도구가 대부분 거부하므로 해당 `SKILL.md` 를 Read 해서 따른다. |
| `readonly: true` | `subagent_type: "Explore"` (Bash 가 남아 있어 "파일 수정 금지"를 지시에 함께 적는다) |
| `environment: "cloud"` | `isolation: "worktree"` |
| `~/.cursor/rules/pstack-models.mdc` | `~/.claude/rules/pstack-models.md` |
| `.cursor/rules/*.mdc` (항상 적용 규칙) | `CLAUDE.md` (Claude Code 는 `.mdc` 를 읽지 않는다) |

### Cursor 전용 기능은 건너뛴다

Claude Code 에는 Cursor Automations, Bugbot, Cursor cloud agent, 내장 PR 도구가 없다. 단계가 이런 기능을 필요로 하면 다음 한 줄만 남기고 다음 단계로 넘어간다. 멈추거나 묻거나 재시도하지 않는다.

```
Skipped in Claude Code: <step>. <reason>.
```

| Cursor 기능 | Claude Code 에서 |
|---|---|
| Automations (routine, 웹훅 등) | 건너뜀 |
| Bugbot | PR 에 달린 Bugbot 코멘트는 일반 리뷰 스레드로 처리. Bugbot 을 시작·대기·설정하는 단계는 건너뜀 |
| Cursor cloud agent | 실행 환경은 worktree 로 대응. cloud agent 자체가 필요한 단계는 건너뜀 |
| 내장 PR 도구 | `gh pr create` 등 단계에 적힌 명령을 쓴다. 건너뜀 아님 |

참고로 README 에 나오는 `poteto-mode` 를 Custom Mode 로 계속 켜 두는 방법(option+enter)과 `/loop` 조합은 Cursor 기준 설명이다.

## 10. 내 개발 흐름에 적용한다면

결론부터 말하면 **통째로 도입하지 않고, 조회·분석 스킬과 일부 원칙만 골라 쓰기로 했다.** `/pstack:poteto-mode` 를 그대로 쓰면 pstack 의 자율성 규칙이 내 단계별 승인 흐름과 부딪힌다.

### 겹치는 점

| pstack | 내 흐름 | 비고 |
|---|---|---|
| `tdd`, `sequence-verifiable-units` | `implement-step` (UC 테스트 먼저, 단계별 진행) | 방향이 같다. |
| `interrogate`, `blast-radius` | `dev-review` | 다중 모델·실행 증명 방식은 참고할 만하다. |
| `prove-it-works` | ralph-loop 검증 | 같다. |

### 충돌하는 점

| pstack | 내 흐름 | 영향 |
|---|---|---|
| `never-block-on-the-human`, 자율성 규칙 | 단계별 승인 후 커밋, 커밋 메시지 승인 | `poteto-mode` 가 확인 없이 진행하려 한다. |
| "the best spec is code", 계획 비기본 | `plan-implementation` 중심 | 계획이 기본 단계가 아니라 따로 요청해야 한다. |
| `setup-pstack` 이 전역 rules 파일 작성 | 전역 `CLAUDE.md` 규칙 | 모든 세션에 로드되어 기존 규칙과 겹칠 수 있다. |

### 바로 써 볼 만한 것

| 대상 | 용도 |
|---|---|
| `/pstack:why`, `/pstack:how`, `/pstack:teach` | 읽기 위주라 기존 흐름을 바꾸지 않는다. |
| `/pstack:blast-radius` | MR 전 영향 범위 점검에 `dev-review` 보조로 |
| `attack-the-premise`, `encode-lessons-in-structure` | 스킬을 개선할 때 참고할 원칙 |

`/pstack:setup-pstack` 은 아직 실행하지 않았다. 필요해지면 생성될 파일 내용을 먼저 확인하고 적용할 생각이다.

## 11. 요약

- pstack 은 "go deep first, write less" 를 표어로 한 Cursor 플러그인이다. 스킬 34개, 원칙 24개, 플레이북 22개, 서브에이전트 2개로 이뤄지고 훅은 없다.
- 진입점 `poteto-mode` 는 요청을 플레이북에 대응시키고, 단계마다 스킬을 부르고, 결정을 바꾼 원칙을 답변에 밝힌다.
- 되돌릴 수 있는 일은 묻지 않고 진행하고, force-push·배포·데이터 삭제·고객 메시지만 멈춘다. 계획은 기본값이 아니다.
- Claude Code 에서는 `/pstack:` 접두사로 쓰고, `setup-pstack` 은 모든 세션에 로드되는 `~/.claude/rules/pstack-models.md` 를 쓴다. 자동 업데이트는 꺼져 있다.
- 내 흐름에는 `why`·`how`·`teach`·`blast-radius` 같은 읽기 위주 스킬과 일부 원칙만 골라 쓴다.

## Ref.

- [cursor/plugins — pstack](https://github.com/cursor/plugins/tree/main/pstack) (README, `docs/harness/claude-code.md`, `skills/*/SKILL.md`, v0.15.43)
