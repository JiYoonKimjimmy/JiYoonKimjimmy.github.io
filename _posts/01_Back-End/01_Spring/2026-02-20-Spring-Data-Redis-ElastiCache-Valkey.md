---
layout: post
title : Spring Data Redis + ElastiCache Valkey 트러블 슈팅
date  : 2026-02-20
image : spring-data-redis.png
tags  : redis springdataredis elasticache valkey redis-stream
---

# ElastiCache Valkey XREADGROUP 명령어 BLOCK 옵션 사용 불가 이슈

## 1. 요약

ElastiCache Valkey 환경에서 Redis Stream의 `XREADGROUP ... BLOCK` 명령이 **새로 생성된 커넥션에서 거부**된다.
Spring Data Redis는 blocking 명령 실행 시 내부적으로 **dedicated(전용) 커넥션을 새로 생성**하기 때문에,
`StreamOperations.read()` + `StreamReadOptions.block()` 조합은 항상 실패한다.

---

## 2. 문제 상세

### 2.1 현상

`StreamOperations.read()`에 `StreamReadOptions.block(Duration)` 옵션을 사용하면 다음과 같은 에러가 발생한다:

```
org.springframework.data.redis.RedisSystemException:
  Error in execution; nested exception is io.lettuce.core.RedisCommandExecutionException:
  ERR Blocking is not allowed on a non-blocking connection
```

### 2.2 근본 원인: Spring Data Redis의 커넥션 라우팅 구조

`LettuceConnection` 내부에서 명령어 실행 시 커넥션을 선택하는 로직이 분리되어 있다:

```java
// LettuceConnection.java
protected StatefulConnection<byte[], byte[]> getAsyncConnection() {
    if (sharedConnection != null) {
        return sharedConnection;       // ← shared pool 커넥션 (기존 커넥션 재사용)
    }
    return getAsyncDedicatedConnection(); // ← dedicated 커넥션 (새로 생성)
}
```

| 호출 방식 | 사용 커넥션 | BLOCK 결과 |
|:---|:---|:---|
| `streamOps.read(Consumer, StreamReadOptions.empty(), ...)` | shared pool 커넥션 | N/A (non-blocking) |
| `streamOps.read(Consumer, StreamReadOptions.block(), ...)` | **dedicated 커넥션 (신규 생성)** | **실패** |
| `connection.execute("XREADGROUP", ...)` | shared pool 커넥션 | **성공** |

- **non-blocking 명령**: shared pool의 기존 커넥션을 통해 실행 → 정상 동작
- **blocking 명령**: Lettuce가 다른 명령의 실행을 차단하지 않도록 **전용 커넥션을 새로 생성** → ElastiCache Valkey가 거부

### 2.3 ElastiCache Valkey의 제약

일반적인 Redis(OSS)에서는 dedicated 커넥션에서 BLOCK 명령이 정상 동작한다.
그러나 ElastiCache Valkey는 내부 프록시 계층이 존재하며,
**새로 생성된 커넥션에서 즉시 BLOCK 명령을 수행하는 것을 허용하지 않는다.**

이 제약은 ElastiCache의 관리형 프록시(managed proxy) 아키텍처에서 기인하며,
프록시가 커넥션 상태를 아직 완전히 인식하지 못한 시점에서 blocking 모드 진입을 차단하는 것으로 추정된다.

---

## 3. 검증한 우회 시도 및 결과

다음의 우회 방법을 모두 시도하였으나, `StreamOperations.read()` + `block()` 조합의 실패를 해결하지 못했다.

### 3.1 커넥션 warm-up (실패)

dedicated 커넥션 생성 후, BLOCK 요청 전에 다른 명령으로 warm-up 시도:

```kotlin
// dedicated 커넥션에서 SET/GET warm-up 후 BLOCK 시도
connection.set("warmup-key".toByteArray(), "1".toByteArray())
connection.get("warmup-key".toByteArray())
// → 이후 XREADGROUP BLOCK 실행 시 여전히 실패
```

```kotlin
// dedicated 커넥션에서 non-blocking XREADGROUP warm-up 후 BLOCK 시도
connection.streamCommands().xReadGroup(consumer, StreamReadOptions.empty(), offset)
// → 이후 XREADGROUP BLOCK 실행 시 여전히 실패
```

**결론**: dedicated 커넥션 자체가 ElastiCache 프록시에서 blocking을 허용하지 않으며,
warm-up 여부와 무관하게 실패한다.

### 3.2 Standalone 구성으로 전환 (실패)

`RedisStaticMasterReplicaConfiguration` 대신 `StandaloneConfiguration`으로 전환하여 테스트:

```kotlin
val config = RedisStandaloneConfiguration(host, port)
val factory = LettuceConnectionFactory(config, clientConfig)
```

**결론**: MasterReplica 라우팅 계층과 무관하게 동일한 에러 발생.

### 3.3 CLIENT SETINFO 비활성화 (실패)

Lettuce 6.4+에서 자동으로 전송하는 `CLIENT SETINFO` 핸드셰이크가 원인인지 확인:

```kotlin
val redisUri = RedisURI.builder()
    .withHost(host).withPort(port)
    .withLibraryName("")   // CLIENT SETINFO 비활성화
    .withLibraryVersion("")
    .build()
```

**결론**: `CLIENT SETINFO` 비활성화 후에도 동일한 에러 발생.

### 3.4 "첫 번째는 non-blocking, 이후 BLOCK" 전략 (불가)

이론적으로는 "첫 요청은 non-blocking으로 커넥션을 확립한 뒤, 이후 BLOCK 사용"을 고려할 수 있으나,
실제로는 다음 두 가지 이유로 적용 불가:

1. **Spring Data Redis가 blocking/non-blocking을 서로 다른 커넥션으로 라우팅**
   - non-blocking 호출 → shared pool 커넥션
   - blocking 호출 → dedicated 커넥션 (신규 생성)
   - `StreamOperations` API로는 커넥션 재사용을 제어할 수 없음

2. **dedicated 커넥션에서의 warm-up이 이미 실패 확인됨** (3.1절 참조)
   - 동일 dedicated 커넥션에서 non-blocking 명령 실행 후 BLOCK을 보내도 여전히 거부됨
   - ElastiCache 프록시의 제약이 단순한 "새 커넥션" 문제가 아님

---

## 4. 채택한 해결 방안: Non-blocking Polling

### 4.1 아키텍처

```
┌─────────────────────────────────────────────┐
│           Polling Task (Runnable)            │
│                                             │
│   while (!stopFlag) {                       │
│       messages = XREADGROUP (non-blocking)  │
│       if (messages.isNotEmpty())            │
│           → process & update lastMessageTime│
│       else                                  │
│           → check idle timeout              │
│           → Thread.sleep(pollIntervalMs)    │
│   }                                         │
└─────────────────────────────────────────────┘
```

### 4.2 주요 설정값

| 설정 | 기본값 | 설명 |
|:---|:---|:---|
| `redis.stream.poll-interval-ms` | `100` | 메시지 없을 때 다음 조회까지 대기 시간 (ms) |
| `redis.stream.idle-timeout-seconds` | `30` | 마지막 메시지 수신 후 consumer 자동 종료 시간 (s) |
| `redis.stream.batch-size` | `10` | 한 번의 XREADGROUP으로 읽는 최대 메시지 수 |

### 4.3 핵심 구현

```kotlin
// CampaignPromotionStreamPollingTaskFactory.kt
val messages = streamRedisTemplate.xReadGroup(
    group = consumerGroup,
    consumer = consumerName,
    streamKey = streamKey,
    count = properties.batchSize.toLong(),
)

if (!messages.isNullOrEmpty()) {
    messages.forEach { consumer.processMessage(it) }
    lastMessageTime.set(System.currentTimeMillis())
} else {
    val idleMs = System.currentTimeMillis() - lastMessageTime.get()
    if (idleMs >= properties.idleTimeoutSeconds * 1000) {
        onIdleStop()
        return@Runnable
    }
    Thread.sleep(properties.pollIntervalMs)  // busy-wait 방지
}
```

```kotlin
// RedisStreamOperationExtensions.kt — BLOCK 옵션 없이 호출
fun StringRedisTemplate.xReadGroup(
    group: String,
    consumer: String,
    streamKey: String,
    count: Long = 10,
): List<MapRecord<String, String, String>>? =
    opsForStream<String, String>().read(
        Consumer.from(group, consumer),
        StreamReadOptions.empty().count(count),  // BLOCK 없음
        StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
    )
```

### 4.4 BLOCK 대비 트레이드오프

| 항목 | BLOCK 방식 | Non-blocking Polling 방식 |
|:---|:---|:---|
| 메시지 수신 지연 | 즉시 (0ms) | 최대 `pollIntervalMs` (기본 100ms) |
| CPU 사용 | 대기 중 0% | 매우 낮음 (100ms 간격 sleep) |
| 커넥션 관리 | dedicated 커넥션 필요 | shared pool 커넥션 재사용 |
| ElastiCache 호환성 | **불가** | **호환** |
| 구현 복잡도 | 낮음 (프레임워크 지원) | 중간 (polling loop 직접 구현) |

### 4.5 커넥션 풀 성능 비교 (검증 완료)

| 동시 구독 수 | Multiplexing (non-block) | Pool (non-block) | 성능 개선 |
|:---:|:---:|:---:|:---:|
| 1 | 107ms | 66ms | 1.6x |
| 32 | 704ms | 64ms | 11.0x |

커넥션 풀 기반 non-blocking 방식은 구독 수가 증가해도 일정한 성능을 유지한다.

---

## 5. 고려했으나 채택하지 않은 대안

### 5.1 `connection.execute()` 기반 BLOCK 폴링

```kotlin
redisTemplate.execute { connection ->
    connection.execute("XREADGROUP", "GROUP", group, consumer,
                       "BLOCK", "5000", "COUNT", "10",
                       "STREAMS", streamKey, ">")
}
```

- shared pool 커넥션을 사용하므로 BLOCK이 **정상 동작**
- 그러나 RESP 바이트를 직접 파싱해야 하며, `StreamOperations`의 역직렬화·타입 변환을 사용할 수 없음
- 유지보수 비용이 높아 채택하지 않음

---

## 6. 참고 사항

- 이 제약은 **ElastiCache Valkey(관리형 서비스) 고유의 제약**이며, 자체 호스팅 Redis에서는 발생하지 않는다.
- AWS에서 이 동작에 대한 공식 문서는 확인되지 않았으며, ElastiCache 프록시 계층의 내부 구현에 기인하는 것으로 판단된다.
- 향후 ElastiCache/Valkey 버전 업데이트로 이 제약이 해소될 가능성이 있으므로, 주기적으로 확인할 필요가 있다.
