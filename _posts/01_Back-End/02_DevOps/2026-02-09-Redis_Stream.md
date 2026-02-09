---
layout: post
title : Redis Streams
date  : 2026-02-09
image : redis.jpg
tags  : redis redis-Stream
---

# Redis Streams

> 참고: [Redis Streams 공식 문서](https://redis.io/docs/latest/develop/data-types/streams/)

---

## 목차

1. [개요](#1-개요)
2. [기본 개념](#2-기본-개념)
3. [핵심 명령어](#3-핵심-명령어)
4. [Consumer Group](#4-consumer-group)
5. [메시지 신뢰성 보장](#5-메시지-신뢰성-보장)
6. [Stream 관리](#6-stream-관리)
7. [Pub/Sub 및 List와의 비교](#7-pubsub-및-list와의-비교)
8. [활용 사례](#8-활용-사례)
9. [Spring Data Redis 연동](#9-spring-data-redis-연동)

---

## 1. 개요

Redis Streams는 **Redis 5.0**에서 도입된 append-only 로그 데이터 구조이다. Apache Kafka의 로그 기반 메시징 모델에서 영감을 받았으며, Redis 내에서 경량 메시지 브로커 역할을 수행한다.

### 핵심 특성

| 특성 | 설명 |
|------|------|
| **Append-only** | 새로운 항목은 항상 끝에 추가되며, 기존 항목은 변경 불가 |
| **영속성** | 메시지가 Stream에 저장되어 소비 후에도 유지됨 |
| **고유 ID** | 각 항목에 시간 기반의 고유 ID가 자동 부여됨 |
| **Consumer Group** | 여러 소비자가 메시지를 분산 처리할 수 있음 |
| **범위 조회** | ID 기반으로 특정 범위의 항목을 조회 가능 |

---

## 2. 기본 개념

### 2.1 Stream Entry 구조

각 Stream entry는 **고유 ID**와 **하나 이상의 field-value 쌍**으로 구성된다. Redis Hash와 유사한 구조이다.

```
Entry ID: 1526919030474-0
┌─────────────┬──────────┐
│ Field       │ Value    │
├─────────────┼──────────┤
│ name        │ Sara     │
│ surname     │ OConnor  │
└─────────────┴──────────┘
```

### 2.2 Entry ID

Entry ID는 `<millisecondsTime>-<sequenceNumber>` 형식이다.

- **millisecondsTime**: Redis 노드의 로컬 시간 (밀리초 단위 Unix timestamp)
- **sequenceNumber**: 동일 밀리초 내에서의 순번 (0부터 시작)

```bash
# 자동 생성 (권장)
> XADD mystream * name Sara
"1526919030474-0"

# 명시적 지정 (ID는 항상 증가해야 함)
> XADD mystream 1526919030474-55 message "Hello"
"1526919030474-55"

# 부분 ID (시퀀스 자동 생성, Redis 7.0+)
> XADD mystream 1526919030474-* message "World"
"1526919030474-56"
```

**ID 규칙:**
- ID는 항상 증가해야 한다 (이전 ID보다 큰 값만 허용)
- 최소 유효 ID: `0-1`
- 특수 ID: `$` (최신), `-` (최소), `+` (최대)

### 2.3 데이터 구조 내부 (Radix Tree)

Redis Streams는 내부적으로 **Radix Tree(기수 트리)**를 사용하여 항목을 저장한다. 이 구조 덕분에 ID 기반의 범위 조회가 효율적이며, 메모리 사용도 최적화된다.

---

## 3. 핵심 명령어

### 3.1 XADD - 항목 추가

Stream에 새로운 entry를 추가한다. Stream이 존재하지 않으면 자동 생성된다.

```
XADD key [NOMKSTREAM] [MAXLEN | MINID [= | ~] threshold [LIMIT count]] <* | id> field value [field value ...]
```

```bash
# 기본 사용
> XADD mystream * name Sara surname OConnor
"1526919030474-0"

# 여러 필드 추가
> XADD mystream * field1 value1 field2 value2 field3 value3
"1526919030475-0"

# Stream 미존재 시 생성하지 않음 (Redis 6.2+)
> XADD nonexistent NOMKSTREAM * field value
(nil)

# 추가와 동시에 트리밍 (대략적)
> XADD mystream MAXLEN ~ 1000 * field value
```

| 옵션 | 설명 | 도입 버전 |
|------|------|----------|
| `NOMKSTREAM` | Stream이 없으면 생성하지 않음 | 6.2 |
| `MAXLEN [= \| ~] N` | 최대 N개 항목 유지 (초과 시 오래된 항목 제거) | 5.0 |
| `MINID [= \| ~] id` | 지정 ID보다 작은 항목 제거 | 6.2 |
| `LIMIT count` | 트리밍 시 검사할 최대 항목 수 | 6.2 |

- **시간 복잡도**: O(1) (추가), O(N) (트리밍, N = 제거되는 항목 수)

### 3.2 XREAD - 항목 읽기

하나 이상의 Stream에서 항목을 읽는다. 여러 Stream을 동시에 읽을 수 있다.

```
XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] id [id ...]
```

```bash
# 비블로킹 - 처음부터 2개 읽기
> XREAD COUNT 2 STREAMS mystream 0-0
1) 1) "mystream"
   2) 1) 1) 1526984818136-0
         2) 1) "duration"
            2) "1532"
      2) 1) 1526999352406-0
         2) 1) "duration"
            2) "812"

# 블로킹 - 새 항목이 올 때까지 최대 5초 대기
> XREAD BLOCK 5000 COUNT 100 STREAMS mystream $

# 여러 Stream 동시 읽기
> XREAD COUNT 2 STREAMS mystream writers 0-0 0-0
```

| 파라미터 | 설명 |
|---------|------|
| `COUNT count` | 각 Stream당 반환할 최대 항목 수 |
| `BLOCK milliseconds` | 블로킹 대기 시간 (0 = 무한 대기) |
| `$` | 현재 시점 이후의 새 항목만 읽기 (첫 호출에만 사용) |
| `+` | 마지막 항목만 읽기 (Redis 7.4+) |

- **시간 복잡도**: O(M) (M = 반환 항목 수)

> **주의**: `$`는 첫 번째 호출에서만 사용하고, 이후에는 마지막으로 받은 ID를 사용해야 한다. 그렇지 않으면 폴링 사이에 도착한 메시지가 누락될 수 있다.

### 3.3 XRANGE / XREVRANGE - 범위 조회

ID 범위로 항목을 조회한다.

```
XRANGE key start end [COUNT count]
XREVRANGE key end start [COUNT count]
```

```bash
# 전체 조회
> XRANGE mystream - +

# 특정 범위 조회
> XRANGE mystream 1526984818136-0 1526999352406-0

# 단일 항목 조회
> XRANGE mystream 1526984818136-0 1526984818136-0

# 페이지네이션 (exclusive range)
> XRANGE mystream (1526984818136-0 + COUNT 10

# 역순 조회
> XREVRANGE mystream + - COUNT 5
```

- **특수 ID**: `-` (최소 ID), `+` (최대 ID)
- **Exclusive Range**: ID 앞에 `(`를 붙이면 해당 ID 제외
- **시간 복잡도**: O(N) (N = 반환 항목 수)

### 3.4 XLEN - 항목 수 조회

```bash
> XLEN mystream
(integer) 3
```

- **시간 복잡도**: O(1)

### 3.5 XDEL - 항목 삭제

```bash
> XDEL mystream 1526984818136-0
(integer) 1
```

---

## 4. Consumer Group

Consumer Group은 여러 소비자(Consumer)가 하나의 Stream을 **분산 처리**하기 위한 메커니즘이다. Kafka의 Consumer Group 개념과 유사하다.

### 4.1 핵심 개념

```
                        ┌──────────────┐
                        │   Stream     │
                        │  (mystream)  │
                        └──────┬───────┘
                               │
                    ┌──────────┴──────────┐
                    │   Consumer Group    │
                    │    (mygroup)        │
                    ├─────────────────────┤
                    │ last_delivered_id   │
                    │ PEL (Pending List)  │
                    └──┬───────┬───────┬──┘
                       │       │       │
                  ┌────┴──┐ ┌──┴───┐ ┌─┴─────┐
                  │ C1    │ │ C2   │ │ C3    │
                  │ (PEL) │ │(PEL) │ │ (PEL) │
                  └───────┘ └──────┘ └───────┘
```

**동작 원리:**
- 각 메시지는 그룹 내 **단 하나의 Consumer**에게만 전달됨
- 메시지는 처리 완료(ACK)될 때까지 **PEL(Pending Entries List)**에 유지됨
- Consumer가 실패하면 다른 Consumer가 미처리 메시지를 인수(Claim)할 수 있음

### 4.2 XGROUP CREATE - 그룹 생성

```
XGROUP CREATE key group <id | $> [MKSTREAM] [ENTRIESREAD entries-read]
```

```bash
# Stream 처음부터 읽기
> XGROUP CREATE mystream mygroup 0

# 새 항목부터 읽기
> XGROUP CREATE mystream mygroup $

# Stream이 없으면 자동 생성
> XGROUP CREATE mystream mygroup $ MKSTREAM
```

| 옵션 | 설명 |
|------|------|
| `0` | Stream의 처음부터 읽기 시작 |
| `$` | 현재 시점 이후의 새 항목부터 읽기 |
| `MKSTREAM` | Stream이 없으면 길이 0으로 자동 생성 |
| `ENTRIESREAD N` | Lag 추적을 위한 읽은 항목 수 설정 (Redis 7.0+) |

### 4.3 XREADGROUP - 그룹으로 읽기

```
XREADGROUP GROUP group consumer [COUNT count] [BLOCK milliseconds] [NOACK] STREAMS key [key ...] id [id ...]
```

```bash
# 새 메시지 읽기
> XREADGROUP GROUP mygroup consumer1 COUNT 10 STREAMS mystream >

# 블로킹으로 대기
> XREADGROUP GROUP mygroup consumer1 BLOCK 2000 COUNT 10 STREAMS mystream >

# Consumer의 pending 메시지 조회
> XREADGROUP GROUP mygroup consumer1 STREAMS mystream 0
```

| ID 값 | 동작 |
|-------|------|
| `>` | 아직 전달되지 않은 새 메시지만 수신 |
| `0` 또는 특정 ID | 해당 Consumer의 pending 메시지만 조회 |

**NOACK 옵션**: PEL에 추가하지 않음. 메시지 손실을 허용할 수 있는 경우에 사용.

### 4.4 XACK - 처리 완료 확인

```bash
> XACK mystream mygroup 1526569495631-0
(integer) 1

# 여러 메시지 한 번에 확인
> XACK mystream mygroup id1 id2 id3
```

`XACK`는 PEL에서 메시지를 제거하여 메모리를 해제한다. 처리가 완료된 메시지는 반드시 ACK 해야 한다.

### 4.5 기본 처리 루프

```
시작 시:
    # 1단계: 이전 크래시로 인한 미처리 메시지 복구
    WHILE true
        entries = XREADGROUP GROUP mygroup consumer1 COUNT 10 STREAMS mystream 0
        IF entries가 비어있음
            BREAK   # pending 메시지 모두 처리 완료
        FOR EACH message IN entries
            process(message)
            XACK mystream mygroup message.id
    END

    # 2단계: 새 메시지 처리
    WHILE true
        entries = XREADGROUP GROUP mygroup consumer1 BLOCK 2000 COUNT 10 STREAMS mystream >
        IF entries == nil
            CONTINUE    # 타임아웃
        FOR EACH message IN entries
            process(message)
            XACK mystream mygroup message.id
    END
```

---

## 5. 메시지 신뢰성 보장

### 5.1 PEL (Pending Entries List)

PEL은 Consumer에게 전달되었지만 아직 ACK되지 않은 메시지들의 목록이다.

```
┌─ PEL Entry ────────────────────────────┐
│ Message ID: 1526569495631-0            │
│ Consumer: consumer1                     │
│ Delivery Time: 1526569495631           │
│ Delivery Count: 1                       │
└─────────────────────────────────────────┘
```

### 5.2 XPENDING - Pending 메시지 조회

```bash
# 요약 조회
> XPENDING mystream mygroup
1) (integer) 2                    # 총 pending 메시지 수
2) 1526984818136-0                # 최소 ID
3) 1526984818137-0                # 최대 ID
4) 1) 1) "consumer1"             # Consumer별 pending 수
      2) "2"

# 상세 조회
> XPENDING mystream mygroup - + 10
1) 1) 1526984818136-0            # 메시지 ID
   2) "consumer1"                # Consumer
   3) (integer) 196415           # 마지막 전달 후 경과 시간 (ms)
   4) (integer) 1                # 전달 횟수

# 특정 시간 이상 유휴 상태인 메시지만 조회 (Redis 6.2+)
> XPENDING mystream mygroup IDLE 60000 - + 10
```

### 5.3 XCLAIM - 메시지 소유권 이전

실패한 Consumer의 메시지를 다른 Consumer가 인수받는다.

```bash
# 1시간 이상 유휴 상태인 메시지를 Alice에게 할당
> XCLAIM mystream mygroup Alice 3600000 1526569498055-0
1) 1) 1526569498055-0
   2) 1) "message"
      2) "orange"
```

| 옵션 | 설명 |
|------|------|
| `IDLE ms` | 유휴 시간을 지정 값으로 설정 |
| `RETRYCOUNT count` | 재시도 횟수 설정 |
| `FORCE` | PEL에 없는 ID도 강제 claim |
| `JUSTID` | ID만 반환, 재시도 횟수 미증가 |

### 5.4 XAUTOCLAIM - 자동 소유권 이전 (Redis 6.2+)

`XPENDING` + `XCLAIM`을 결합한 편의 명령어이다.

```bash
> XAUTOCLAIM mystream mygroup Alice 3600000 0-0 COUNT 25
1) "0-0"                         # 다음 호출 시 사용할 커서
2) 1) 1) "1609338752495-0"       # claim된 메시지
      2) 1) "field"
         2) "value"
3) (empty array)                 # 삭제된 메시지 ID
```

SCAN 방식으로 동작하므로 대규모 PEL에서도 효율적이다.

### 5.5 장애 복구 패턴

```
Consumer 장애 감지 시:

1. XPENDING으로 실패한 Consumer의 pending 메시지 확인
2. XAUTOCLAIM으로 유휴 메시지를 다른 Consumer에게 이전
3. 전달 횟수(delivery count)가 임계값을 초과하면 Dead Letter Queue로 이동
```

---

## 6. Stream 관리

### 6.1 XTRIM - Stream 크기 조절

```
XTRIM key <MAXLEN | MINID> [= | ~] threshold [LIMIT count]
```

```bash
# 정확히 1000개만 유지
> XTRIM mystream MAXLEN 1000

# 대략적 트리밍 (성능 우선)
> XTRIM mystream MAXLEN ~ 1000

# 특정 ID 이전 항목 제거
> XTRIM mystream MINID 649085820
```

| 전략 | 설명 |
|------|------|
| `MAXLEN = N` | 정확히 N개까지 트리밍 |
| `MAXLEN ~ N` | 대략 N개까지 트리밍 (더 효율적) |
| `MINID = id` | 지정 ID보다 작은 항목 제거 |
| `MINID ~ id` | 대략적으로 지정 ID 이전 항목 제거 |

> **`~` (approximate) 사용 권장**: 정확한 트리밍은 매 호출마다 전체를 검사해야 하지만, 대략적 트리밍은 매크로 노드 단위로 처리하여 훨씬 효율적이다.

### 6.2 XINFO - Stream 정보 조회

```bash
# Stream 기본 정보
> XINFO STREAM mystream

# Consumer Group 정보
> XINFO GROUPS mystream

# Consumer 정보
> XINFO CONSUMERS mystream mygroup
```

### 6.3 메모리 관리 전략

| 전략 | 설명 | 적합한 경우 |
|------|------|------------|
| `XADD ... MAXLEN ~` | 추가 시 동시에 트리밍 | 실시간 처리, 크기 제한 필요 시 |
| `XTRIM MAXLEN ~` | 별도 배치로 트리밍 | 주기적 정리 |
| `XTRIM MINID ~` | 시간 기반 트리밍 | 보존 기간 기반 관리 |
| TTL (EXPIRE) | Stream 키 자체에 TTL 설정 | 임시 Stream |

---

## 7. Pub/Sub 및 List와의 비교

| 기능 | Redis Streams | Pub/Sub | List (BRPOP) |
|------|--------------|---------|--------------|
| **메시지 영속성** | O (저장됨) | X (Fire & Forget) | X (소비 시 삭제) |
| **Consumer Group** | O | X | X |
| **다중 소비자 (Fan-out)** | O | O | X |
| **분산 처리** | O (Consumer Group) | X | 부분적 (경쟁 소비) |
| **메시지 재읽기** | O (ID 기반) | X | X |
| **메시지 ACK** | O | X | X |
| **범위 조회** | O | X | X |
| **블로킹 읽기** | O | O | O |
| **메시지 순서 보장** | O | O | O |
| **Dead Letter 처리** | O (PEL + XCLAIM) | X | X |

**사용 기준:**
- **Pub/Sub**: 실시간 알림, 메시지 손실 허용, 구독자 모두 수신 필요
- **List**: 단순 작업 큐, 1:1 소비
- **Streams**: 메시지 영속성, 분산 처리, 신뢰성 보장이 필요한 메시지 큐

---

## 8. 활용 사례

### 8.1 이벤트 소싱 (Event Sourcing)

```bash
# 주문 이벤트 기록
> XADD order-events * type ORDER_CREATED orderId 12345 userId 100
> XADD order-events * type PAYMENT_COMPLETED orderId 12345 amount 50000
> XADD order-events * type ORDER_SHIPPED orderId 12345 trackingNo TRK001

# 특정 시점 이후 이벤트 조회
> XRANGE order-events 1526984818136-0 +
```

### 8.2 실시간 데이터 파이프라인

```bash
# Producer: 센서 데이터 수집
> XADD sensor:temp * deviceId D001 temperature 23.5 timestamp 1609459200

# Consumer Group으로 분산 처리
> XGROUP CREATE sensor:temp analytics-group 0 MKSTREAM
> XREADGROUP GROUP analytics-group worker1 COUNT 100 BLOCK 5000 STREAMS sensor:temp >
```

### 8.3 작업 큐 (Task Queue)

```bash
# 작업 등록
> XADD task-queue * type email to user@example.com subject "Welcome"

# 워커가 분산으로 처리
> XREADGROUP GROUP workers worker-1 COUNT 1 BLOCK 0 STREAMS task-queue >
# ... 처리 후 ACK
> XACK task-queue workers 1526569495631-0
```

### 8.4 활동 로그 / 감사 추적

```bash
# 사용자 활동 기록
> XADD audit-log * userId 100 action LOGIN ip 192.168.1.1
> XADD audit-log * userId 100 action UPDATE_PROFILE field email

# 시간 기반으로 보존 (7일)
> XTRIM audit-log MINID ~ <7일_전_timestamp>
```

---

## 9. Spring Data Redis 연동

> 참고: [Spring Data Redis - Streams](https://docs.spring.io/spring-data/redis/reference/redis/redis-streams.html)

Spring Data Redis 2.2부터 Redis Streams를 지원한다. 명령형(Imperative)과 반응형(Reactive) 두 가지 프로그래밍 모델을 모두 지원한다.

### 9.1 의존성

```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

### 9.2 메시지 Produce

#### RedisTemplate 사용

```java
@RequiredArgsConstructor
@Service
public class StreamProducer {

    private final RedisTemplate<String, Object> redisTemplate;

    // Map 기반 레코드 추가
    public RecordId send(String streamKey, Map<String, String> data) {
        StringRecord record = StreamRecords.string(data)
            .withStreamKey(streamKey);
        return redisTemplate.opsForStream().add(record);
    }

    // Object 기반 레코드 추가
    public RecordId send(String streamKey, Object payload) {
        ObjectRecord<String, Object> record = StreamRecords.newRecord()
            .in(streamKey)
            .ofObject(payload);
        return redisTemplate.opsForStream().add(record);
    }
}
```

#### MAXLEN을 적용한 추가

```java
StringRecord record = StreamRecords.string(data).withStreamKey("my-stream");
redisTemplate.opsForStream().add(record, StreamAddOptions.maxLen(1000).approximate());
```

### 9.3 메시지 Consume (동기)

```java
// 최신 메시지 읽기
List<MapRecord<String, String, String>> messages = redisTemplate.opsForStream()
    .read(StreamReadOptions.empty().count(10),
          StreamOffset.latest("my-stream"));

// Consumer Group에서 읽기
List<MapRecord<String, String, String>> messages = redisTemplate.opsForStream()
    .read(Consumer.from("my-group", "my-consumer"),
          StreamReadOptions.empty().count(10),
          StreamOffset.create("my-stream", ReadOffset.lastConsumed()));
```

### 9.4 메시지 Consume (비동기) - StreamMessageListenerContainer

비동기 메시지 수신을 위한 핵심 컴포넌트이다. 스레드 관리와 런타임 구독 변경을 지원한다.

#### StreamListener 구현

```java
@Slf4j
@Component
public class OrderEventListener implements StreamListener<String, MapRecord<String, String, String>> {

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        log.info("Message ID: {}", message.getId());
        log.info("Stream: {}", message.getStream());
        log.info("Body: {}", message.getValue());
    }
}
```

#### Container 설정

```java
@Configuration
public class RedisStreamConfig {

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer(
            RedisConnectionFactory connectionFactory,
            OrderEventListener listener) {

        var options = StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ofMillis(100))
            .batchSize(10)
            .build();

        var container = StreamMessageListenerContainer.create(connectionFactory, options);

        // 단순 구독 (Consumer Group 없이)
        container.receive(
            StreamOffset.fromStart("my-stream"),
            listener
        );

        // Consumer Group으로 구독
        container.receive(
            Consumer.from("my-group", "my-consumer"),
            StreamOffset.create("my-stream", ReadOffset.lastConsumed()),
            listener
        );

        // 자동 ACK로 구독
        container.receiveAutoAck(
            Consumer.from("my-group", "my-consumer"),
            StreamOffset.create("my-stream", ReadOffset.lastConsumed()),
            listener
        );

        container.start();
        return container;
    }
}
```

### 9.5 Consumer Group 관리

```java
@RequiredArgsConstructor
@Service
public class StreamGroupManager {

    private final RedisTemplate<String, Object> redisTemplate;

    // Consumer Group 생성
    public void createGroup(String streamKey, String groupName) {
        try {
            redisTemplate.opsForStream()
                .createGroup(streamKey, ReadOffset.from("0"), groupName);
        } catch (RedisSystemException e) {
            // BUSYGROUP: 이미 존재하는 그룹
            log.warn("Consumer group already exists: {}", groupName);
        }
    }

    // 메시지 ACK
    public void acknowledge(String streamKey, String groupName, RecordId... recordIds) {
        redisTemplate.opsForStream().acknowledge(streamKey, groupName, recordIds);
    }

    // Pending 메시지 조회
    public PendingMessagesSummary pending(String streamKey, String groupName) {
        return redisTemplate.opsForStream().pending(streamKey, groupName);
    }
}
```

### 9.6 Object Mapping

Stream entry를 도메인 객체로 자동 매핑할 수 있다.

```java
// 도메인 객체
@Data
public class OrderEvent {
    private String orderId;
    private String type;
    private String userId;
}

// ObjectRecord로 추가
ObjectRecord<String, OrderEvent> record = StreamRecords.newRecord()
    .in("order-events")
    .ofObject(new OrderEvent("12345", "CREATED", "user1"));
redisTemplate.opsForStream().add(record);

// ObjectRecord로 읽기
List<ObjectRecord<String, OrderEvent>> records = redisTemplate.opsForStream()
    .read(OrderEvent.class, StreamOffset.fromStart("order-events"));
```

#### ObjectRecord 기반 Listener

```java
@Component
public class OrderEventObjectListener
        implements StreamListener<String, ObjectRecord<String, OrderEvent>> {

    @Override
    public void onMessage(ObjectRecord<String, OrderEvent> message) {
        OrderEvent event = message.getValue();
        // event.getOrderId(), event.getType() ...
    }
}

// Container에 ObjectMapper 설정
var options = StreamMessageListenerContainerOptions.builder()
    .pollTimeout(Duration.ofMillis(100))
    .targetType(OrderEvent.class)  // 자동 매핑
    .build();
```

### 9.7 ReadOffset 전략

| ReadOffset | 단독 사용 | Consumer Group |
|------------|----------|----------------|
| `ReadOffset.latest()` | 최신 메시지 읽기 | 최신 메시지 읽기 |
| `ReadOffset.from(id)` | 특정 ID 이후 읽기 | 특정 ID 이후 읽기 |
| `ReadOffset.lastConsumed()` | 지원 안 함 | 마지막 소비된 메시지 이후 (`>`) |

> **주의**: `ReadOffset.latest()`는 폴링 간격 사이에 도착한 메시지가 누락될 수 있다. 메시지 손실이 허용되지 않는 경우 `lastConsumed()` 또는 특정 ID를 사용해야 한다.

### 9.8 반응형 (Reactive) - StreamReceiver

```java
ReactiveRedisConnectionFactory connectionFactory = ...;

var options = StreamReceiverOptions.builder()
    .pollTimeout(Duration.ofMillis(100))
    .build();

StreamReceiver<String, MapRecord<String, String, String>> receiver =
    StreamReceiver.create(connectionFactory, options);

Flux<MapRecord<String, String, String>> messages =
    receiver.receive(StreamOffset.fromStart("my-stream"));

messages
    .doOnNext(message -> {
        log.info("ID: {}, Body: {}", message.getId(), message.getValue());
    })
    .subscribe();
```

반응형 모델은 **Backpressure** 기반의 수요 주도형 소비를 지원하여, 소비자의 처리 속도에 따라 메시지를 가져온다.

---

## 참고 자료

- [Redis Streams - 공식 문서](https://redis.io/docs/latest/develop/data-types/streams/)
- [Redis Commands - XADD](https://redis.io/docs/latest/commands/xadd/)
- [Redis Commands - XREAD](https://redis.io/docs/latest/commands/xread/)
- [Redis Commands - XREADGROUP](https://redis.io/docs/latest/commands/xreadgroup/)
- [Redis Commands - XACK](https://redis.io/docs/latest/commands/xack/)
- [Redis Commands - XPENDING](https://redis.io/docs/latest/commands/xpending/)
- [Redis Commands - XCLAIM](https://redis.io/docs/latest/commands/xclaim/)
- [Redis Commands - XAUTOCLAIM](https://redis.io/docs/latest/commands/xautoclaim/)
- [Redis Commands - XTRIM](https://redis.io/docs/latest/commands/xtrim/)
- [Spring Data Redis - Streams](https://docs.spring.io/spring-data/redis/reference/redis/redis-streams.html)
