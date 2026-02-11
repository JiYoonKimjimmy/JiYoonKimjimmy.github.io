import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.RedisSystemException
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.connection.stream.StreamReadOptions
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@Import(StandaloneConnectionTestConfig::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class StandaloneBlockTest(
    @Autowired private val standaloneStreamContext: StandaloneConnectionTestConfig.StandaloneStreamContext,
) {
    private val connectionFactory: LettuceConnectionFactory get() = standaloneStreamContext.connectionFactory
    private val redisTemplate: StringRedisTemplate get() = standaloneStreamContext.redisTemplate

    private val streamKey = "test:standalone:stream"
    private val consumerGroup = "test-standalone-group"

    @BeforeEach
    fun setup() {
        try {
            redisTemplate.opsForStream<String, String>()
                .createGroup(streamKey, ReadOffset.from("0"), consumerGroup)
        } catch (e: RedisSystemException) {
            if (e.cause?.message?.contains("BUSYGROUP") != true) throw e
        }
    }

    @AfterEach
    fun cleanup() {
        redisTemplate.delete(streamKey)
    }

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

        // Step 5: MONITOR로 Lettuce XREADGROUP BLOCK 명령 캡처
        // 별도 커넥션에서 MONITOR 시작 → Lettuce 명령 실행 → MONITOR 결과 확인
        val monitorLogs = mutableListOf<String>()
        val monitorThread = Thread {
            try {
                connectionFactory.connection.use { monitorConn ->
                    val nativeConn = monitorConn.nativeConnection
                    if (nativeConn is io.lettuce.core.api.StatefulRedisConnection<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val syncCommands = (nativeConn as io.lettuce.core.api.StatefulRedisConnection<String, String>).sync()
                        // MONITOR 대신 DEBUG SLEEP 사용 불가, 직접 RESP로 캡처 불가
                        // 대안: Lettuce의 CommandListener 사용
                    }
                }
            } catch (e: Exception) {
                monitorLogs.add("MONITOR 실패: ${e.message}")
            }
        }

        // Step 5-1: Lettuce StreamOps XREADGROUP BLOCK
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

        // Step 5-2: 풀 커넥션에서 Lettuce 네이티브 API로 XREADGROUP BLOCK
        try {
            val nativeResult = redisTemplate.execute { connection ->
                val nativeConn = connection.nativeConnection
                results.add("  nativeConnection 타입: ${nativeConn?.javaClass?.name}")
                when (nativeConn) {
                    is io.lettuce.core.api.StatefulRedisConnection<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        val sync = (nativeConn as io.lettuce.core.api.StatefulRedisConnection<ByteArray, ByteArray>).sync()
                        val xReadArgs = io.lettuce.core.XReadArgs().count(10).block(100)
                        val xResult = sync.xreadgroup(
                            io.lettuce.core.Consumer.from(consumerGroup.toByteArray(), "diag-native".toByteArray()),
                            xReadArgs,
                            io.lettuce.core.XReadArgs.StreamOffset.lastConsumed(streamKey.toByteArray()),
                        )
                        "StatefulRedisConnection → xreadgroup BLOCK 성공: ${xResult?.size}건"
                    }
                    is io.lettuce.core.api.async.RedisAsyncCommands<*, *> -> {
                        // Pool 커넥션은 RedisAsyncCommands 타입. StatefulConnection을 직접 추출
                        @Suppress("UNCHECKED_CAST")
                        val asyncCmd = nativeConn as io.lettuce.core.api.async.RedisAsyncCommands<ByteArray, ByteArray>
                        val statefulConn = asyncCmd.statefulConnection
                        val sync = statefulConn.sync()
                        val xReadArgs = io.lettuce.core.XReadArgs().count(10).block(100)
                        val xResult = sync.xreadgroup(
                            io.lettuce.core.Consumer.from(consumerGroup.toByteArray(), "diag-native".toByteArray()),
                            xReadArgs,
                            io.lettuce.core.XReadArgs.StreamOffset.lastConsumed(streamKey.toByteArray()),
                        )
                        "RedisAsyncCommands → xreadgroup BLOCK 성공: ${xResult?.size}건"
                    }
                    else -> "nativeConnection 타입 불일치: ${nativeConn?.javaClass?.name}"
                }
            }
            results.add("XREADGROUP BLOCK (풀 커넥션 native): $nativeResult")
        } catch (e: Exception) {
            results.add("XREADGROUP BLOCK (풀 커넥션 native) 실패: ${e.cause?.message ?: e.message}")
        }

        // Step 6: CLIENT SETINFO 비활성화 RedisClient로 XREADGROUP BLOCK
        try {
            val noSetInfoClient = standaloneStreamContext.noSetInfoClient
            val conn = noSetInfoClient.connect(io.lettuce.core.codec.ByteArrayCodec.INSTANCE)
            val sync = conn.sync()
            val xReadArgs = io.lettuce.core.XReadArgs().count(10).block(100)
            val result = sync.xreadgroup(
                io.lettuce.core.Consumer.from(consumerGroup.toByteArray(), "diag-nosetinfo".toByteArray()),
                xReadArgs,
                io.lettuce.core.XReadArgs.StreamOffset.lastConsumed(streamKey.toByteArray()),
            )
            results.add("XREADGROUP BLOCK (no CLIENT SETINFO) 성공: ${result?.size}건")
            conn.close()
        } catch (e: Exception) {
            results.add("XREADGROUP BLOCK (no CLIENT SETINFO) 실패: ${e.cause?.message ?: e.message}")
        }

        // Step 6-1: 새 커넥션 SET/GET 워밍업 후 XREADGROUP BLOCK
        try {
            val noSetInfoClient = standaloneStreamContext.noSetInfoClient
            val conn = noSetInfoClient.connect(io.lettuce.core.codec.ByteArrayCodec.INSTANCE)
            val sync = conn.sync()

            // 워밍업: SET/GET (비-Stream 명령)
            sync.set("test:warmup".toByteArray(), "ok".toByteArray())
            sync.get("test:warmup".toByteArray())

            val xReadArgs = io.lettuce.core.XReadArgs().count(10).block(100)
            val result = sync.xreadgroup(
                io.lettuce.core.Consumer.from(consumerGroup.toByteArray(), "diag-warmup-setget".toByteArray()),
                xReadArgs,
                io.lettuce.core.XReadArgs.StreamOffset.lastConsumed(streamKey.toByteArray()),
            )
            results.add("XREADGROUP BLOCK (SET/GET 워밍업 후) 성공: ${result?.size}건")
            conn.close()
        } catch (e: Exception) {
            results.add("XREADGROUP BLOCK (SET/GET 워밍업 후) 실패: ${e.cause?.message ?: e.message}")
        }

        // Step 6-2: 새 커넥션 Stream 워밍업(XREADGROUP non-block) 후 XREADGROUP BLOCK
        try {
            val noSetInfoClient = standaloneStreamContext.noSetInfoClient
            val conn = noSetInfoClient.connect(io.lettuce.core.codec.ByteArrayCodec.INSTANCE)
            val sync = conn.sync()

            // 워밍업: XREADGROUP non-block (Stream 명령)
            sync.xreadgroup(
                io.lettuce.core.Consumer.from(consumerGroup.toByteArray(), "diag-warmup-stream".toByteArray()),
                io.lettuce.core.XReadArgs().count(1),
                io.lettuce.core.XReadArgs.StreamOffset.lastConsumed(streamKey.toByteArray()),
            )

            val xReadArgs = io.lettuce.core.XReadArgs().count(10).block(100)
            val result = sync.xreadgroup(
                io.lettuce.core.Consumer.from(consumerGroup.toByteArray(), "diag-warmup-stream".toByteArray()),
                xReadArgs,
                io.lettuce.core.XReadArgs.StreamOffset.lastConsumed(streamKey.toByteArray()),
            )
            results.add("XREADGROUP BLOCK (Stream 워밍업 후) 성공: ${result?.size}건")
            conn.close()
        } catch (e: Exception) {
            results.add("XREADGROUP BLOCK (Stream 워밍업 후) 실패: ${e.cause?.message ?: e.message}")
        }

        // Step 7: 메시지 추가 후 XREADGROUP BLOCK - raw execute로 수신 검증
        val rawConsumer = "diag-consumer-raw"
        streamOps.add(
            StreamRecords.string(mapOf("raw-test" to "1")).withStreamKey(streamKey),
        )
        try {
            val rawResult = redisTemplate.execute { connection ->
                connection.execute(
                    "XREADGROUP",
                    "GROUP".toByteArray(),
                    consumerGroup.toByteArray(),
                    rawConsumer.toByteArray(),
                    "COUNT".toByteArray(),
                    "10".toByteArray(),
                    "BLOCK".toByteArray(),
                    "100".toByteArray(),
                    "STREAMS".toByteArray(),
                    streamKey.toByteArray(),
                    ">".toByteArray(),
                )
            }
            results.add("XREADGROUP BLOCK (raw execute) 성공: $rawResult (타입: ${rawResult?.javaClass?.name})")
        } catch (e: Exception) {
            results.add("XREADGROUP BLOCK (raw execute) 실패: ${e.cause?.message ?: e.message}")
        }

        // Step 7-1: raw execute로 BLOCK 없이 XREADGROUP → 비교 대조군
        streamOps.add(
            StreamRecords.string(mapOf("raw-test" to "2")).withStreamKey(streamKey),
        )
        try {
            val rawNonBlock = redisTemplate.execute { connection ->
                connection.execute(
                    "XREADGROUP",
                    "GROUP".toByteArray(),
                    consumerGroup.toByteArray(),
                    rawConsumer.toByteArray(),
                    "COUNT".toByteArray(),
                    "10".toByteArray(),
                    "STREAMS".toByteArray(),
                    streamKey.toByteArray(),
                    ">".toByteArray(),
                )
            }
            results.add("XREADGROUP non-block (raw execute) 성공: $rawNonBlock (타입: ${rawNonBlock?.javaClass?.name})")
        } catch (e: Exception) {
            results.add("XREADGROUP non-block (raw execute) 실패: ${e.cause?.message ?: e.message}")
        }

        // Step 8: Lettuce dispatch()로 raw execute와 동일한 byte[] args 구성 (dedicated connection)
        streamOps.add(
            StreamRecords.string(mapOf("dispatch-test" to "1")).withStreamKey(streamKey),
        )
        try {
            val dispatchResult = redisTemplate.execute { connection ->
                // connection.execute()와 동일한 dedicated connection 경로 사용
                val codec = io.lettuce.core.codec.ByteArrayCodec.INSTANCE
                val args = io.lettuce.core.protocol.CommandArgs(codec)
                    .add("GROUP".toByteArray())
                    .add(consumerGroup.toByteArray())
                    .add("diag-dispatch".toByteArray())
                    .add("COUNT".toByteArray())
                    .add("10".toByteArray())
                    .add("BLOCK".toByteArray())
                    .add("100".toByteArray())
                    .add("STREAMS".toByteArray())
                    .add(streamKey.toByteArray())
                    .add(">".toByteArray())

                // connection.execute()와 동일하게 raw 실행
                connection.execute(
                    "XREADGROUP",
                    "GROUP".toByteArray(),
                    consumerGroup.toByteArray(),
                    "diag-dispatch".toByteArray(),
                    "COUNT".toByteArray(),
                    "10".toByteArray(),
                    "BLOCK".toByteArray(),
                    "100".toByteArray(),
                    "STREAMS".toByteArray(),
                    streamKey.toByteArray(),
                    ">".toByteArray(),
                )
            }
            results.add("XREADGROUP BLOCK (dispatch raw args): 성공 $dispatchResult (타입: ${dispatchResult?.javaClass?.name})")
        } catch (e: Exception) {
            results.add("XREADGROUP BLOCK (dispatch raw args) 실패: ${e.cause?.message ?: e.message}")
        }

        // Step 8-1: Lettuce API 방식 args (CommandKeyword + addKey + add(long))로 비교
        streamOps.add(
            StreamRecords.string(mapOf("api-test" to "1")).withStreamKey(streamKey),
        )
        try {
            val apiStyleResult = redisTemplate.execute { connection ->
                // Lettuce API가 내부적으로 하는 것과 동일한 방식 - 하지만 connection.execute()로 실행
                // COUNT와 BLOCK 값을 정수가 아닌 문자열로 전달하면 어떤 차이가 있는지 확인
                connection.execute(
                    "XREADGROUP",
                    "GROUP".toByteArray(),
                    consumerGroup.toByteArray(),
                    "diag-api-style".toByteArray(),
                    "COUNT".toByteArray(),
                    "10".toByteArray(),
                    "BLOCK".toByteArray(),
                    "100".toByteArray(),
                    "STREAMS".toByteArray(),
                    streamKey.toByteArray(),
                    ">".toByteArray(),
                )
            }
            results.add("XREADGROUP BLOCK (api-style raw): 성공 $apiStyleResult")
        } catch (e: Exception) {
            results.add("XREADGROUP BLOCK (api-style raw) 실패: ${e.cause?.message ?: e.message}")
        }

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
}
