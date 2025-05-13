---
layout: post
title : Spring Data Redis's CacheErrorHandler
date  : 2025-05-08
image : spring-data-redis.png
tags  : redis springdataredis cacheerrorhandler
---

## Spring Data Redis 예외 처리

### Situation

- Spring Data Redis 의 `@Cacheable` 애노테이션 적용한 캐싱 패턴 중 Redis 인스턴스의 지연 이슈로 `RedisCommandTimeoutException` 발생
- Redis 명령어 요청 후 `spring.redis.timeout: 500` 설정을 `2000(ms)` 조절하였지만, 추가적인 방어 코드를 통해 `RedisCommandTimeoutException` 에 대해서는 예외 무시 처리

```
Redis command timed out; nested exception is io.lettuce.core.RedisCommandTimeoutException: Command timed out after 500 millisecond(s).
```

> **Redis Timeout 설정 변경**
>
> ```properties
> # Properties
> spring.redis.timeout: 2000
> 
> # YAML
> spring:
>     redis:
>         timeout: 2000
> ```

---

### Solution

#### CacheErrorHandler 인터페이스

- Spring Data Redis 는 에러 핸들링을 위한 `CacheErrorHandler` 인터페이스를 제공
- 기본적으로 `SimpleCacheErrorHandler` 구현체를 통해 예외 발생하는 경우, 모두 `throw exception;` 통해 예외 발생 처리

```java
public class SimpleCacheErrorHandler implements CacheErrorHandler {

    @Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		throw exception;
	}

    // ... code ...
}
```

#### CustomCacheErrorHandler 클래스 구현

- `SimpleCacheErrorHandler` 처럼 `CacheErrorHandler` 인터페이스를 상속받아 모든 인터페이스 함수에 대해 구현 가능
- 모든 기능에 대한 자체 처리가 필요하지 않다면, `SimpleCacheErrorHandler` 를 상속받아 필요한 함수만 오버라이딩하여 구현 가능

```java
public class CustomCacheErrorHandler extends SimpleCacheErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(CustomCacheErrorHandler.class);

    @Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        if (exception instanceof RedisCommandTimeoutException) {
            logger.warn("Redis timeout during cache GET for key {}: {}", key, exception.getMessage());
        } else {
            throw exception;
        }
	}

}
```

##### CustomCacheErrorHandler 클래스 적용

- `CustomCacheErrorHandler` 클래스는 `RedisCacheManger` 에서 활용하기 위해서는 등록 필요
- `CachingConfigurerSupport` 클래스 상속을 통한 `errorHandler` 등록 처리

```java
@Configuration
public class RedisConfig extends CachingConfigurerSupport {
    // ... code ...

    @Override
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }
}
```

---

### Test Code

- 테스트를 위해 `RedisCommandTimeoutException` 예외를 강제로 발생하기 위해서는, `RedisCacheManager` 내 일부 함수 Mocking 처리 필요
  - Spring Data Redis 의 `@Cacheable` 은 `AOP` 기반으로 처리되는 캐싱 전략으로 캐시 처리 기능을 위임받은 `RedisCacheManager` Mocking 필요

```java
@SpringBootTest
class CustomCacheErrorHandlerTest {

    @Autowired
    private MyCacheService cacheService;

    @SpyBean
    private RedisCacheManager redisCacheManager;

    private 

    @DisplayName("Key 기준 Cache 조회 성공 정상 확인한다")
    @Test
    void getCacheSuccessTest() {
        // given
        String key = "hello";

        // when
        List<String> result = cacheService.getCache(key);

        // then
        assertThat(result).isNotEmpty();
    }

    @DisplayName("Key 기준 Cache 조회 시 RedisCommandTimeoutException 발생하여도 DB 조회 성공 정상 확인한다")
    @Test
    void getCacheIgnoreRedisCommandTimeoutExceptionTest() {
        // given
        String key = "hello";

        // Cache mocking 처리
        String cacheName = "my:cache"   // `@Cacheable.cacheNames`
        RedisCache originalCache = (RedisCache) redisCacheManager.getCache(cacheName);
        assert cache != null;
        RedisCache spyCache = Mockito.spy(originalCache);

        // RedisCache.get() 함수 예외 발생 mocking 처리
        Mockito.doThrow(new RedisCommandTimeoutException("redis command timeout")).when(spyCache).get(Mockito.any());
        // RedisCacheManger.getCache() 함수 spyCache 반환 mocking 처리
        Mockito.doReturn(spyCache).when(redisCacheManager).getCache(cacheName);

        // when
        List<String> result = cacheService.getCache(key);

        // then
        assertThat(result).isNotEmpty();
    }

}
```

---
