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
