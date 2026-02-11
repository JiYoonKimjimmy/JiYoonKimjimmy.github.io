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
