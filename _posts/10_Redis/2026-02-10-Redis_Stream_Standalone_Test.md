---
layout: post
title : StandaloneConfiguration XREADGROUP BLOCK 검증 테스트
date  : 2026-02-10
image : redis.jpg
tags  : redis redis-Stream
---

# StandaloneConfiguration XREADGROUP BLOCK 검증 테스트

## 배경

[멀티플렉싱 단일 커넥션 테스트](./2026-02-10-Redis_Stream_Multiplexing_Test_Result.md)에서
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
package com.musinsapayments.prepay.application.prepay.admin.campaign.infrastructure.redis.stream

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
