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
