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
