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
