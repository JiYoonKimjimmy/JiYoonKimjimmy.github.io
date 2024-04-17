---
layout: post
title : Ready for Interview
date  : 2024-01-27
image : ready-for-interview.png
tags  : interview back-end
---

## Back-End 개발자 인터뷰를 위한 준비

알고 있는 것도 헷갈리고, 서툰 신입이 되는 순간이 인터뷰를 진행하는 순간인 것 같다.
항상 처음 사회 생활을 시작하는 마음으로 준비된 모습을 보여주기 위해서는 아는 것도 기록하고, 복기하고, 정리하는 것이 중요하다고 생각이 들었다.

개발자로서 CS부터 실무 개발 프레임워크까지 너무 알아야할 지식들이 많지만, 정리를 하다보면 남들만큼 알 수 있지 않을까 하는 희망을 가져본다.

---

### Items

1. Java `Thread-Pool` 관리
1. Java `for-loop` 처리 방식
1. Java 동시성 처리 패턴
1. Java 병렬 처리 패턴
1. `CQRS` 개념에 대해서
1. 데이터베이스 `Isolation Level`
1. Spring F/W `Bean` 생성 패턴
1. `JWT` Token 인증
1. `OAuth` 인증
1. Kotlin 언어 특징
1. Spring F/W & Spring Boot 최신 버전 확인
1. 시스템 아키텍처 디자인
1. 시스템 인프라 모니터
1. Redis

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

- `Thread-Pool` **크기 조절** 가능
- `Thread` 의 실행 **우선 순위 지정** 가능
- `Thread` 실행 중인 상태에서 **대기 시간 조절** 가능
- `Thread` 실행 대기 `Queue` **종류 지정** 가능

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

### Java `for-loop` 처리 방식

#### 단순 `for-loop`

- 가장 기본적인 형태의 반복문
- 초기화식, 조건식, 증감식으로 반복문을 구성

```java
for (int i = 0; i < 5; i++) {
    System.out.println("현재 숫자 : " + i);
}
```

#### 향상된 `for-loop`

- `Collection` 객체 또는 `Array` 객체를 순회하면서 간편한 방법으로 반복문을 구성(`for-each` 구문이라고 한다.)
- 배열 또는 컬렉션의 각 요소를 순차적으로 접근하여 처리

```java
int[] numbers = {1, 2, 3, 4, 5};
for (int number : numbers) {
    System.out.println("현재 숫자: " + number);
}
```

#### `Stream` 객체

- `Java 8` 에서 추가된 기능으로, `Collection` 반복 처리 지원 객체
- 함수형 프로그래밍 스타일 지원

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
numbers.stream().forEach(number -> System.out.println("현재 숫자: " + number));
```

---

#### Kotlin Collection API 처리 방식

Kotlin 언어에서는 `List`, `Map` 등 `Collection` 객체을 편리하게 활용할 수 있는 많은 함수를 확장 함수로 지원하고 있다.
`List` 객체의 `.filter()`, `.map()` 등 비즈니스 로직을 처리할 때 많이 사용하는 함수를 대부분 확장 함수로 정의되어 있다보니,
Java 의 `Stream` 객체 API 를 활용할 때보단 편하게 코딩을 할 수 있다.

그렇다면, 그런 확장 함수는 과연 어떤 동작을 하게 되는 것일까? 바로 위에 바로 언급한 Java 의 `Stream` 객체이다.

```kotlin
val list = listOf(1, 2, 3, 4)
list.filter { it > 2 }
println(list)   // [3, 4]
```

위와 같은 Kotlin 의 코드는 실행을 위해 컴파일되는 시점에 아래와 같은 Java 코드 변환 뒤 `.class` 파일로 컴파일된다.

```java
List<Integer> list = Arrays.asList(1, 2, 3, 4);
list.stream().filter(i -> i > 2);
System.out.println(list);
```

결국은 `Stream` 객체를 사용하기 때문에, Kotlin 에서도 `Collection` 객체를 사용할 때 주의 사항 중 하나인 **중간 객체 생성의 조심성**이 생긴 것 같다.

> Kotlin 에서 `List` 와 같은 객체에 많은 *Chaining 체이닝*을 하는 경우 많은 양의 중간 객체가 생성되면서 `OOM` 에러 발생 여지가 있다.

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

Java 에서 병렬 처리를 위한 방식으로는 `Stream` API 를 사용하거나 많은 프로그래밍 언어에서 지원하고 있는 `Coroutine` 라이브러리를 활용할 수 있다.

##### Coroutine

`Coroutine` 은 일종의 **코드 블록을 일시 중단 가능한 작업 단위이다.** `Coroutine` 는 `Thread` 와 유사하지만, **생성 및 관리를 자동으로 처리한다**는 큰 특징이 있다.

다음과 같은 구현 목적을 위해 `Coroutine` 를 많이 활용한다.

- 병렬 처리 : 하나 또는 여러 개의 작업을 **동시 수행 처리**
- 비동기 처리 : 여러 개의 작업을 **별도 수행 처리**
- 일시 중단 작업 처리 : 작업을 **일시 중단**할 수 있도록 처리

병렬과 비동기 처리를 위해서는 기존 Java 언어만으로는 코드가 많이 복잡하고 구현 방식이 까다로웠지만,
`Coroutine` 을 활용하면 좀 더 **간견한 코드 구현**과 **`Thread` 관리에 대한 부담이 줄어드는 장점**을 가지게 되는 것 같다.

하지만, 잘못 사용하는 경우 코드에 대한 복잡도가 높아지거나 성능 이슈 또는 서비스 장애 발생할 수 있으니 정확한 상황에 대한 적절한 구현에 대한 결정이 필요할 것 같다.

---

#### Sync vs Async & Blocking vs Non-Blocking

많은 애플리케이션에서 성능 최적화를 위해 `Sync 동기식` 보다는 `Async 비동기식` 프로그래밍을 하고, `Blocking` 보다는 `Non-Blocking` 을 하여,
프로세스와 스레드를 최대한 활용하여 높은 `Throughput 처리량` 성능 효과를 볼 수 있을 것이다.

하지만 무차별적이고 잘못된 방식의 `Async & Non-Blocking` 프로그래밍은 오히려 성능 저하와 장애을 불러올 수 있다.

##### Sync vs Async

동기와 비동기의 차이는 여러 개의 작업이 주어졌을 때, **순차적으로 수행할지 vs 한번에 수행**할지로 정리해볼 수 있다.

작업 수행 속도로만 따진다면, **분명 비동기 방식이 빠를 것이다.** 하지만 **작업 순서가 보장**되어야 하는 비즈니스 로직에서는 당연히 **동기식 프로그래밍이 필요할 것이다.**

그러므로, 각 방식에 대한 개념을 잘 이해하고 상황에 맞는 적절한 개발을 해야 한다.

##### Blocking vs Non-Blocking

블로킹과 논-블로킹의 개념은 조금 더 어려울 수 있다. 이 두가지는 스레드의 작업 제어권이 어느 쪽에 있는지를 이해하는 것이 중요하다.

`Blocking 블로킹`은 이미 수행 중인 **작업(A)**에게 있던 제어권이 다른 **작업(B)**이 수행되면 **작업(B)** 대상이 제어권을 가져가게 되면서, 기존 **작업(A)**은 일시 중단되는 방식이다.

`Non-Blocking 논-블로킹`은 이미 수행 중인 **작업(A)**가 끝날 때까지 제어권을 소유하고, 다른 **작업(B)**이 필요하다면 새로운 제어권으로 수행되는 방식이다.
논-블로킹 방식도 뭔가 동시에 작업이 수행되기 때문에 높은 성능 효과를 볼 수 있지만, 완벽한 논-블로킹 성능 효과를 보기 위해서는 모든 작업이 논-블로킹 방식여야 하는 점과 구현 & 디버깅의 어려움이 있다.

---

### CQRS 패턴

`CQRS` 패턴은 `Command Query Responsibility Segregation` 라는 의미로, **명령과 질의를 분리하는 개념**의 아키텍처 패턴이다.

- `Command 명령` : 데이터를 변경하는 작업
- `Query 질의` : 데이터를 조회하는 작업

`CQRS` 는 데이터를 변경 작업을 위한 접근 대상과 데이터를 조회를 위한 접근 대상을 다르게 구성하는 것이다.

이렇게 분리하는 목적은,

- 성능 향상 : 변경 작업에 대한 부담을 분리하여 조회하는 서비스에 대한 성능 향상 효과 기대
- 확장성 향상 : 분리된 각 영역에 대한 확장성 개선
- 유연성 향상 : 각 영역 별 맞춰진 방식에 대한 개발 및 유지 보수 관리 가능

---

### 데이터베이스 Isolation Level

데이터베이스를 활용한 애플리케이션을 개발하면서 주의를 기울여야 하는 것이 바로 `Transaction 트랜잭션` 일 것이다.

트랜잭션은 데이터의 일관성과 유효성을 지켜주는 중요한 수단이 되기 때문에, 대용량 트래픽의 서비스인 경우 각별히 주의할 로직 중 일부분이다.

데이터베이스는 기본적으로 데이터에 대한 트랜잭션 처리를 위해 `Isolation Level 격리 수준` 을 가지고 있고,
**데이터베이스의 데이터의 일관성과 유효성을 *보장*하기 위해 트랜잭션을 *격리*하고 처리하는 수준을 지정하고 관리하게 된다.**

트랜잭션을 **격리** 한다는 의미는, 하나의 트랜잭션이 접근한 데이터에 대해서는 다른 트랜잭션이 조회 또는 변경을 하지 못하는 것을 의미한다.

데이터베이스는 **격리 수준** 설정을 통해 다른 트랜잭션이 조회 또는 변경 수준을 조절할 수 있도록 여러 가지의 옵션을 제공하고 있고, **총 4가지**의 격리 수준을 제공한다.

- `SERIALIZABLE`
- `REPEATABLE READ`
- `READ COMMITTED`
- `READ UNCOMMITED`

#### SERIALIZABLE

`SERIALIZABLE` 는 *가장 엄격한 수준*으로, 트랜잭션을 말그대로 **순차적으로 처리**하는 수준을 의미한다.

여러 개의 트랜잭션이 동일한 데이터에 동시 접근이 불가하기 때문에, 데이터의 부정합 문제는 발생하지 않을 것이다.
하지만, 반대로 트랜잭션이 순차적으로 실행된다는 특성 때문에 동시 처리 성능은 현저히 떨어질 것이다.

#### REPEATABLE READ

`REPEATABLE READ` 는 **트랜잭션이 시작되기 전에 `COMMIT 커밋` 완료된 데이터만 조회가 가능한** 수준을 의미한다.

해당 격리 수준은 하나의 트랜잭션 내에서 동일한 결과를 보장하지만, 새로운 레코드 추가되는 경우 데이터 부정합이 생길 수 있다.

RDBMS 기준 변경 전의 데이터 레코드를 `Undo` 저장 공간에 `Back-up 백업` 하고, `MVCC (Multi-Version Concurrency Control)` **다중 버전 동시성 제어**을 통해
트랜잭션 롤백에 대한 데이터 복원과 서로 다른 트랜잭션 간의 세밀한 데이터 관리가 가능하다.

하지만, `MySQL` 의 `Gap Lock 갭-락` 처럼 별도 처리가 없다면, `Phantom Read` 와 같은 데이터 부정합이 발생할 수 있다.

`READ COMMITTED` 격리 수준보다 성능적인 측면에서 다소 느릴 수 있지만, 보다 **높은 데이터 정합성을 보장**할 수 있다.

> 자세한 내용은 [망나니 개발자님의 "[MySQL] 트랜잭션의 격리 수준(Isolation Level)에 대해 쉽고 완벽하게 이해하기"](https://mangkyu.tistory.com/299) 참고

#### READ COMMITTED

`READ COMMITTED` 는 **커밋이 완료된 데이터만 조회 가능**하도록 하는 수준을 의미한다.

트랜잭션이 시작된 후 **이미 커밋된** 데이터만 읽지만, 다른 트랜잭션이 해당 데이터를 변경하고 커밋하였다면 **데이터 변경 내용이 이전 트랜잭션에 반영**될 수 있다.

해당 격리 수준은 `Phantom Read` 와 `Non-repeatable Read` 이슈까지 발생할 수 있다.
커밋이 완료되지 않은 상태에서 다른 트랜잭션 조회 요청은 `Undo` 영역의 데이터 조회되기 때문이다.

많은 DBMS에서 해당 격리 수준을 기본으로 설정하고 있지만, 데이터 잠금 정책을 통해서 해결할 수 있을 것 같다.

`REPEATABLE READ` 격리 수준보다 데이터 정합성은 떨어질 수 있으나, **높은 성능적인 이점**을 가질 수 있다.

#### READ UNCOMMITED

`READ UNCOMMITED` 는 *가장 낮은 수준*으로, **커밋되지 않는 데이터도 조회 가능**하도록 하는 수준을 의미한다.
다른 트랜잭션이 커밋 또는 롤백되지 않은 데이터도 모두 조회 가능하다.

해당 격리 수준은 많은 DBMS에서도 인정하지 않을 정도의 수준이라고 한다.

##### DBMS 기본 격리 수준

| DBMS | 격리 수준 |
| :---: | :---: |
| MySQL | `REPEATABLE READ` |
| OracleDB | `READ COMMITTED` |
| PostgreSQL | `READ COMMITTED` |
| MS SQL Server | `READ COMMITTED` |
| MongoDB | `READ UNCOMMITED` |

---

##### Isolation Level 장애 이슈

###### `Phantom Read`

- 하나의 트랜잭션 내에서 동일한 쿼리(조회)의 결과가 없어지거나 다른 `row` 가 생성되는 경우
- `회원-A`가 데이터 접근을 여러번 하는 동안, `회원-B`가 신규 데이터를 생성한다면, `회원-A`의 동일한 조회 결과가 **없어질 수 있다.**
- 방지하기 위해서는 `Write Lock, Exclusive Lock` 쓰기 잠금 처리가 필요하다.

###### `Non-repeatable Read`

- 하나의 트랜잭션 내에서 동일한 쿼리(조회)의 결과값이 달라지는 경우
- `트랜잭션-1`이 데이터 접근 후 완전히 실행 종료되기 전에 `트랜잭션-2`가 데이터 접근하여 변경한다면, `트랜잭션-1`이 다시 데이터 접근한 경우 **변경될 수 있다.**

###### `Dirty Read`

- 하나의 트랜잭션에서 아직 데이터에 대한 변경이 끝나지 않은 상태*(commit 되지 않은 상태)*에서 다른 트랜잭션이 조회하는 경우
- 기존 변경하던 트랜잭션의 작업은 **`Roll-back 롤백` 된다.**

---

### Spring Framework

#### Spring F/W 특징

- DI
- IoC
- DispatcherServlet
- Filter
- Interceptor
- AOP
- DelegatingFilterProxy
- Spring Boot
- Spring Security
- Spring Cloud

---

#### Spring F/W `Bean` 생성 패턴

Spring F/W 의 기본 `Bean` 생성 패턴은 `Single-ton 싱글턴` 객체 생성 패턴이다. 그리고 아래와 같은 패턴을 지정할 수 있다.

##### Spring Bean Scope 종류

| Scope 구분 | 설명 |
| :---: | --- |
| `singleton` | 단 하나의 인스턴스만 생성 |
| `prototype` | 요청마다 새로운 인스턴스 생성 |
| `request` | `HTTP` 요청 완료될 때까지 인스턴스 유지 |
| `session` | `HTTP` 세션 유지되는 동안 인스턴스 유지 |
| `global-session` | 글로벌 세션 유지되는 동안 인스턴스 유지 |
| `thread` | 실행 스레드가 유지되는 동안 인스턴스 유지 |

---

### 시스템 아키텍처 디자인

- 기본 MVC 패턴 RESTful API 시스템
- 분산 시스템 구조
- Load-Balancing 처리
    - 클라우드 인프라 로드-밸런서
    - `L4 s/w + Nginx` 기반 로드 밸런싱 장점
- Cache 처리
- Event-Sourcing & CQRS 패턴 처리
    - `Axon`
- 데이터 분산 및 Sharding 처리

#### 회원 가입 & 로그인 & 인증 시스템 디자인

- 도메인 설계
    - 회원 정보
- 회원 가입 프로세스 정리
    - 회원 정보 수집 (OAuth, 통신사 인증 등)
    - 회원 정보 검증
        - 정보 유효성 검증
        - 중복 가입 방지
    - 회원 정보 저장
    - 회원 기기 정보 저장
- 로그인 프로세스 정리
    - 회원 가입 정보 인증
    - API 인증 Token 발급
- API 인증 프로세스 정리
    - JWT Token 인증
- JWT Token 인증 처리 방식 고도화
    - Access Token 인증
    - Refresh Token 인증 & Access Token 갱신

---

### 시스템 인프라 모니터링

- 네트워크 모니터링
- 서버 모니터링
- 데이터베이스 모니터링
- 대용량 트래픽 대응 경험

#### 모니터링 도구

- [Prometheus](https://prometheus.io/)
- [Datadog](https://www.datadoghq.com/)
- [Jennifer](https://jennifersoft.com/ko/)

---

### Redis

Redis 는 `key-value` 형식의 데이터를 저장할 수 있는 `In-memory` 형식의 `NoSQL` 저장소이다.
인-메모리 저장소로 주로 `Cache 캐시` 서버로 주로 활용하지만, 다양한 데이터 형식을 지원하기 때문에 캐시 서버 외 다양한 용도 활용성이 있다.

Redis 가 인-메모리 저장소로 데이터를 임시 저장만 하지 않고 영구적으로 디스크에 저장할 수 있는 백업 기능도 있으며~~(하지만 그래도 저장 공간에 대한 제약은 있다.)~~,
분산 처리를 위한 클러스터 구조 설계, 싱글-스레드 아키텍처로 설계되어 멀티-스레드 구조보단 `Thread-Safe` 한 구조를 가지고 있다.

#### Redis 데이터 백업 방식

##### RDB (Redis Database)

- 메모리에 있는 전체 데이터의 스냅샷을 생성하여 디스크에 저장하는 방식
- 특정 시간마다 여러 개의 스냅샷을 생성하고, 데이터 복원 시점 스냅샷을 그대로 로딩하는 방식
- 만약 특정 시간 이후 변경된 데이터에 대해서는 복구 불가하여 데이터 유실

##### AOF (Append Only File)

- 데이터가 생성/변경/삭제 이벤트 발생하면 **초 단위** 수집하여 모두 로그 파일 작성 방식
- 최신 데이터 백업 가능하며 RDB 방식보다 유실량 적음
- RDB 방식보다 느린 성능과 파일 크기가 커지는 단점

#### Redis Single-Thread 구조

- Redis 는 사용자의 명령을 `Queue` 에 담아 `Event-loop` 를 통해 `Single-Thread` 방식으로 처리
- 싱글-스레드 처리 방식이기 때문에 `Context-Switch` 가 발생하지 않아 효율적인 시스템 리소스 활용 가능
- 하지만, 오버-헤드가 큰 작업 처리하는 동안 다른 작업이 불가한 성능 이슈 발생 가능

---