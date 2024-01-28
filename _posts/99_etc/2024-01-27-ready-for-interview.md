---
layout: post
title : Ready for Interview
date  : 2024-01-27
image : development-knowledge-note.png
tags  : interview
---

## Back-End 개발자 인터뷰를 위한 준비

알고 있는 것도 헷갈리고, 서툰 신입이 되는 순간이 인터뷰를 진행하는 순간인 것 같다.
항상 처음 사회 생활을 시작하는 마음으로 준비된 모습을 보여주기 위해서는 아는 것도 기록하고, 복기하고, 정리하는 것이 중요하다고 생각이 들었다.

개발자로서 CS부터 실무 개발 프레임워크까지 너무 알아야할 지식들이 많지만, 정리를 하다보면 남들만큼 알 수 있지 않을까 하는 희망을 가져본다.

---

### Items

1. Java `Thread-Pool` 관리
2. Java 동시성 처리 패턴
3. Java 병렬 처리 패턴
4. `CQRS` 개념에 대해서
5. 데이터베이스 `Isolation Level` 에 대해서
6. Spring F/W 의 `Bean` 생성 기본 패턴이 `Single-ton` 인 이유
7. `JWT` Token 인증
8. `OAuth` 인증
9. Kotlin 최신 버전 확인
10. Spring F/W & Spring Boot 최신 버전 확인
11. 시스템 아키텍처 디자인

---

#### Java `Thread-Pool` 관리

먼저, `Thread` 는 컴퓨터 `CPU` 에서 작업을 처리하는 프로세스의 일부이며, 프로세스 내에 독립적으로 실행되는 작업 단위이다. `Thread` 는 각각 레지스터, 스택, 스레드풀 등 보유한다.
`Thread` 는 프로세스와 동일한 메모리를 사용하지만, 동시 실행 가능한 특성을 가지고 있기 때문에 성능 측면에서 더 우수하다.

하지만, 동시 실행 가능한 특성 때문에 서로 다른 `Thread` 가 동시에 동일한 자원에 접근하여 발생하는 **동시성 문제**가 있다. 해당 문제를 해결하기 위해 `Thread` 간 동기화를 위해
`synchronized` 코드 블록을 활용하거나 `Lock 락` 객체 이용 등 다양한 방식으로 처리가 필요하다.

Java 에서 `Thread-Pool` 관리하기 위해서는 `ExecutorService` 인터페이스와 `ThreadPoolExecutor` 클래스를 활용하여 구현할 수 있다.

##### ExecutorService 인터페이스 제공 `Thread-Pool` 종류

- `ExecutorService` 인터페이스를 활용하면 손쉽게 `Thread-Pool` 생성 가능하다.

| 종류 | 설명 |
| :---: | --- |
| `newFixedThreadPool()` | 고정된 크기의 `Thread-Pool` 생성 |
| `newSingleThreadExecutor()` | 단일 스레드의 `Thread-Pool` 생성 |
| `newCachedThreadPool()` | 필요에 따라 `Thread` 생성 및 종료하는 `Thread-Pool` 생성 |
| `newScheduledThreadPool()` | 지정된 시간에 작업을 실행하는 `Thread-Pool` 생성 |

```java
public class ThreadPoolTest {
    @Test
    void generateThreadPoolByExecutorService() {
        Executors.newFixedThreadPool(10);
        Executors.newSingleThreadExecutor();
        Executors.newCachedThreadPool() ;
        Executors.newScheduledThreadPool(10);
    }
}
```

##### ThreadPoolExecutor 클래스 활용 `Thread-Pool` 생성

- `ThreadPoolExecutor` 클래스를 활용하면 상황에 맞는 `Thread-Pool` 생성 가능하다.
- `Thread-Pool` 생성을 위한 다양한 옵션을 제공한다.

| 옵션 | 설명 |
| :---: | --- |
| `corePoolSize` | 기본적으로 실행 중인 `Thread` 의 수 |
| `maximumPoolSize` | 최대로 실행 중인 `Thread` 의 수 |
| `keepAliveTime` | `Thread` 가 실행 중인 상태에서 대기하는 시간 |
| `unit` | `keepAliveTime` 시간 단위 |
| `workQueue` | `Thread` 가 실행될 때까지 대기하는 `Queue` |

`ThreadPoolExecutor` 클래스를 활용하여 `Thread-Pool` 생성한다면 다음과 같은 장점을 가질 수 있다.

- `Thread-Pool` 크리 조절 가능
- `Thread` 의 실행 우선 순위 지정 가능
- `Thread` 실행 중인 상태에서 대기하는 시간 조절 가능
- `Thread` 실행 대기 `Queue` 종류 지정 가능

```java
ThreadPoolExecutor executorService = new ThreadPoolExecutor(
    10,                             // corePoolSize
    20,                             // maximumPoolSize
    1000,                           // keepAliveTime
    TimeUnit.MILLISECONDS,          // unit
    new LinkedBlockingQueue<>(100)  // workQueue
);
public class ThreadPoolTest {
    @Test
    void generateThreadPoolByThreadPoolExecutor() {
        ThreadPoolExecutor executorService = new ThreadPoolExecutor(
            10,                             // corePoolSize
            20,                             // maximumPoolSize
            1000,                           // keepAliveTime
            TimeUnit.MILLISECONDS,          // unit
            new LinkedBlockingQueue<>(100)  // workQueue
        );
    }
}
```

---

### Java 동시성 & 병렬 처리 패턴

#### 동시성 이슈 처리

여러 `Thread` 는 동시에 실행될 수 있기 대문에, 한번에 **동일한 자원에 접근이 가능하다.**
하나의 `Thread` 가 특정 자원에 접근하여 변경을 하는 동안 다른 `Thread` 가 해당 자원을 다시 접근한다면, 자원에 대한 일관성이 깨질 수가 있다.

위와 같은 **동시성 이슈**를 해결하기 위한 방식에 대해 살펴보고자 한다.

##### synchronized

`synchronized` 는 Java 에서 많이 사용하는 `Thread` 동기화를 위한 방법으로, `synchronized` 가 있는 함수 또는 코드 블록은 먼저 진입한 `Thread` 만을 접근 허용한다.

```java
public class Counter {

    private int count = 0;

    public synchronized void increment() {
        count++;
    }

    public int getCount() {
        return count;
    }
}
```

위 예제에서 `count` 라는 전역 변수에 대한 접근은 `increment()` 함수에서만 허용하고, `increment()` 함수를 먼저 호출한 `Thread` 만이 접근 가능하다.

##### Lock 객체

`Lock` 객체도 `synchronized` 키워드와 비슷하게 먼저 `Lock` 객체를 선점하는 `Thread` 만을 접근 허용한다.

```java
public class Counter {

    private int count = 0;

    private Lock lock = new ReentrantLock();

    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    public int getCount() {
        return count;
    }
}
```

`lock` 이란 객체를 생성하고, `lock.lock()` 함수 호출하는 것처럼 최초 접근한 `Thread` 접근 허용 후 **잠금 처리한다.**

그 후 비즈니스 로직 수행 완료 후 `lock.unlock()` 함수를 호출하여 **잠금 해제**하는 것을 볼 수 있다.

---

#### 병렬 처리

동시 처리와 병렬 처리는 같으면서도 다른 정의를 할 수 있을 것 같다.

- 동시 처리 : 여러 스레드가 **다른 작업**을 동시에 수행
- 병렬 처리 : 여러 스레드가 **같은 작업**을 동시에 수행

Java 에서 병렬 처리를 위한 방식으로는 `Stream` API 를 사용하거나 많은 프로그래밍 언어에서 사용하는 `Coroutine` 라이브러리가 있을 것 같다.

##### Coroutine

**`Coroutine` 은 코드 블록을 일시 중단 가능한 작업 단위이다.** `Coroutine` 는 `Thread` 와 유사하지만 생성 및 관리를 자동으로 처리한다는 큰 특징이 있다.

다음과 같은 구현 목적을 위해 `Coroutine` 를 많이 활용한다.

- 병렬 처리 : 여려 개의 작업을 동시에 수행 처리
- 비동기 처리 : 여러 개의 작업을 비동기 방식 수행 처리
- 일시 중단 작업 처리 : 작업을 일시 중단할 수 있도록 처리

병렬과 비동기 처리를 위해서는 기존 Java 언어만으로는 코드가 많이 복잡하고 구현 방식이 까다로웠지만,
`Coroutine` 을 활용하면 좀 더 **간견한 코드 구현**과 **`Thread` 관리에 대한 부담이 줄어드는 장점**을 가지게 되는 것 같다.

하지만, 잘못 사용하는 경우 코드에 대한 복잡도가 높아지거나 성능 이슈 또는 서비스 장애 발생할 수 있으니 정확한 상황에 대한 적절한 구현에 대한 결정이 필요할 것 같다.

---

### `CQRS`

---
