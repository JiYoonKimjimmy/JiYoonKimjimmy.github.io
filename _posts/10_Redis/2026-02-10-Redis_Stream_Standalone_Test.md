---
layout: post
title : StandaloneConfiguration XREADGROUP BLOCK 검증 테스트
date  : 2026-02-10
image : redis.jpg
tags  : redis redis-Stream
---

# StandaloneConfiguration XREADGROUP BLOCK 검증 테스트

## 배경

[멀티플렉싱 단일 커넥션 테스트](./2026-02-10-Redis_Stream_Multiplexing_Test.md#테스트-결과)에서
기존 `RedisStaticMasterReplicaConfiguration` 경유 `XREADGROUP BLOCK`이
ElastiCache Valkey 엔진에서 `ERR [ENGINE] Invalid command`로 거부되는 것을 확인하였다.

`StaticMasterReplicaConnection`의 라우팅/핸드셰이크 과정이 원인으로 추정되므로,
라우팅 레이어가 없는 `RedisStandaloneConfiguration`으로 Master에 직접 연결하면
`XREADGROUP BLOCK`이 정상 동작하는지 검증한다.

## 테스트 환경

| 항목 | 값 |
|------|-----|
| Redis | Valkey 7.2.6 on Amazon ElastiCache (ap-northeast-2) |
| 접속 경로 | 로컬 VPN 경유 (`127.0.0.1:40198` → 개발 ElastiCache) |
| 클라이언트 | Lettuce 6.4.2 (Spring Boot 3.4.3 / Spring Data Redis) |
| 기존 ConnectionFactory | `RedisStaticMasterReplicaConfiguration` + `ReadFrom.REPLICA_PREFERRED` + RESP2 |
| 테스트 ConnectionFactory | `RedisStandaloneConfiguration` + `LettucePoolingClientConfiguration` + RESP2 |

## 핵심 검증 포인트

`RedisStandaloneConfiguration`으로 연결하면 `StatefulRedisMasterReplicaConnection` 라우팅 레이어를
거치지 않고 `StatefulRedisConnection`으로 직접 연결된다.
이 경로에서 `XREADGROUP BLOCK`이 ElastiCache Valkey에서 정상 동작하는지 확인한다.

```
기존 (실패):
  LettuceConnectionFactory
    └── StatefulRedisMasterReplicaConnection (라우팅 레이어)
          └── XREADGROUP BLOCK → ERR [ENGINE] Invalid command

검증 대상 (성공 기대):
  LettuceConnectionFactory
    └── StatefulRedisConnection (직접 연결)
          └── XREADGROUP BLOCK → 정상 동작?
```

## 테스트 구성

### TestConfiguration

```kotlin
import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.protocol.ProtocolVersion
import io.lettuce.core.resource.DefaultClientResources
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

/**
 * StandaloneConfiguration + 커넥션 풀로 Master에 직접 연결하는 테스트 설정.
 *
 * CLIENT SETINFO를 비활성화하여 ElastiCache Valkey와의 핸드셰이크 호환성 문제를 회피한다.
 * RedisURI에 libraryName/libraryVersion을 빈 문자열로 설정하면 CLIENT SETINFO가 전송되지 않는다.
 *
 * RedisConnectionFactory를 직접 빈으로 등록하면 기존 redisConnectFactory와 충돌하므로,
 * StandaloneStreamContext로 래핑하여 단일 빈으로 노출한다.
 */
@TestConfiguration
class StandaloneConnectionTestConfig {

    @Bean
    fun standaloneStreamContext(
        @Value("\${redis.master.host}") host: String,
        @Value("\${redis.master.port}") port: Int,
    ): StandaloneStreamContext {
        val standaloneConfig = RedisStandaloneConfiguration(host, port)

        val poolConfig = GenericObjectPoolConfig<Any>().apply {
            maxTotal = 40
            maxIdle = 20
            minIdle = 0
        }

        val clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .clientOptions(
                ClientOptions.builder()
                    .autoReconnect(true)
                    .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                    .protocolVersion(ProtocolVersion.RESP2)
                    .build(),
            )
            .commandTimeout(Duration.ofSeconds(10))
            .build()

        val connectionFactory = LettuceConnectionFactory(standaloneConfig, clientConfig).apply {
            afterPropertiesSet()
        }

        // CLIENT SETINFO를 비활성화한 별도 RedisClient 생성 (비교용)
        val redisUri = RedisURI.builder()
            .withHost(host)
            .withPort(port)
            .withLibraryName("")
            .withLibraryVersion("")
            .withTimeout(Duration.ofSeconds(10))
            .build()

        val clientOptions = ClientOptions.builder()
            .autoReconnect(true)
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .protocolVersion(ProtocolVersion.RESP2)
            .build()

        val noSetInfoClient = RedisClient.create(DefaultClientResources.create(), redisUri).apply {
            options = clientOptions
        }

        val redisTemplate = StringRedisTemplate(connectionFactory)

        return StandaloneStreamContext(connectionFactory, redisTemplate, noSetInfoClient)
    }

    data class StandaloneStreamContext(
        val connectionFactory: LettuceConnectionFactory,
        val redisTemplate: StringRedisTemplate,
        val noSetInfoClient: RedisClient,
    )
}
```

기존 멀티플렉싱 테스트(`StreamMultiplexingTestConfig`)와의 차이:

| 항목 | 멀티플렉싱 테스트 | Standalone 테스트 |
|------|----------------|-----------------|
| Configuration | `RedisStaticMasterReplicaConfiguration` (기존 `redisConnectFactory` 재사용) | `RedisStandaloneConfiguration` (신규 생성) |
| Connection Pool | 없음 (멀티플렉싱) | `LettucePoolingClientConfiguration` + `commons-pool2` |
| ReadFrom | `REPLICA_PREFERRED` | 없음 (Standalone은 라우팅 불필요) |
| 커넥션 타입 | `StatefulRedisMasterReplicaConnection` | `StatefulRedisConnection` |

### 시나리오 1: Stream 명령 호환성 진단

멀티플렉싱 테스트와 동일한 진단을 수행하여 비교한다.

```kotlin
@Test
@Order(1)
fun `진단 - StandaloneConfiguration에서 Stream 명령 호환성`() {
    val streamOps = redisTemplate.opsForStream<String, String>()
    val results = mutableListOf<String>()

    // Step 1: ConnectionFactory 타입 확인
    results.add("ConnectionFactory 타입: ${connectionFactory.javaClass.name}")
    results.add("StandaloneConfiguration 사용: ${connectionFactory.standaloneConfiguration != null}")

    // Step 2: XADD
    try {
        val recordId = streamOps.add(
            StreamRecords.string(mapOf("diag" to "1")).withStreamKey(streamKey),
        )
        results.add("XADD 성공: $recordId")
    } catch (e: Exception) {
        results.add("XADD 실패: ${e.message}")
    }

    // Step 3: XLEN
    try {
        val len = streamOps.size(streamKey)
        results.add("XLEN 결과: $len")
    } catch (e: Exception) {
        results.add("XLEN 실패: ${e.message}")
    }

    // Step 4: XREADGROUP (non-block)
    try {
        val messages = streamOps.read(
            Consumer.from(consumerGroup, "diag-consumer"),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
        )
        results.add("XREADGROUP (non-block) 수신: ${messages?.size}건")
    } catch (e: Exception) {
        results.add("XREADGROUP (non-block) 실패: ${e.cause?.message ?: e.message}")
    }

    // Step 5: XREADGROUP BLOCK 100ms (핵심 검증)
    try {
        streamOps.read(
            Consumer.from(consumerGroup, "diag-consumer-block"),
            StreamReadOptions.empty().count(10).block(Duration.ofMillis(100)),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
        )
        results.add("XREADGROUP BLOCK (Lettuce) 성공")
    } catch (e: Exception) {
        results.add("XREADGROUP BLOCK (Lettuce) 실패: ${e.cause?.message ?: e.message}")
    }

    // ... (raw execute, noSetInfoClient 등 추가 진단 단계는 실제 테스트 코드 참고)

    // Step 7: SET / GET / DEL
    try {
        redisTemplate.opsForValue().set("test:standalone:diag", "ok")
        val value = redisTemplate.opsForValue().get("test:standalone:diag")
        redisTemplate.delete("test:standalone:diag")
        results.add("SET/GET/DEL 성공 (value=$value)")
    } catch (e: Exception) {
        results.add("SET/GET/DEL 실패: ${e.message}")
    }

    println("\n═══ StandaloneConfiguration 진단 결과 ═══")
    results.forEach { println(it) }
    println("══════════════════════════════════════════\n")

    assertThat(results.any { it.contains("XADD 성공") })
        .withFailMessage("XADD가 실패하면 Stream 자체를 사용할 수 없음")
        .isTrue()
}
```

**기대 결과:**

| 명령 | StaticMasterReplica (기존) | Standalone (검증 대상) |
|------|:---:|:---:|
| `XADD` | OK | OK |
| `XLEN` | OK | OK |
| `XREADGROUP` (non-block) | OK | OK |
| `XREADGROUP BLOCK` | ERR | **OK (기대)** |
| `SET` / `GET` / `DEL` | OK | OK |

### 시나리오 2: Blocking polling 메시지 수신

`XREADGROUP BLOCK`이 동작한다면, `StreamMessageListenerContainer`에서 `pollTimeout > 0`으로
blocking polling 메시지 수신이 가능한지 확인한다.

```kotlin
@Test
@Order(2)
fun `Blocking polling으로 메시지 수신`() {
    val options = StreamMessageListenerContainerOptions.builder()
        .pollTimeout(Duration.ofMillis(2000)) // XREADGROUP BLOCK 2000
        .batchSize(10)
        .build()

    val container = StreamMessageListenerContainer
        .create(connectionFactory, options)

    val received = CopyOnWriteArrayList<MapRecord<String, String, String>>()

    container.receive(
        Consumer.from(consumerGroup, "consumer-0"),
        StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
        StreamListener { message -> received.add(message) },
    )

    container.start()

    // 메시지 10건 발행
    val streamOps = redisTemplate.opsForStream<String, String>()
    repeat(10) { i ->
        streamOps.add(
            StreamRecords.string(mapOf("index" to "$i")).withStreamKey(streamKey),
        )
    }

    // blocking polling이므로 pollTimeout 내 수신
    Thread.sleep(5000)

    println("═══ Blocking polling 결과 ═══")
    println("수신된 메시지: ${received.size}건 / 10건")
    println("═════════════════════════════")

    assertThat(received).hasSize(10)

    container.stop()
}
```

### 시나리오 3: Blocking polling 중 다른 Redis 명령 응답시간

BLOCK 대기 중 동일 ConnectionFactory(Pool)에서 다른 Redis 명령이 정상 동작하는지 확인한다.
커넥션 풀을 사용하므로 BLOCK이 다른 명령을 차단하지 않아야 한다.

```kotlin
@Test
@Order(3)
fun `Blocking polling 중 다른 Redis 명령 응답시간`() {
    val options = StreamMessageListenerContainerOptions.builder()
        .pollTimeout(Duration.ofMillis(2000))
        .batchSize(10)
        .build()

    val container = StreamMessageListenerContainer
        .create(connectionFactory, options)

    container.receive(
        Consumer.from(consumerGroup, "consumer-0"),
        StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
        StreamListener { /* no-op */ },
    )

    container.start()
    Thread.sleep(500)

    // BLOCK polling 중 다른 Redis 명령 실행 (10회 측정)
    val latencies = (1..10).map {
        val start = System.currentTimeMillis()
        redisTemplate.opsForValue().set("test:standalone:ping", "pong")
        val value = redisTemplate.opsForValue().get("test:standalone:ping")
        val elapsed = System.currentTimeMillis() - start
        assertThat(value).isEqualTo("pong")
        elapsed
    }

    println("═══ Blocking polling 중 Redis 명령 응답시간 ═══")
    println("평균: ${latencies.average().toLong()}ms")
    println("최대: ${latencies.max()}ms")
    println("최소: ${latencies.min()}ms")
    println("전체: $latencies")
    if (latencies.average() > 100) {
        println("⚠️ BLOCK polling이 다른 명령에 영향을 줌")
    } else {
        println("✅ BLOCK polling이 다른 명령에 영향 없음 (Pool 분리 정상)")
    }
    println("═══════════════════════════════════════════════")

    container.stop()
    redisTemplate.delete("test:standalone:ping")
}
```

### 시나리오 4: 다중 Subscription blocking polling

Container 1개에 N개 Subscription을 등록하고 blocking polling으로 메시지 수신 및 성능을 측정한다.
멀티플렉싱 테스트의 non-blocking 결과와 비교한다.

```kotlin
@ParameterizedTest
@ValueSource(ints = [1, 2, 4, 8, 16, 32])
@Order(4)
fun `Subscription N개에서 blocking polling 메시지 수신 + 다른 명령 응답시간`(subscriptionCount: Int) {
    val options = StreamMessageListenerContainerOptions.builder()
        .pollTimeout(Duration.ofMillis(2000)) // XREADGROUP BLOCK 2000
        .batchSize(10)
        .build()

    val container = StreamMessageListenerContainer
        .create(connectionFactory, options)

    val received = AtomicInteger(0)

    repeat(subscriptionCount) { i ->
        container.receive(
            Consumer.from(consumerGroup, "consumer-$i"),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            StreamListener { received.incrementAndGet() },
        )
    }

    container.start()
    Thread.sleep(1000)

    // 메시지 100건 발행
    val streamOps = redisTemplate.opsForStream<String, String>()
    repeat(100) { i ->
        streamOps.add(
            StreamRecords.string(mapOf("index" to "$i")).withStreamKey(streamKey),
        )
    }

    // 다른 Redis 명령 응답시간 측정 (10회)
    val latencies = (1..10).map {
        val start = System.currentTimeMillis()
        redisTemplate.opsForValue().set("test:standalone:latency", "check")
        redisTemplate.opsForValue().get("test:standalone:latency")
        System.currentTimeMillis() - start
    }

    Thread.sleep(5000)

    println(
        """
        |═══ Subscription ${subscriptionCount}개 결과 (blocking polling) ═══
        |수신 메시지: ${received.get()}건 / 100건
        |Redis 명령 응답시간:
        |  평균: ${latencies.average().toLong()}ms
        |  최대: ${latencies.max()}ms
        |  최소: ${latencies.min()}ms
        |  전체: $latencies
        |═══════════════════════════════════════════════════
        """.trimMargin(),
    )

    container.stop()
    redisTemplate.delete("test:standalone:latency")
}
```

**비교 기준 (멀티플렉싱 non-blocking 결과):**

| Subscription 수 | non-blocking SET+GET 평균 | blocking SET+GET 평균 (기대) |
|:---:|:---:|:---:|
| 1 | 107ms | < 107ms |
| 32 | 704ms | < 704ms |

커넥션 풀 사용 시 BLOCK이 별도 커넥션에서 실행되므로,
Subscription 수 증가에 따른 다른 명령 응답시간 저하가 크게 줄어들 것으로 기대한다.

## 판정 기준

| 케이스 | 조건 | 판정 |
|--------|------|------|
| **Case A**: BLOCK 정상 동작 + 성능 양호 | `XREADGROUP BLOCK` 성공 + 32 sub에서 SET+GET < 300ms | `StandaloneConfiguration` 채택 |
| **Case B**: BLOCK 정상 동작 + 성능 저하 | `XREADGROUP BLOCK` 성공 + 32 sub에서 SET+GET >= 300ms | Pool 사이징 조정 후 재검증 |
| **Case C**: BLOCK 여전히 실패 | `ERR [ENGINE] Invalid command` 동일 | non-blocking polling 또는 다른 대안 검토 |

## 테스트 코드 위치

```
src/test/kotlin/.../campaign/infrastructure/redis/stream/
├── StreamMultiplexingTestConfig.kt          ← 기존 (멀티플렉싱)
├── SingleSubscriptionTest.kt               ← 기존
├── MultipleSubscriptionTest.kt             ← 기존
├── CoexistenceTest.kt                      ← 기존
├── StandaloneConnectionTestConfig.kt       ← 신규 (Standalone + Pool)
└── StandaloneBlockTest.kt                  ← 신규 (BLOCK 검증)
```

## 재현 방법

```bash
# 로컬 VPN으로 개발 Redis 연결 후
./gradlew test --tests "*StandaloneBlockTest" --info
```

---

## 테스트 결과

> - **테스트 일자:** 2026-02-10
> - **테스트 대상:** Valkey 7.2.6 on Amazon ElastiCache (ap-northeast-2)
> - **테스트 환경:** 로컬 VPN 경유 개발 Redis (`127.0.0.1:40198`)
> - **클라이언트:** Lettuce 6.4.2 (Spring Boot 3.4.3 / Spring Data Redis)
> - **커넥션 설정:** `RedisStandaloneConfiguration` + `LettucePoolingClientConfiguration` + RESP2

---

### 1. 진단 테스트 결과

#### 1.1 Stream 명령 호환성 (기본 진단)

| 명령 | StaticMasterReplica (기존) | Standalone (이번 테스트) | 비고 |
|------|:---:|:---:|------|
| `XADD` | OK | OK | |
| `XLEN` | OK | OK | |
| `XGROUP CREATE` | OK | OK | |
| `XREADGROUP` (non-block) | OK | OK | |
| `XREADGROUP BLOCK` | ERR | **ERR** | `ERR [ENGINE] Invalid command` |
| `SET` / `GET` / `DEL` | OK | OK | |

#### 1.2 XREADGROUP BLOCK 원인 심층 분석

단일 진단 테스트에서 다양한 방법으로 `XREADGROUP BLOCK`을 시도하여 근본 원인을 추적하였다.

| 방식 | 커넥션 경로 | 결과 | 비고 |
|------|-----------|:---:|------|
| Lettuce StreamOps | 전용(dedicated) 커넥션 (새 생성) | **ERR** | Spring Data Redis가 blocking 감지 → 전용 커넥션 생성 |
| 풀 커넥션 native Lettuce API | 전용(dedicated) 커넥션 | **ERR** | `nativeConnection` → `getAsyncDedicatedConnection()` |
| no CLIENT SETINFO | 별도 RedisClient (새 생성) | **ERR** | CLIENT SETINFO 비활성화해도 동일 실패 |
| SET/GET 워밍업 후 | 별도 RedisClient (새 생성) | **ERR** | 일반 명령 워밍업 후에도 실패 |
| Stream 워밍업 후 | 별도 RedisClient (새 생성) | **ERR** | XREADGROUP non-block 워밍업 후에도 실패 |
| **raw execute** | **공유(shared) 풀 커넥션** | **OK** | `connection.execute()` → `getAsyncConnection()` |

#### 1.3 근본 원인: Spring Data Redis 커넥션 라우팅

Spring Data Redis `LettuceConnection`은 내부적으로 **두 종류의 커넥션 경로**를 사용한다:

```java
// LettuceConnection.getAsyncConnection() — execute()가 사용하는 경로
RedisClusterAsyncCommands getAsyncConnection() {
    if (isQueueing() || isPipelined()) {
        return getAsyncDedicatedConnection();  // 전용 커넥션
    }
    StatefulConnection sharedConnection = this.asyncSharedConnection;
    if (sharedConnection != null) {
        return sharedConnection.async();       // ← 공유 풀 커넥션 (우선 사용)
    }
    return getAsyncDedicatedConnection();
}
```

| 경로 | 사용하는 메서드 | XREADGROUP BLOCK |
|------|---------------|:---:|
| **공유(shared) 풀 커넥션** | `connection.execute()`, 일반 Redis 명령 | **OK** |
| **전용(dedicated) 커넥션** | `nativeConnection`, blocking 명령 감지 시 | **ERR** |

- `connection.execute("XREADGROUP", ...)` → `getAsyncConnection()` → **공유 풀 커넥션** → 성공
- `streamOps.read(BLOCK)` → Spring Data Redis blocking 감지 → **전용 커넥션 신규 생성** → 실패
- `connection.nativeConnection` → `getAsyncDedicatedConnection()` → **전용 커넥션** → 실패

**공유 풀 커넥션**은 `@BeforeEach`에서 `XGROUP CREATE` 실행 시 이미 생성되어 사용 중인 커넥션이고,
**전용 커넥션**은 blocking 명령을 위해 새로 생성되는 커넥션이다.

#### 1.4 배제된 가설

| 가설 | 결과 | 판정 |
|------|------|:---:|
| `StaticMasterReplicaConnection` 라우팅 레이어 문제 | Standalone에서도 동일 에러 | ❌ |
| `CLIENT SETINFO` 핸드셰이크 간섭 | CLIENT SETINFO 비활성화해도 동일 에러 | ❌ |
| 커넥션 워밍업 부족 | SET/GET, XREADGROUP non-block 워밍업 후에도 동일 에러 | ❌ |
| Lettuce 명령 직렬화 차이 | `connection.execute()`의 raw byte 직렬화도 동일한 RESP 생성 | ❌ |
| **신규 커넥션의 XREADGROUP BLOCK 거부** | 모든 새 커넥션에서 실패, 기존 커넥션에서만 성공 | **✅** |

**결론: ElastiCache Valkey 환경에서 새로 생성된 커넥션의 `XREADGROUP BLOCK`이 거부된다.**
기존에 이미 사용 중인 공유 풀 커넥션에서는 `connection.execute()`를 통해 `XREADGROUP BLOCK`이 정상 동작한다.

---

### 2. Blocking polling 메시지 수신 (시나리오 2)

`pollTimeout(Duration.ofMillis(2000))` → 내부적으로 `XREADGROUP BLOCK 2000` 실행

| 항목 | 결과 |
|------|------|
| 발행 메시지 | 10건 |
| 수신 메시지 | **0건 (0%)** |

`XREADGROUP BLOCK`이 실패하므로 `StreamMessageListenerContainer`가 메시지를 수신하지 못한다.
Container는 에러를 로깅하며 polling을 반복하지만, 매 시도마다 동일한 에러가 발생한다.

---

### 3. 다른 Redis 명령 응답시간 (시나리오 3)

`pollTimeout(Duration.ofMillis(2000))` blocking polling 중 SET+GET 응답시간:

| 항목 | 값 |
|------|-----|
| 평균 | 68ms |
| 최대 | 76ms |
| 최소 | 59ms |

**참고: BLOCK이 실패하므로 실제 BLOCK 대기가 발생하지 않아 응답시간이 양호하다.**
이 수치는 "BLOCK이 정상 동작할 때의 성능"이 아니라 "BLOCK 실패 후 Pool에서 다른 커넥션을 사용한 성능"이다.

---

### 4. 다중 Subscription 테스트 결과 (시나리오 4)

Container 1개 + Subscription N개, `pollTimeout=2000ms` (BLOCK 모드), 메시지 100건 발행

| Subscription 수 | 수신 | SET+GET 평균 | SET+GET 최대 | SET+GET 최소 |
|:---:|:---:|:---:|:---:|:---:|
| 1 | 0/100 | 66ms | 71ms | 59ms |
| 2 | 0/100 | 68ms | 94ms | 59ms |
| 4 | 0/100 | 70ms | 81ms | 64ms |
| 8 | 0/100 | 67ms | 74ms | 60ms |
| 16 | 0/100 | 64ms | 76ms | 61ms |
| 32 | 0/100 | 64ms | 72ms | 61ms |

**관찰:**

- 모든 Subscription 수에서 메시지 수신 **0건** (BLOCK 실패)
- SET+GET 응답시간은 **Subscription 수와 무관하게 64~70ms로 안정적**
- 멀티플렉싱 non-blocking 테스트(1개=107ms → 32개=704ms)와 비교하면 커넥션 풀 효과가 확인됨

#### 멀티플렉싱 non-blocking vs Standalone Pool 응답시간 비교

| Subscription 수 | 멀티플렉싱 non-blocking SET+GET | Standalone Pool SET+GET | 개선율 |
|:---:|:---:|:---:|:---:|
| 1 | 107ms | 66ms | 1.6x |
| 4 | 245ms | 70ms | 3.5x |
| 8 | 428ms | 67ms | 6.4x |
| 16 | 761ms | 64ms | 11.9x |
| 32 | 704ms | 64ms | 11.0x |

> 커넥션 풀을 사용하면 Subscription 수가 증가해도 다른 Redis 명령 응답시간에 영향이 없다.
> 이는 BLOCK이 정상 동작하더라도 동일하게 적용되는 커넥션 풀의 장점이다.

---

### 5. 결론

#### XREADGROUP BLOCK 실패 원인 확정

| 테스트 조건 | XREADGROUP BLOCK 결과 | 커넥션 유형 |
|------------|:---:|------|
| redis-cli 직접 실행 | **OK** | 직접 커넥션 |
| Lettuce + StaticMasterReplicaConfiguration | **ERR** | 전용 커넥션 (신규) |
| Lettuce + StandaloneConfiguration + Pool | **ERR** | 전용 커넥션 (신규) |
| CLIENT SETINFO 비활성화 | **ERR** | 별도 커넥션 (신규) |
| SET/GET, Stream 명령 워밍업 후 | **ERR** | 별도 커넥션 (신규) |
| **`connection.execute()` (raw)** | **OK** | **공유 풀 커넥션 (기존)** |

`XREADGROUP BLOCK` 실패의 근본 원인은:

1. Spring Data Redis가 blocking 명령 감지 시 **전용(dedicated) 커넥션을 신규 생성**
2. ElastiCache Valkey 환경에서 **신규 커넥션의 `XREADGROUP BLOCK`이 거부**됨
3. 기존에 사용 중인 공유 풀 커넥션에서는 `connection.execute()`를 통해 정상 동작

CLIENT SETINFO, 커넥션 구성 방식(StaticMasterReplica vs Standalone), 워밍업 모두 원인이 아니다.

#### 커넥션 풀 효과는 확인됨

- 32개 Subscription에서도 SET+GET 64ms (멀티플렉싱 non-blocking 704ms 대비 **11배 개선**)
- Subscription 수 증가에 따른 응답시간 저하 없음

#### 테스트 시나리오별 판정

| 케이스 | 조건 | 판정 |
|--------|------|------|
| Case A: BLOCK 정상 동작 + 성능 양호 | - | ❌ 해당 없음 |
| Case B: BLOCK 정상 동작 + 성능 저하 | - | ❌ 해당 없음 |
| **Case C: BLOCK 여전히 실패** | 전용 커넥션에서 `ERR [ENGINE] Invalid command` | **✅ 이 케이스에 해당** |

#### 권장 사항

1. **non-blocking polling (`pollTimeout=Duration.ZERO`) + 커넥션 풀**: BLOCK을 사용하지 않으므로 문제 회피. 커넥션 풀로 멀티플렉싱 성능 이슈 해소. 단, tight loop으로 인한 CPU/네트워크 부하 존재.
2. **커스텀 poll loop 구현**: non-blocking polling + `Thread.sleep` 간격으로 tight loop 완화. `StreamMessageListenerContainer`의 `pollTimeout`은 내부적으로 XREADGROUP BLOCK 또는 XREADGROUP non-block + sleep이므로, 커스텀 구현이 필요할 수 있음.
3. **`connection.execute()` 기반 BLOCK polling**: 공유 풀 커넥션 경로(`getAsyncConnection()`)로 XREADGROUP BLOCK이 동작하는 것이 확인됨. `StreamMessageListenerContainer`를 커스텀하여 이 경로를 사용하면 BLOCK polling 가능. 단, Spring Data Redis 내부 API 의존으로 유지보수 부담.
4. **Lettuce / ElastiCache 호환성 패치 대기**: Lettuce ↔ ElastiCache Valkey 간 신규 커넥션 XREADGROUP BLOCK 호환성 이슈가 해결될 때까지 non-blocking polling 사용.

---

### 6. 테스트 코드 위치 (결과)

```
src/test/kotlin/.../campaign/infrastructure/redis/stream/
├── StandaloneConnectionTestConfig.kt   ← @TestConfiguration (Standalone + Pool + noSetInfoClient)
└── StandaloneBlockTest.kt              ← BLOCK 검증 (진단 + 4개 시나리오)
```

### 7. 재현 방법 (결과)

위와 동일.

### 8. 참고 자료

- [멀티플렉싱 단일 커넥션 테스트](./2026-02-10-Redis_Stream_Multiplexing_Test.md#테스트-결과) — `StaticMasterReplicaConfiguration`에서의 동일 에러 확인
- [Lettuce GitHub Issue #2817](https://github.com/redis/lettuce/issues/2817) — CLIENT SETINFO 핸드셰이크 이슈 (fire-and-forget 처리됨)
- [AWS ElastiCache Supported Commands](https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/SupportedCommands.html) — `XREADGROUP`은 공식 지원 명령
- [AWS ElastiCache Lettuce Client Configuration](https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/BestPractices.Clients-lettuce.html) — ElastiCache 환경 Lettuce 설정 가이드
- [Spring Data Redis LettuceConnection 소스](https://github.com/spring-projects/spring-data-redis) — `getAsyncConnection()` vs `getAsyncDedicatedConnection()` 커넥션 라우팅 로직
