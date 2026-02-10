---
layout: post
title : Redis Stream 멀티플렉싱 단일 커넥션 테스트 결과
date  : 2026-02-10
image : redis.jpg
tags  : redis redis-Stream
---

# Redis Stream 멀티플렉싱 단일 커넥션 테스트 결과

> 테스트 일자: 2026-02-10
> 테스트 대상: Valkey 7.2.6 on Amazon ElastiCache (ap-northeast-2)
> 테스트 환경: 로컬 VPN 경유 개발 Redis (`127.0.0.1:40198`)
> 클라이언트: Lettuce (Spring Boot 3.4.3 / Spring Data Redis)
> 커넥션 설정: `RedisStaticMasterReplicaConfiguration` + `ReadFrom.REPLICA_PREFERRED` + RESP2

---

## 1. 진단 테스트 결과 (SingleSubscriptionTest)

### 1-1. Stream 명령 호환성

| 명령 | 결과 | 비고 |
|------|------|------|
| `XADD` | ✅ 성공 | |
| `XLEN` | ✅ 성공 | |
| `XGROUP CREATE` | ✅ 성공 | |
| `XREADGROUP` (non-block) | ✅ 성공 | |
| `XREADGROUP BLOCK` | ❌ 실패 | `ERR [ENGINE] Invalid command` |
| `SET` / `GET` / `DEL` | ✅ 성공 | |

**핵심 발견: ElastiCache Valkey 엔진이 Lettuce `StaticMasterReplicaConnection` 경유 `XREADGROUP BLOCK`을 거부한다.**

- redis-cli로 직접 `XREADGROUP GROUP grp c1 COUNT 1 BLOCK 100 STREAMS key >` 실행 시에는 **정상 동작**
- Lettuce `StaticMasterReplicaConnection`을 통해 동일 명령 전송 시 `ERR [ENGINE] Invalid command` 반환
- `BLOCK` 파라미터가 없는 `XREADGROUP`은 동일 커넥션에서 **정상 동작**

> **참고: Lettuce 자체가 BLOCK을 거부하는 것은 아니다.**
>
> - Lettuce 공식 문서에 `XREADGROUP BLOCK`을 금지한다는 내용은 없다
>   ([Advanced Usage](https://redis.github.io/lettuce/advanced-usage/))
> - Spring Data Redis `LettuceConnectionFactory`는 blocking 명령에 대해 자동으로 전용(dedicated) 커넥션을 할당하도록 설계되어 있다
>   ([LettuceConnectionFactory API](https://docs.spring.io/spring-data/redis/docs/current/api/org/springframework/data/redis/connection/lettuce/LettuceConnectionFactory.html))
> - ElastiCache Valkey에서 `XREADGROUP`은 제한 명령 목록에 포함되지 않는 공식 지원 명령이다
>   ([ElastiCache Supported Commands](https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/SupportedCommands.html))
>
> 에러의 출처는 **ElastiCache 엔진 측**(`ERR [ENGINE]` 접두사)이며,
> Lettuce `StaticMasterReplicaConnection`이 `XREADGROUP BLOCK`을 위한 dedicated 커넥션을 생성하는 과정에서
> ElastiCache 엔진이 인식하지 못하는 형식으로 명령이 전달되는 것으로 추정된다.
> (`CLIENT SETINFO` 핸드셰이크 역시 동일하게 `ERR [ENGINE] Invalid command`로 실패)

### 1-2. Non-blocking polling 메시지 수신

`StreamMessageListenerContainer`의 `pollTimeout(Duration.ZERO)` 설정으로 `BLOCK` 파라미터 없이 polling:

| 항목 | 결과 |
|------|------|
| 발행 메시지 | 10건 |
| 수신 메시지 | **10건 (100%)** |
| 메시지 유실 | 없음 |

### 1-3. Non-blocking polling 중 다른 Redis 명령 응답시간

| 항목 | 값 |
|------|-----|
| 평균 | 104ms |
| 최대 | 180ms |
| 최소 | 77ms |

---

## 2. Subscription 수 증가 테스트 결과 (MultipleSubscriptionTest)

`Container 1개 + Subscription N개`, non-blocking polling, 메시지 100건 발행

| Subscription 수 | 수신 | SET+GET 평균 | SET+GET 최대 | SET+GET 최소 |
|:---:|:---:|:---:|:---:|:---:|
| 1 | 100/100 | 107ms | 161ms | 84ms |
| 2 | 100/100 | 148ms | 229ms | 106ms |
| 4 | 100/100 | 245ms | 361ms | 199ms |
| 8 | 100/100 | 428ms | 479ms | 406ms |
| 16 | 100/100 | 761ms | 827ms | 698ms |
| 32 | 99/100 | 704ms | 828ms | 571ms |

**관찰:**

- Subscription 수가 증가할수록 다른 Redis 명령의 응답시간이 **선형적으로 증가**
- 1개(107ms) → 32개(704ms): **약 6.6배 증가**
- 32개에서 메시지 1건 유실 발생 (99/100)
- non-blocking polling은 XREADGROUP을 tight loop으로 실행하므로, Subscription 수가 커넥션 경합에 직접적 영향

---

## 3. 공존 테스트 결과 (CoexistenceTest)

32개 Subscription non-blocking polling + 메시지 100건 발행 중 기존 `redisTemplate` 성능 측정:

> 원래 계획은 1000건 발행이었으나, 32개 non-blocking Subscription의 tight loop polling과 단일
> 멀티플렉싱 커넥션의 과부하로 XADD 자체가 `ERR [ENGINE] Invalid command`로 실패하여 100건으로 축소하였다.

| 항목 | Baseline (Stream 없음) | With Stream (32 subs) | 배율 |
|------|:---:|:---:|:---:|
| SET+GET+DEL 평균 | 114ms | 2,137ms | **18.6x** |
| SET+GET+DEL 최대 | 420ms | 2,515ms | 6.0x |

**판정: ⚠️ 유의미한 성능 저하 (3x 기준 초과)**

---

## 4. 결론

### XREADGROUP BLOCK 사용 불가

ElastiCache Valkey 엔진이 Lettuce `StaticMasterReplicaConnection` 경유 `XREADGROUP BLOCK`을 거부한다.
Lettuce 자체가 BLOCK을 금지하는 것은 아니며, ElastiCache 엔진 측에서 `ERR [ENGINE] Invalid command`를 반환한다.
(redis-cli 직접 실행 시에는 정상 동작하므로, `StaticMasterReplicaConnection`의 커넥션 라우팅/핸드셰이크 과정이 원인으로 추정)

`StreamMessageListenerContainer`의 기본 동작(`pollTimeout > 0`)은 내부적으로 `XREADGROUP BLOCK`을 사용하므로, **기존 `redisConnectFactory`로는 blocking polling 방식의 Stream 소비가 불가능하다.**

### Non-blocking polling 대안의 한계

`pollTimeout(Duration.ZERO)`로 non-blocking polling은 가능하지만:

1. **tight loop 문제**: BLOCK 없이 polling하면 CPU와 네트워크를 과도하게 소비
2. **커넥션 경합**: Subscription 수에 비례하여 다른 Redis 명령 응답시간이 선형 증가 (1개=107ms → 32개=704ms)
3. **기존 기능 영향**: 32개 Subscription 시 기존 Redis 명령 응답시간 **18.6배 저하** (114ms → 2,137ms)
4. **메시지 유실 가능성**: 고부하 시 메시지 유실 발생 (32 sub에서 99/100)

### 테스트 시나리오별 판정

| 시나리오 | 결과 | 판정 |
|---------|------|------|
| Case A: 기존 커넥션으로 문제 없음 | - | ❌ 해당 없음 |
| Case B: 소수 Subscription에서는 OK | - | ❌ 해당 없음 |
| Case C: BLOCK이 커넥션을 점유하여 블로킹 | - | ❌ 해당 없음 (BLOCK 자체가 동작하지 않음) |
| Case D: XREADGROUP BLOCK 자체가 동작하지 않음 | BLOCK 거부 + non-blocking 우회 시 심각한 성능 저하 | ✅ **이 케이스에 해당** |

### 권장 사항

1. **별도 ConnectionFactory 필수**: `StaticMasterReplicaConfiguration` 대신 Stream 전용 `StandaloneConfiguration` 또는 전용 풀링 커넥션 팩토리 필요
2. **BLOCK 지원 검증**: 별도 ConnectionFactory에서 `XREADGROUP BLOCK`이 동작하는지 우선 검증
3. **초기 설계 유지**: 기존 설계대로 Stream 전용 ConnectionFactory + Pool 구성 진행 (이후 Phase 1에서 non-blocking + 공유 ConnectionFactory로 결정)
4. **Subscription 수 제한 고려**: 단일 커넥션당 Subscription 수를 제한하거나, Subscription마다 별도 커넥션 할당 검토

---

## 5. 테스트 코드 위치

```
src/test/kotlin/.../campaign/infrastructure/redis/stream/
├── StreamMultiplexingTestConfig.kt     ← @TestConfiguration
├── SingleSubscriptionTest.kt           ← 진단 + non-blocking polling 기본 동작
├── MultipleSubscriptionTest.kt         ← Subscription 수별 성능 (1, 2, 4, 8, 16, 32)
└── CoexistenceTest.kt                  ← 기존 Redis 기능과의 공존 테스트
```

상세 코드는 [멀티플렉싱 테스트 계획](./2026-02-10-Redis_Stream_Multiplexing_Test.md) 참고.

## 6. 재현 방법

```bash
# 로컬 VPN으로 개발 Redis 연결 후
./gradlew test --tests "*SingleSubscriptionTest"
./gradlew test --tests "*MultipleSubscriptionTest"
./gradlew test --tests "*CoexistenceTest"
```

---

## 7. 참고 자료

### Lettuce 공식 문서

- [Lettuce Advanced Usage](https://redis.github.io/lettuce/advanced-usage/) — 공유 커넥션에서 blocking 명령 사용 시 성능 경고 (금지가 아닌 경고)
- [Lettuce FAQ](https://redis.github.io/lettuce/faq/) — `blpop(Duration.ZERO, …)` 타임아웃 이슈 및 `TimeoutOptions` 설정 가이드
- [Lettuce HA & Sharding Reference](https://redis.github.io/lettuce/ha-sharding/) — Master/Replica 토폴로지 설정 및 ReadFrom 라우팅
- [Lettuce Master-Replica Wiki](https://github.com/redis/lettuce/wiki/Master-Replica) — `StaticMasterReplicaTopologyProvider` 동작 방식

### Spring Data Redis 공식 문서

- [LettuceConnectionFactory API](https://docs.spring.io/spring-data/redis/docs/current/api/org/springframework/data/redis/connection/lettuce/LettuceConnectionFactory.html) — `shareNativeConnection=true` 시 blocking 명령에 대해 자동으로 dedicated 커넥션 할당

### AWS ElastiCache 공식 문서

- [ElastiCache Supported Commands](https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/SupportedCommands.html) — `XREADGROUP`은 제한 명령이 아닌 공식 지원 명령
- [ElastiCache Lettuce Client Configuration](https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/BestPractices.Clients-lettuce.html) — Cluster Mode Disabled 환경에서 `StaticMasterReplicaTopologyProvider` 사용 권장
