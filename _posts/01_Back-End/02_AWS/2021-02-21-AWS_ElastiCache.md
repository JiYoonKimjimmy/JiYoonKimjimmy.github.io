---
layout: post
title : AWS ElastiCache for Redis
date  : 2021-02-21
image : aws_redis.png
tags  : back-end AWS
---

## Spring Boot + AWS ElastiCache for Redis

### AWS ElastiCache for Redis Cluster 생성
자세한 방법은 [AWS ElastiCache 가이드 참고](https://docs.aws.amazon.com/ko_kr/AmazonElastiCache/latest/red-ug/GettingStarted.CreateCluster.html).
#### 요약
1. VPC Subnet Group 생성
2. Redis Cache Cluster 생성
3. 보안 그룹 설정에 Redis 관련 Port 추가

### Spring Boot + Redis 연동 설정
#### Redis 관련 의존성 추가
```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
compile group: 'redis.clients', name: 'jedis', version: '3.3.0'
```
#### application.yml 수정
```yml
spring:
  redis:
    host: [AWS ElastiCache End-Point Host url]
    port: 6379
```

#### Redis Config 추가
```java
@RequiredArgsConstructor
@EnableRedisRepositories
@Configuration
public class RedisConfiguration {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new JedisConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean(value = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        redisTemplate.setConnectionFactory(redisConnectionFactory);

        return redisTemplate;
    }

    @Primary
    @Bean(name = "cacheManager")
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        /**
         * Cache 기본 설정
         */
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()                                 // Null Value 는 Cache 사용하지 않음
                .entryTtl(Duration.ofSeconds(CacheKey.DEFAULT_EXPIRE_SEC))  // Cache 의 기본 유효시간 설정(60sec)
                .computePrefixWith(CacheKeyPrefix.simple())                 // Cache Key 의 Prefix 설정(name + "::")
                // Redis Cache 데이터 저장방식을 StringSerializer 로 설정
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        /**
         * Cache 상세 설정
         * - Cache default expire 시간 설정
         */
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CacheKey.USER, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(CacheKey.USER_EXPIRE_SEC)));
        cacheConfigurations.put(CacheKey.BOARD, RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(CacheKey.BOARD_EXPIRE_SEC)));

        return RedisCacheManager.RedisCacheManagerBuilder
                .fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(configuration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();

    }

}
```

### Entity Redis 설정
#### Caching 객체 Serializable
##### Caching 객체 Serializable 하는 이유
* `Redis`에 객체를 저장하면 내부적으로 직렬화하여 저장
* `Entity`에 `Serializable`을 선언하지 않으면 오류 발생 가능
```java
@Entity
public class Board implements Serializable {
  ...
}
```
#### Lazy Loading False
##### Lazy Loading false 처리 하는 이유
* Entity 객체내에서 연관관계 Mapping에 의해 Lazy(지연) Loading 되는 경우 오류 발생 가능
```java
@Proxy(lazy = false)
public class User implements Serializable {
  ...
}
```

### CRUD Methods Caching 처리
**Caching 처리 관련 Annotation**
| Annotation | 설명 |
| :---: | --- |
| `@Cacheable` | `Cache`가 존재하면 요청된 `Method`를 실행하지 않고 `Cache`데이터를 반환 처리 |
| `@CachePut` | `Cache`에 데이터를 넣거나 수정시 사용. `Method`의 반환값이 `Cache`에 없으면 저장하고, 있는 경우엔 갱신 처리 |
| `@CacheEvict` | `Cache` 삭제 |
| `@Caching` | 여러개의 `Cache Annotation`을 실행되어야 할 때 사용 |

##### Annotation 옵션 설명
* `value = CacheKey.BOARD` : 저장시 사용되는 Key Name
* `key = "#id"` : `value` 옵션에서 선언된 Key Name 과 결합 ***(ex. 'board::1')***
* `unless = "#result == null"` : 결과가 `null` 이 아닌 경우만 Caching 처리
* `condition = "#id > 10"` : 간단한 조건문에 따라 Caching 처리

### Cache 처리의 또다른 방법. CacheService 추가
* `Caching` 처리해주는 Service
* 왜 `CacheService`가 필요한가?
  * 요청받은 `Method`의 인자값으로 `Cache Key`를 조합할 수 없을 때가 있기 때문
  * `Proxy`의 특성상 같은 객체내에서는 `Caching`처리된 `Method` 호출시 동작하지 않기 때문

### Custom Key Generator
#### CustomKeyGenerator.java
```java
public class CustomKeyGenerator {
    public static Object create(Object o1) {
        return "FRONT:" + o1;
    }
    public static Object create(Object o1, Object o2) {
        return "FRONT:" + o1 + ":" + o2;
    }
}
```

#### CustomKeyGenerator 이용한 Caching
```java
@Cacheable(value = CacheKey.BOARD, key = "T(com.demo.restapi.config.redis.CustomKeyGenerator).create(#id)", unless = "#result == null")
```

---

> **Reference**
> [keyholesoftware.com [Using Amazon ElastiCache For Redis]](https://keyholesoftware.com/2018/08/28/using-amazon-elasticache-for-redis-to-optimize-your-spring-boot-application/)
