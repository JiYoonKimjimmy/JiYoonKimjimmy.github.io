---
layout: post
title : Redis Stream 멀티플렉싱 단일 커넥션 테스트 계획
date  : 2026-02-10
image : redis.jpg
tags  : redis redis-Stream
---

# Redis Stream 멀티플렉싱 단일 커넥션 테스트 계획

## 목적

현재 프로젝트의 Redis 설정은 Lettuce 기본 모드(단일 커넥션 멀티플렉싱)를 사용하고 있다.
별도 Connection Pool 없이 기존 `redisTemplate`만으로 `StreamMessageListenerContainer`를 구성했을 때
**실제로 문제가 발생하는지** 검증한다.

### 검증하려는 가설

> "Lettuce 멀티플렉싱 모드에서 `XREADGROUP BLOCK`을 실행하면
> 단일 커넥션이 점유되어 다른 Redis 명령이 블로킹된다"

이 가설이 실제로 재현되는지, 그리고 어느 수준의 동시성에서 문제가 발생하는지 확인한다.

---

## 테스트 구성

### 사용할 기존 인프라 (변경 없음)

```
기존 RedisTemplateConfig
├── redisConnectFactory (RedisStaticMasterReplicaConfiguration, REPLICA_PREFERRED, RESP2)
└── redisTemplate (GenericJackson2JsonRedisSerializer)
```

### 테스트 전용 컴포넌트

기존 Bean을 그대로 사용하되, **Stream 전용 StringRedisTemplate 1개만 추가**한다.

```kotlin
package com.musinsapayments.prepay.application.prepay.admin.campaign.infrastructure.redis.stream

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

@TestConfiguration
class StreamMultiplexingTestConfig {

    /**
     * 기존 redisConnectFactory(멀티플렉싱)를 그대로 사용하는 StringRedisTemplate.
     * 별도 ConnectionFactory를 만들지 않는다.
     *
     * 별도 StringRedisTemplate을 만드는 이유:
     * 기존 redisTemplate의 hashValueSerializer가 GenericJackson2JsonRedisSerializer라서
     * Stream entry에 타입 정보가 들어간다.
     * Connection은 동일한 멀티플렉싱 커넥션을 공유한다.
     */
    @Bean
    fun testStreamRedisTemplate(
        redisConnectFactory: RedisConnectionFactory,
    ): StringRedisTemplate = StringRedisTemplate(redisConnectFactory)
}
```

> `StringRedisTemplate`을 별도로 만드는 이유: 기존 `redisTemplate`의 `hashValueSerializer`가
> `GenericJackson2JsonRedisSerializer`라서 Stream entry에 타입 정보가 들어간다.
> Connection은 동일한 멀티플렉싱 커넥션을 공유한다.

### 테스트 환경

- 로컬 VPN을 통해 개발 환경 Redis(Amazon ElastiCache Valkey 7.2.6)에 연결
- `redis.yml` 로컬 프로필: `127.0.0.1:40198` (VPN 경유 원격 Redis)
- 원격 서버 특성상 기본 RTT가 ~80ms 수준 (로컬 Redis 대비 높음)

---

## 테스트 시나리오

### 시나리오 1: 진단 + Container 1개 + Subscription 1개

**목적**: Stream 명령 호환성 진단 후, 가장 단순한 구성에서 기본 동작 확인

```kotlin
package com.musinsapayments.prepay.application.prepay.admin.campaign.infrastructure.redis.stream

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

@SpringBootTest
@Import(StreamMultiplexingTestConfig::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SingleSubscriptionTest(
    @Autowired private val redisConnectFactory: RedisConnectionFactory,
    @Autowired private val testStreamRedisTemplate: StringRedisTemplate,
) {
    private val streamKey = "test:multiplexing:stream"
    private val consumerGroup = "test-group"

    @BeforeEach
    fun setup() {
        try {
            testStreamRedisTemplate.opsForStream<String, String>()
                .createGroup(streamKey, ReadOffset.from("0"), consumerGroup)
        } catch (e: RedisSystemException) {
            if (e.cause?.message?.contains("BUSYGROUP") != true) throw e
        }
    }

    @AfterEach
    fun cleanup() {
        testStreamRedisTemplate.delete(streamKey)
    }

    @Test
    @Order(1)
    fun `진단 - XADD, XREADGROUP 직접 호출`() {
        val streamOps = testStreamRedisTemplate.opsForStream<String, String>()
        val results = mutableListOf<String>()

        // Step 1: ConnectionFactory 타입 확인
        results.add("ConnectionFactory 타입: ${redisConnectFactory.javaClass.name}")

        // Step 2: XADD
        try {
            val recordId = streamOps.add(
                StreamRecords.string(mapOf("diag" to "1")).withStreamKey(streamKey),
            )
            results.add("✅ XADD 성공: $recordId")
        } catch (e: Exception) {
            results.add("❌ XADD 실패: ${e.message}")
        }

        // Step 3: XLEN
        try {
            val len = streamOps.size(streamKey)
            results.add("✅ XLEN 결과: $len")
        } catch (e: Exception) {
            results.add("❌ XLEN 실패: ${e.message}")
        }

        // Step 4: XREADGROUP (non-block)
        try {
            val messages = streamOps.read(
                Consumer.from(consumerGroup, "diag-consumer"),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            )
            results.add("✅ XREADGROUP (non-block) 수신: ${messages?.size}건")
        } catch (e: Exception) {
            results.add("❌ XREADGROUP (non-block) 실패: ${e.cause?.message ?: e.message}")
        }

        // Step 5: XREADGROUP BLOCK 100ms
        try {
            streamOps.read(
                Consumer.from(consumerGroup, "diag-consumer-block"),
                StreamReadOptions.empty().count(10).block(Duration.ofMillis(100)),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            )
            results.add("✅ XREADGROUP BLOCK 성공")
        } catch (e: Exception) {
            results.add("❌ XREADGROUP BLOCK 실패: ${e.cause?.message ?: e.message}")
        }

        // 전체 결과 출력
        println("\n═══ 진단 결과 ═══")
        results.forEach { println(it) }
        println("═════════════════\n")

        assertThat(results.any { it.contains("XADD 성공") })
            .withFailMessage("XADD가 실패하면 Stream 자체를 사용할 수 없음")
            .isTrue()
    }

    @Test
    @Order(2)
    fun `Non-blocking polling으로 메시지 수신`() {
        // pollTimeout=ZERO → XREADGROUP에 BLOCK 파라미터 없이 non-blocking polling
        val options = StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ZERO)
            .batchSize(10)
            .build()

        val container = StreamMessageListenerContainer
            .create(redisConnectFactory, options)

        val received = CopyOnWriteArrayList<MapRecord<String, String, String>>()

        container.receive(
            Consumer.from(consumerGroup, "consumer-0"),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            StreamListener { message -> received.add(message) },
        )

        container.start()

        // 메시지 10건 발행
        val streamOps = testStreamRedisTemplate.opsForStream<String, String>()
        repeat(10) { i ->
            streamOps.add(
                StreamRecords.string(mapOf("index" to "$i")).withStreamKey(streamKey),
            )
        }

        // non-blocking polling이므로 빠르게 수신되어야 함
        Thread.sleep(3000)

        println("═══ Non-blocking polling 결과 ═══")
        println("수신된 메시지: ${received.size}건 / 10건")
        println("═════════════════════════════════")

        assertThat(received).hasSize(10)

        container.stop()
    }

    @Test
    @Order(3)
    fun `Non-blocking polling 중 다른 Redis 명령 응답시간`() {
        val options = StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ZERO)
            .batchSize(10)
            .build()

        val container = StreamMessageListenerContainer
            .create(redisConnectFactory, options)

        container.receive(
            Consumer.from(consumerGroup, "consumer-0"),
            StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            StreamListener { /* no-op */ },
        )

        container.start()

        // Container가 polling을 시작할 시간
        Thread.sleep(500)

        // polling 중 다른 Redis 명령 실행 (10회 측정)
        val latencies = (1..10).map {
            val start = System.currentTimeMillis()
            testStreamRedisTemplate.opsForValue().set("test:ping", "pong")
            val value = testStreamRedisTemplate.opsForValue().get("test:ping")
            val elapsed = System.currentTimeMillis() - start
            assertThat(value).isEqualTo("pong")
            elapsed
        }

        println("═══ Non-blocking polling 중 Redis 명령 응답시간 ═══")
        println("평균: ${latencies.average().toLong()}ms")
        println("최대: ${latencies.max()}ms")
        println("최소: ${latencies.min()}ms")
        println("전체: $latencies")
        if (latencies.average() > 100) {
            println("⚠️ non-blocking polling이 다른 명령에 영향을 줌")
        } else {
            println("✅ non-blocking polling이 다른 명령에 영향 없음")
        }
        println("══════════════════════════════════════════════════")

        container.stop()
        testStreamRedisTemplate.delete("test:ping")
    }
}
```

### 시나리오 2: Container 1개 + Subscription N개

**목적**: Subscription 수를 늘렸을 때 멀티플렉싱 커넥션 경합의 한계점 확인

```kotlin
package com.musinsapayments.prepay.application.prepay.admin.campaign.infrastructure.redis.stream

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@Import(StreamMultiplexingTestConfig::class)
class MultipleSubscriptionTest(
    @Autowired private val redisConnectFactory: RedisConnectionFactory,
    @Autowired private val testStreamRedisTemplate: StringRedisTemplate,
) {
    private val streamKey = "test:multiplexing:multi:stream"
    private val consumerGroup = "test-multi-group"

    @BeforeEach
    fun setup() {
        try {
            testStreamRedisTemplate.opsForStream<String, String>()
                .createGroup(streamKey, ReadOffset.from("0"), consumerGroup)
        } catch (e: RedisSystemException) {
            if (e.cause?.message?.contains("BUSYGROUP") != true) throw e
        }
    }

    @AfterEach
    fun cleanup() {
        testStreamRedisTemplate.delete(streamKey)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 4, 8, 16, 32])
    fun `Subscription N개에서 non-blocking polling 메시지 수신 + 다른 명령 응답시간`(subscriptionCount: Int) {
        // pollTimeout=ZERO → XREADGROUP BLOCK 미사용 (ElastiCache Valkey 호환)
        val options = StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ZERO)
            .batchSize(10)
            .build()

        val container = StreamMessageListenerContainer
            .create(redisConnectFactory, options)

        val received = AtomicInteger(0)

        // N개 Subscription 등록
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
        val streamOps = testStreamRedisTemplate.opsForStream<String, String>()
        repeat(100) { i ->
            streamOps.add(
                StreamRecords.string(mapOf("index" to "$i")).withStreamKey(streamKey),
            )
        }

        // 다른 Redis 명령 응답시간 측정 (10회)
        val latencies = (1..10).map {
            val start = System.currentTimeMillis()
            testStreamRedisTemplate.opsForValue().set("test:latency", "check")
            testStreamRedisTemplate.opsForValue().get("test:latency")
            System.currentTimeMillis() - start
        }

        Thread.sleep(5000)

        println(
            """
            |═══ Subscription ${subscriptionCount}개 결과 (non-blocking polling) ═══
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
        testStreamRedisTemplate.delete("test:latency")
    }
}
```

### 시나리오 3: Stream 소비 + 기존 Redis 기능 동시 사용

**목적**: Stream Container가 동작하는 동안 기존 기능(Session, 캐시 등)에 영향을 주는지 확인

> 메시지 발행 건수는 100건으로 설정한다. 32개 non-blocking Subscription이 tight loop으로
> polling하는 상태에서 1000건을 발행하면 단일 멀티플렉싱 커넥션 과부하로 XADD 자체가 실패한다.
> 발행 시 에러 핸들링을 추가하여 실패 건수도 함께 기록한다.

```kotlin
package com.musinsapayments.prepay.application.prepay.admin.campaign.infrastructure.redis.stream

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions
import org.junit.jupiter.api.Test
import java.time.Duration

@SpringBootTest
@Import(StreamMultiplexingTestConfig::class)
class CoexistenceTest(
    @Autowired private val redisConnectFactory: RedisConnectionFactory,
    @Autowired private val testStreamRedisTemplate: StringRedisTemplate,
    @Autowired private val redisTemplate: RedisTemplate<String, String>,
) {
    private val streamKey = "test:coexistence:stream"
    private val consumerGroup = "test-coexist-group"

    @Test
    fun `Stream 32개 Subscription non-blocking polling 중 기존 RedisTemplate 성능 측정`() {
        // setup
        try {
            testStreamRedisTemplate.opsForStream<String, String>()
                .createGroup(streamKey, ReadOffset.from("0"), consumerGroup)
        } catch (e: RedisSystemException) {
            if (e.cause?.message?.contains("BUSYGROUP") != true) throw e
        }

        // ── 기준값: Stream 없이 기존 Redis 응답시간 ──
        val baselineLatencies = measureRedisLatency(redisTemplate, 50)
        println("Baseline (Stream 없음): avg=${baselineLatencies.average().toLong()}ms, max=${baselineLatencies.max()}ms")

        // ── Stream Container 시작 (32개 Subscription, non-blocking polling) ──
        val options = StreamMessageListenerContainerOptions.builder()
            .pollTimeout(Duration.ZERO)
            .batchSize(10)
            .build()

        val container = StreamMessageListenerContainer
            .create(redisConnectFactory, options)

        repeat(32) { i ->
            container.receive(
                Consumer.from(consumerGroup, "consumer-$i"),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
                StreamListener { Thread.sleep(50) },
            )
        }
        container.start()

        // Stream에 메시지 발행 (Consumer들이 처리 중인 상태 만들기)
        val streamOps = testStreamRedisTemplate.opsForStream<String, String>()
        var publishedCount = 0
        var publishFailCount = 0
        repeat(100) { i ->
            try {
                streamOps.add(
                    StreamRecords.string(mapOf("index" to "$i")).withStreamKey(streamKey),
                )
                publishedCount++
            } catch (e: Exception) {
                publishFailCount++
            }
        }
        println("메시지 발행: 성공=${publishedCount}건, 실패=${publishFailCount}건")

        Thread.sleep(3000)

        // ── Stream 동작 중 기존 Redis 응답시간 ──
        val withStreamLatencies = measureRedisLatency(redisTemplate, 50)
        println("With Stream (32 subs, non-blocking): avg=${withStreamLatencies.average().toLong()}ms, max=${withStreamLatencies.max()}ms")

        // ── 비교 ──
        val baselineAvg = baselineLatencies.average()
        val withStreamAvg = withStreamLatencies.average()
        val degradation = if (baselineAvg > 0) withStreamAvg / baselineAvg else 0.0
        println(
            """
            |═══ 공존 테스트 결과 (non-blocking polling) ═══
            |Baseline 평균: ${baselineAvg.toLong()}ms
            |With Stream 평균: ${withStreamAvg.toLong()}ms
            |성능 저하 비율: ${String.format("%.1f", degradation)}x
            |
            |판정: ${if (degradation > 3.0) "⚠️ 유의미한 성능 저하" else "✅ 허용 범위"}
            |══════════════════════════════════════════════
            """.trimMargin(),
        )

        container.stop()
        testStreamRedisTemplate.delete(streamKey)
    }

    private fun measureRedisLatency(template: RedisTemplate<String, String>, count: Int): List<Long> {
        return (1..count).map {
            val start = System.currentTimeMillis()
            template.opsForValue().set("test:latency:$it", "value")
            template.opsForValue().get("test:latency:$it")
            template.delete("test:latency:$it")
            System.currentTimeMillis() - start
        }
    }
}
```

---

## 확인 포인트

| # | 확인 항목 | 예상 (문제 발생 시) | 예상 (문제 없을 시) |
|---|---------|-------------------|-------------------|
| 1 | `XREADGROUP BLOCK` 호환성 | ElastiCache 엔진에서 거부 | 정상 동작 |
| 2 | Non-blocking polling 메시지 수신 | 일부 메시지 미수신 | 100% 수신 |
| 3 | Subscription 수 증가 시 다른 명령 응답시간 | 선형 증가 (커넥션 경합) | baseline 대비 < 3x |
| 4 | 기존 Redis 기능 성능 저하 | SET/GET 응답시간 수 초 | baseline 대비 < 3x |

> 테스트 환경이 VPN 경유 원격 Redis이므로 기본 RTT가 ~80ms 수준이다.
> 로컬 Redis 기준 "< 10ms" 같은 기대값은 적용되지 않는다.

---

## 결과에 따른 분기

### Case A: 문제 없음

멀티플렉싱 단일 커넥션으로도 Stream이 정상 동작하는 경우:

- **별도 ConnectionFactory/Pool 불필요** → 설계 대폭 단순화
- `RedisStreamConfig` 제거, 기존 `redisConnectFactory` 그대로 사용
- `PromotionStreamManager.createConnectionFactory()` 제거
- `PromotionConsumerContext`에서 `connectionFactory` 필드 제거

### Case B: 소수 Subscription에서는 문제 없음, 다수에서 문제 발생

특정 Subscription 수(예: 8개 이상)부터 성능 저하가 발생하는 경우:

- Consumer 수 상한을 문제 없는 범위로 제한
- 또는 공용 Pool은 기존 커넥션 사용, Container 전용만 Pool 생성

### Case C: 문제 있음 (1개 Subscription부터)

BLOCK이 단일 커넥션을 점유하여 다른 명령이 블로킹되는 경우:

- 기존 설계대로 **별도 ConnectionFactory + Pool 필수**
- 초기 설계를 그대로 진행 (이후 Phase 1에서 non-blocking + 공유 ConnectionFactory로 결정)

### Case D: XREADGROUP BLOCK 자체가 동작하지 않음

ElastiCache 엔진이 Lettuce `StaticMasterReplicaConnection` 경유 `XREADGROUP BLOCK`을 거부하는 경우:

- non-blocking polling(`pollTimeout(Duration.ZERO)`)으로 우회 가능 여부 확인
- non-blocking polling 시 커넥션 경합 및 성능 영향 측정
- 결과에 따라 별도 ConnectionFactory(`StandaloneConfiguration`) 필요 여부 판단

---

## 실행 방법

```bash
# 로컬 VPN으로 개발 Redis 연결 후
./gradlew test --tests "*SingleSubscriptionTest"
./gradlew test --tests "*MultipleSubscriptionTest"
./gradlew test --tests "*CoexistenceTest"
```

> 테스트는 실제 Redis 인스턴스가 필요하다 (`@SpringBootTest`).
> Embedded Redis 또는 Testcontainers 사용 시 실제 ElastiCache와 동작이 다를 수 있으므로
> dev 환경 Redis에 연결하여 테스트하는 것을 권장한다.
