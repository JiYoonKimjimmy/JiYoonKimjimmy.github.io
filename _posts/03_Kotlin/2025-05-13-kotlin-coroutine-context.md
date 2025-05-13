---
layout: post
title : Coroutine Context 대한 이해 (feat. Redisson)
date  : 2025-05-13
image : kotlin_coroutines.png
tags  : kotlin coroutine coroutinecontext redisson
---

## Kotlin 코루틴 Trouble-Shooting (with Redisson 분산락)

### Situation

- `Redisson` 분산락을 사용하는 `lock` 함수에서, `block()` 람다 함수 실행 후에 락이 정상적으로 해제(unlock)되지 않는 현상 발생

```
-- 1. 정상적으로 `unlock` 되는 경우
2025-05-13 13:04:00.052.DefaultDispatcher-worker-1 @coroutine#1> INFO  T[] U[] M[] - Redisson 'ktc:202505131304:expire-traffic-token-schedule-lock' locked.
2025-05-13 13:04:00.122.DefaultDispatcher-worker-1 @coroutine#1> INFO  T[] U[] M[] - Expired Traffic Token count : 0
2025-05-13 13:04:00.131.DefaultDispatcher-worker-1 @coroutine#1> INFO  T[] U[] M[] - Redisson 'ktc:202505131304:expire-traffic-token-schedule-lock' unlocked.
-- 2. 비정상적으로 `unlock` 되는 경우
2025-05-13 13:05:00.008.DefaultDispatcher-worker-1 @coroutine#2> INFO  T[] U[] M[] - Redisson 'ktc:202505131305:expire-traffic-token-schedule-lock' locked.
2025-05-13 13:05:00.022.DefaultDispatcher-worker-4 @coroutine#2> INFO  T[] U[] M[] - Expired Traffic Token count : 0
-- 3. 비정상적으로 `unlock` 되는 경우
2025-05-13 13:06:00.012.DefaultDispatcher-worker-4 @coroutine#3> INFO  T[] U[] M[] - Redisson 'ktc:202505131306:expire-traffic-token-schedule-lock' locked.
2025-05-13 13:06:00.025.DefaultDispatcher-worker-1 @coroutine#3> INFO  T[] U[] M[] - Expired Traffic Token count : 0
-- 4. 정상적으로 `unlock` 되는 경우
2025-05-13 13:07:00.005.DefaultDispatcher-worker-4 @coroutine#4> INFO  T[] U[] M[] - Redisson 'ktc:202505131307:expire-traffic-token-schedule-lock' locked.
2025-05-13 13:07:00.018.DefaultDispatcher-worker-4 @coroutine#4> INFO  T[] U[] M[] - Expired Traffic Token count : 0
2025-05-13 13:07:00.020.DefaultDispatcher-worker-4 @coroutine#4> INFO  T[] U[] M[] - Redisson 'ktc:202505131307:expire-traffic-token-schedule-lock' unlocked.
```

#### 문제의 코드

```kotlin
override suspend fun <R> lock(
    key: String,
    waitTime: Long,
    leaseTime: Long,
    timeUnit: TimeUnit,
    block: suspend () -> R
): R {
    val lock = redissonClient.getLock(key)
    return try {
        if (lock.tryLock(waitTime, leaseTime, timeUnit)) {
            block()
        } else {
            throw InternalServiceException(ErrorCode.REDISSON_LOCK_ATTEMPT_ERROR)
        }
    } finally {
        if (lock.isHeldByCurrentThread) {
            lock.unlock()
        }
    }
}
```

#### 원인 분석

- `Redisson` 락은 **스레드 기반 동작 구조**
- 락을 획득한 스레드와 해제하는 스레드가 달라지면 `unlock` 실패 or `unlock` 미호출
- **Kotlin 코루틴은 suspend 함수 내에서 컨텍스트 전환이 일어나면, block 실행 시점과 unlock 시점의 스레드가 달라질 수 있음**
- 이로 인해 `lock.isHeldByCurrentThread`가 false가 되거나, `IllegalMonitorStateException` 예외 발생 가능
- 비정상적으로 `unlock` 되지 않는 경우, `Worker Thread` 가 락 획득 시점과 `block()` 함수 처리된 후 스레드가 다름

```
-- 2. 비정상적으로 `unlock` 되는 경우
2025-05-13 13:05:00.008.DefaultDispatcher-worker-1 @coroutine#2> INFO  T[] U[] M[] - Redisson 'ktc:202505131305:expire-traffic-token-schedule-lock' locked.
2025-05-13 13:05:00.022.DefaultDispatcher-worker-4 @coroutine#2> INFO  T[] U[] M[] - Expired Traffic Token count : 0
-- 3. 비정상적으로 `unlock` 되는 경우
2025-05-13 13:06:00.012.DefaultDispatcher-worker-4 @coroutine#3> INFO  T[] U[] M[] - Redisson 'ktc:202505131306:expire-traffic-token-schedule-lock' locked.
2025-05-13 13:06:00.025.DefaultDispatcher-worker-1 @coroutine#3> INFO  T[] U[] M[] - Expired Traffic Token count : 0
```

---

### Solution

- **`lock()` ~ `unlock()` 락 획득부터 해제까지 같은 스레드에서 실행 필요**
- 코루틴 컨텍스트를 `withContext(Dispatchers.IO)` 등으로 고정하여, 같은 코루틴 컨텍스트에서 실행되도록 코드 수정

```kotlin
override suspend fun <R> lock(
    key: String,
    waitTime: Long,
    leaseTime: Long,
    timeUnit: TimeUnit,
    block: suspend () -> R
): R = withContext(Dispatchers.IO) {
    val lock = redissonClient.getLock(key)
    try {
        if (lock.tryLock(waitTime, leaseTime, timeUnit)) {
            block()
        } else {
            throw InternalServiceException(ErrorCode.REDISSON_LOCK_ATTEMPT_ERROR)
        }
    } finally {
        if (lock.isHeldByCurrentThread) {
            lock.unlock()
        }
    }
}
```

```
2025-05-13 13:19:00.052.DefaultDispatcher-worker-2 @coroutine#1> INFO  T[] U[] M[] - Redisson 'ktc:202505131319:expire-traffic-token-schedule-lock' locked.
2025-05-13 13:19:00.113.DefaultDispatcher-worker-2 @coroutine#1> INFO  T[] U[] M[] - Expired Traffic Token count : 0
2025-05-13 13:19:00.121.DefaultDispatcher-worker-2 @coroutine#1> INFO  T[] U[] M[] - Redisson 'ktc:202505131319:expire-traffic-token-schedule-lock' unlocked.

2025-05-13 13:20:00.022.DefaultDispatcher-worker-3 @coroutine#2> INFO  T[] U[] M[] - Redisson 'ktc:202505131320:expire-traffic-token-schedule-lock' locked.
2025-05-13 13:20:00.036.DefaultDispatcher-worker-3 @coroutine#2> INFO  T[] U[] M[] - Expired Traffic Token count : 0
2025-05-13 13:20:00.039.DefaultDispatcher-worker-3 @coroutine#2> INFO  T[] U[] M[] - Redisson 'ktc:202505131320:expire-traffic-token-schedule-lock' unlocked.

2025-05-13 13:21:00.018.DefaultDispatcher-worker-1 @coroutine#3> INFO  T[] U[] M[] - Redisson 'ktc:202505131321:expire-traffic-token-schedule-lock' locked.
2025-05-13 13:21:00.030.DefaultDispatcher-worker-1 @coroutine#3> INFO  T[] U[] M[] - Expired Traffic Token count : 0
2025-05-13 13:21:00.032.DefaultDispatcher-worker-1 @coroutine#3> INFO  T[] U[] M[] - Redisson 'ktc:202505131321:expire-traffic-token-schedule-lock' unlocked.
```

---

### Conclusion

- Redisson 분산락과 같은 **동일한 Thread 기반 컨텍스트 유지**가 필요한 로직애소 코루틴 사용시 주의 필요
- 코루틴 환경에서 같은 스레드에서 실행되도록 `withContext(Dispatchers.IO)` 로 컨텍스트 구분하도록 설정 필요

---
