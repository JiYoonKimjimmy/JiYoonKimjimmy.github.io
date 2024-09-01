---
layout: post
title : Coroutine (feat. Kotlin)
date  : 2024-08-31
image : kotlin_coroutines.png
tags  : coroutine 코루틴
---

`Coroutine 코루틴` 은 동시성 처리를 위한 비즈니스 로직에서 많이 사용되고 있는 프로그래밍 기법이라 할 수 있다.

코루틴은 코틀린 언어뿐만 아니라 Go-lang, C++, Javasacript 등 많은 언어에서 지원하고 있고, 
코틀린 진영에서도 많은 개발자들이 적극 활용하여 비동기 프로그래밍을 구현하고 있다.

---

## Coroutine 코루틴에 대해서

먼저 코루틴은 간단히 정리해보자면, **프로세스를 일시 정지할 수 있도록 하는 하나의 함수이다.**
프로세스를 일시 정지한다는 의미는 `Main Routine` **메인 루틴**에서 실행되고 있는 `Sub Routine` **서브 루틴**을 **일시 정지**하고 다시 **재개**할 수 있다는 것이다.

코루틴은 **협력적 멀티태스킹**을 지원하고, 비동기 프로그래밍을 쉽게 할 수 있도록 도와주는 **경량 스레드**라고도 많이 불린다.

- **경량성** : 코루틴은 스레드보다 훨씬 가볍고 생성 비용이 적기 때문에, 수천 개의 코루틴을 생성해도 메모리와 성능 큰 영향이 없다.
- **비동기 코드 간결성** : 흔히 **콜백 지옥**과 같은 코드의 가독성 저하 문제를 피할 수 있고, 동기 코드처럼 작성할 수 있다.
- **구조화된 동시성** : 코루틴은 부모-자식 관계를 통해 구조화된 동시성을 지원한다.
  - 코루틴의 `parent` 가 종료되면, `parent` 의 모든 `children` 도 취소된다.
  - `child` 코루틴 실행 중 `exception` 예외 발생하여 종료되면, `exception` 은 `parent` 에게 전달되어 취소시킨다.
  - 만약 `child` 에서 명시적으로 취소하면, `parent` 에게 취소는 전파되지 않는다.

### Thread 스레드 vs. Coroutine 코루틴

- `Thread` **스레드**는 OS 운영체제에서 관리하는 독립적인 실행 단위이다. 스레드는 생성 비용과 `Context-Switching` 전환 비용이 크다.
- `Coroutine` **코루틴**은 스레드 위에서 실행되는 경량 스레드이다. 코루틴은 스레드보다 생성 비용이 적고, **컨텍스트 스위칭 비용이 적다.**
- 스레드는 `Preemptive Multitasking` **선점형 멀티태스킹**을 사용하고, 코루틴은 `Cooperative Multitasking` **협력형 멀티태스킹**을 사용한다.

---

## Coroutine Builder 코루틴 빌더

**코루틴 빌더**는 코루틴을 생성하고 관리하는 함수로, 다양한 종류의 코루틴 생성하는 방법을 제공한다.

### 주요 코루틴 빌더

- `runBlocking`
- `launch`
- `async`
- `withContext`

#### `runBlocking`

- `runBlocking` : 현재 스레드를 `blocking` 차단하여 블록 내의 코루틴이 시작하고, 완료될 때까지 기다린다.
  - 주로 메인 함수나 테스트 코드에서 코루틴을 실행할 때 사용된다.

```kotlin
fun main() = runBlocking {
    launch {
        delay(1000)
        println("World!")   
    }
    println("Hello")
}

// Hello
// World!
```

1. `runBlocking` 블록 내에서 스레드가 차단되고, 블록 내의 코루틴 코드가 실행된다.
2. `launch` 블록 내에서 코루틴이 실행되고, 동시에 블록 내 또다른 코드가 실행되어 `Hello` 메시지가 출력된다.
3. `delay` 함수는 `suspend` 함수이며, 코루틴을 일시 정지하여 1초 후에 `World!` 메시지가 출력된다.
   - `delay` 함수는 `suspend` 함수이며, 코루틴을 1초동안 정지시킨다.

#### `launch`

- `launch` 는 코루틴을 시작하고, 결과를 반환하지 않는 대신 `Job` 객체를 반환하여 코루틴의 상태를 추적하거나 취소할 수 있다.

```kotlin
runBlocking {
  launch {
      // 코루틴 내에서 실행하는 코드
  }
}
```

#### `async`

- `async` 는 코루틴을 시작하고, `Deferred` 객체를 통해 결과를 반환한다.

```kotlin
runBlocking {
    val deferred = async {
        // 코루틴 내에서 실행하는 코드
    }
    val result = deferred.await()   // 결과 반환할 때까지 기다림
}
```

#### `withContext`

- `withContext` 는 현재 코루틴을 일시 중단하고, 지정된 컨텍스트에서 코루틴을 실행하여 결과를 반환한다.
- 주로 다른 디스패처로 전환할 때 사용된다.
- 만약 실행되고 있는 코루틴에서 DB 작업과 같은 네트워크 I/O 작업이 필요하다면, `withContext(Dispatchers.IO)` 를 사용하여 컨텍스트를 전환할 수 있다.
- 만약 컨텍스트 전환이 없다면, 메인 스레드를 그대로 사용하면서 I/O 작업이 실행되기 때문에 성능 이슈가 발생할 수 있다.

```kotlin
runBlocking {
    val result = withContext(Dispatchers.IO) {
        // IO 디스패처에서 실행할 코드
        delay(1000L)
        "Result"
    }
    println(result) // Output: Result
}
```

---

## Coroutine Scope 코루틴 범위

**코루틴 스코프**는 코루틴을 실행하는 범위를 나타내며, 코루틴의 생명주기를 제어하기 위해 사용된다.

코루틴 스코프는 코루틴 빌더를 사용하여 코루틴을 시작할 수 있는 컨텍스트를 제공한다.
그리고, 코루틴의 **구조적 동시성**을 보장하는 중요한 역할을 한다.

###  `GlobalScope`

- 애플리케이션 전역 범위에서 수명과 코루틴의 수명이 동일한 경우 사용된다.
- 코루틴이 종료되지 않고 계속 실행되는 경우, 메모리 누수가 발생할 수 있다.
- 전역 스코프로 **구조적 동시성을 보장하지 않기 때문에** 일반적으로 사용되지 않는다.

### `CoroutineScope`

- 특정 범위에서 코루틴을 실행하는 데 사용된다.
- 특정 작업이나 컴포넌트의 수명동안 코루틴을 실행한다.
- **구조적 동시성을 보장한다.**

---

## Coroutine Context 코루틴 컨텍스트

**코루틴 컨텍스트**는 코루틴의 실행 환경을 정의하는 요소들의 집합을 의미한다.

### 주요 코루틴 컨텍스트 요소

- `Job` : 코루틴의 생명 주기를 관리
- `CoroutineDispatcher` : 코루틴이 실행되는 스레드를 결정
- `CoroutineName` : 코루틴의 이름을 지정
- `CoroutineExceptionHandler` : 코루틴에서 발생하는 예외를 처리

#### Dispatchers 디스패처

`Dispatchers` 는 코루틴이 실행되는 어떤 스레드 또는 스레드-풀에서 실행될지를 결정하는 역할을 한다.

- `Dispatchers.Default` : 공유된 백그라운드 스레드-풀에서 실행되며, CPU 집약적인 작업에 적합
- `Dispatchers.IO` : I/O 작업 최적화된 디스패처로, 파일 읽기/쓰기, 네트워크 요청 등 사용
- `Dispatchers.Main` : 메인 스레드에서 코루틴을 실행
- `Dispatchers.Unconfined` : 특정 스레드에 바인딩되지 않고, 호출된 스레드에서 실행

---

## Coroutine Cancelation & Timeout 코루틴 취소와 타임아웃

### `cancel`

### `withTimeout`

### `withTimeoutOrNull`

---

## Coroutine Exception Handling 코루틴 예외 처리

### `try-catch`

### `CoroutineExceptionHandler`

---

## Coroutine Channels & Flows 코루틴 채널 & 플로우

### `Channel`

### `Flow`

---

#### 출처

- [Kotlin Official Documentation - Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Ecem Okan - Introduction to Kotlin Coroutines](https://medium.com/@ecemokan/introduction-to-kotlin-coroutine-458353fb4c70)
- [당근마켓 밋업 - Kotlin Coroutines 톺아보기](https://www.youtube.com/watch?v=eJF60hcz3EU)
- [나무위키 - 코루틴](https://namu.wiki/w/%EC%BD%94%EB%A3%A8%ED%8B%B4)

---