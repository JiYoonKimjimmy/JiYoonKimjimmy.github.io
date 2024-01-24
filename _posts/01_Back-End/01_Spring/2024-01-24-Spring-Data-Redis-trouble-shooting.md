---
layout: post
title : Spring Data Redis 의 `Phantom` & `Secondary Indexing`
date  : 2024-01-24
image : spring-data-redis.png
tags  : redis springdataredis phantom secondaryindexing
---

## Spring Data Redis 사용 후기

### 👉 `:phantom` Cache Key 에 대해서.. 👈
- Spring Data Redis 자체적으로 저장된 Cache Key 정보가 `Expiry` 만료가 되는 시점에 별도의 `Event` 이벤트를 받기 위해 `{cache key}:phantom` 방식으로 복사본을 저장한다.
- `{cache key}` 정보가 만료되는 시점에 `{cache key}:phantom` 를 통해서 Application 에 정의된 Event 처리를 수행할 수 있게 된다.

```java
@EnableRedisRepositories(enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP)
```

- 위 설정을 통해서 `Keysapce` Event 에 대한 처리를 Application 에서 수행하게 된다.
    - **‼ 해당 설정이 없다면, Key 가 만료되더라도 `:phantom` 를 포함한 다른 Cache 정보가 삭제되지 않는다 ‼**

### 👉 Secondary Indexing 에 대해서.. 👈
- **Secondary Indexing** : `key` 조건으로 검색 외의 `Index` 로 지정한 정보에 대해서 검색할 수 있도록 새로운 `SET` Cache 정보를 저장한다.
- `Redis` 의 `Secondary Indexing` 처리를 위해 `SpringDataRedis` 에서는 `@Indexed` Annotation 을 통해서 간단하게 구현이 가능하다.

```java
@RedisHash(value = "blacklist")
data class BlacklistUserCacheModel(
    @Id
    val id: String,
    val aspId: String,
    @Indexed
    val hashId: String? = null,
    @TimeToLive(unit = TimeUnit.SECONDS)
    val ttl: Long? = 604800
)
```

![Redis Secondary Indexing](/images/redis_secondary_indexing.png)

- `blacklist:01012341234:000177000000000` Cache Key 로 `hashId` 저장
- `blacklist:hashId:6D173B65DA1943BF369FB590A3F4D1D5EBCB12E14164CAF13AB4F43C1DFE8B58` Cache Key 로 원본 Cache 정보를 `Index` 처리하여 새로운 Cache 정보로 저장

#### ⁉ `Secondary Indexing` 설정할 때 **주의 사항** ⁉
##### Keyspace 설정 확인 ❗
- `BlacklistUserCacheModel` 클래스의 `@RedisHash` 를 보면 `blacklist` 란 키워드로 Keyspace 를 설정한 것을 확인할 수 있다.
    - 보통 Redis 에서 Key 생성 시, `:` 구분자로 Keyspace 를 구성한다.
- Keyspace 가 만약 `blacklist` 가 아닌 `map:blacklist` 와 같이 `:` 가 들어가면, **만료 Event 발생하였을 때 Event 처리가 되지 않을 수 있다.**
    - `RedisKeyExpiredEvent` 클래스에서 Event 를 수신하여 처리하지만, 설정된 Keyspace 의 문자열에서 `:` 로 잘라서 Key 만료 처리를 한다.
    - ❕ 이 때, 잘린 Keyspace 명이 일치하지 않아 정상적으로 `Index` Cache 정보가 삭제되지 않게 된다 ❕

---
