---
layout: post
title : SDKMAN 가이드
date  : 2026-01-14
image : sdk-man-logo.svg
tags  : sdkman java jdk
---

## SDKMAN 설치 및 사용 가이드

### 설치

```bash
curl -s "https://get.sdkman.io" | bash
```

설치 후 새 터미널을 열거나 아래 명령어 실행:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

### 핵심 명령어

#### JDK 목록 조회

```bash
sdk list java                    # 설치 가능한 모든 JDK 목록
sdk list java | grep tem         # Temurin만 필터링
sdk list java | grep installed   # 설치된 버전만 보기
```

#### JDK 설치

```bash
sdk install java 21.0.9-tem      # Temurin JDK 21 설치
sdk install java 17.0.17-tem     # Temurin JDK 17 설치
sdk install java 21.0.9-amzn     # Amazon Corretto 21 설치
```

#### 버전 전환

```bash
sdk use java 21.0.9-tem          # 현재 세션에서만 전환
sdk default java 21.0.9-tem      # 기본값으로 설정 (영구 적용)
```

#### 현재 버전 확인

```bash
sdk current java                 # 현재 사용 중인 JDK
java -version                    # Java 버전 상세 정보
```

#### 버전 삭제

```bash
sdk uninstall java 17.0.17-tem   # 특정 버전 삭제
```

---

### 주요 JDK 배포판

| 배포판 | 식별자 | 특징 |
|--------|--------|------|
| Temurin | tem | Eclipse 재단, 가장 널리 사용됨 |
| Corretto | amzn | Amazon 배포, AWS 환경에 최적 |
| Zulu | zulu | Azul Systems, 안정적 |
| GraalVM CE | graalce | 고성능, 네이티브 이미지 지원 |
| Liberica | librca | BellSoft, JavaFX 포함 버전 제공 |

---

### 환경 변수

SDKMAN이 자동으로 설정하는 환경 변수:
- `JAVA_HOME`: `~/.sdkman/candidates/java/current`
- `PATH`: JAVA_HOME/bin이 자동 추가됨

---

### SDKMAN 자체 관리

```bash
sdk version                      # SDKMAN 버전 확인
sdk update                       # 후보 목록 업데이트
sdk selfupdate                   # SDKMAN 자체 업데이트
```

---

### 프로젝트별 버전 설정

프로젝트 루트에 `.sdkmanrc` 파일 생성:

```
java=17.0.17-tem
```

해당 디렉토리 진입 시 자동 전환 활성화:

```bash
sdk env install                  # .sdkmanrc에 지정된 버전 설치
sdk env                          # .sdkmanrc 버전으로 전환
```

자동 전환 설정 (`~/.sdkman/etc/config`):

```
sdkman_auto_env=true
```

---

#### 출처

- [sdkman.io](https://sdkman.io)