---
layout: post
title : Kotlin inline, reified, noinline 정리
date  : 2026-02-11
image : kotlin-overview.png
tags  : kotlin inline reified noinline
---

## 목차

1. [inline 함수](#1-inline-함수)
2. [reified 타입 파라미터](#2-reified-타입-파라미터)
3. [noinline 람다](#3-noinline-람다)
4. [@JvmName 오버로딩](#4-jvmname-오버로딩)
5. [실제 적용 사례](#5-실제-적용-사례)
6. [요약](#6-요약)

---

## 1. inline 함수

### 1.1 동작 원리

`inline` 키워드가 붙은 함수는 컴파일 시 **호출 지점에 함수 본문이 직접 복사**된다.
일반 함수 호출처럼 스택 프레임을 만들지 않고, 코드 자체가 호출부에 펼쳐진다.

```kotlin
inline fun measure(block: () -> Unit) {
    val start = System.nanoTime()
    block()
    println("Elapsed: ${System.nanoTime() - start}ns")
}

// 호출
measure { doSomething() }
```

컴파일 후 (개념적):

```kotlin
// measure {} 호출이 아래처럼 펼쳐짐
val start = System.nanoTime()
doSomething()                          // block() 람다도 인라이닝
println("Elapsed: ${System.nanoTime() - start}ns")
```

### 1.2 inline의 핵심 효과

| 항목 | 일반 함수 | inline 함수 |
|------|----------|------------|
| 함수 호출 | 스택 프레임 생성 | 호출 지점에 코드 복사 |
| 람다 파라미터 | `Function` 객체 생성 (힙 할당) | 코드가 인라이닝 (객체 생성 없음) |
| 타입 파라미터 | 런타임에 소거 (type erasure) | `reified`로 런타임 유지 가능 |

### 1.3 람다도 함께 인라이닝

`inline` 함수에 전달되는 **람다는 기본적으로 함께 인라이닝**된다.
이는 람다가 "객체"가 아니라 **코드 조각**으로 취급된다는 뜻이다.

```kotlin
inline fun runTwice(action: () -> Unit) {
    action()
    action()
}

// 호출
runTwice { println("hello") }

// 컴파일 후
println("hello")    // action() 첫 번째
println("hello")    // action() 두 번째
```

---

## 2. reified 타입 파라미터

### 2.1 문제: JVM의 타입 소거

JVM에서 제네릭 타입은 컴파일 후 소거(erasure)된다.
따라서 일반 함수에서는 타입 파라미터를 런타임에 사용할 수 없다.

```kotlin
fun <T> createMock(): T {
    return mockk<T>()  // ERROR: Cannot use 'T' as reified type parameter
}
```

### 2.2 해결: inline + reified

`inline` 함수는 호출 지점에 코드가 복사되므로, **호출 시점의 구체 타입을 코드에 직접 삽입**할 수 있다.
이것이 `reified`가 반드시 `inline`과 함께 사용되어야 하는 이유이다.

```kotlin
inline fun <reified T> createMock(): T {
    return mockk<T>()  // OK: T가 호출 시점의 구체 타입으로 치환됨
}

// 호출
val mock = createMock<CampaignPromotionVoucherProcessor>()

// 컴파일 후
val mock = mockk<CampaignPromotionVoucherProcessor>()  // T → 구체 타입으로 치환
```

### 2.3 reified로 가능해지는 것

```kotlin
inline fun <reified T> example() {
    // 1. 클래스 참조
    val clazz = T::class

    // 2. 타입 체크
    if (obj is T) { ... }

    // 3. 타입을 인자로 전달
    val instance = jacksonObjectMapper().readValue<T>(json)
}
```

---

## 3. noinline 람다

### 3.1 문제 상황

`inline` 함수의 람다는 기본적으로 인라이닝되어 **코드 조각**이 된다.
코드 조각은 "객체"가 아니므로, **변수에 저장하거나 다른 non-inline 함수에 전달할 수 없다**.

```kotlin
inline fun <reified T> createTestProcessor(
    consumerName: String,
    onProcess: (String) -> Unit,       // 인라이닝된 코드 조각
): T {
    val processor = mockk<T>()
    every { processor.process(any()) } answers {
        onProcess(json)                // ERROR!
        // answers는 non-inline 함수 → 인라이닝된 코드 조각을 전달할 수 없음
    }
    return processor
}
```

`answers { ... }`는 mockk 라이브러리의 일반(non-inline) 함수이다.
인라이닝된 `onProcess`는 객체가 아니라 코드 조각이므로, `answers` 블록 안에 전달할 수 없다.

### 3.2 해결: noinline

`noinline`을 붙이면 해당 람다만 인라이닝에서 **제외**된다.
일반 `Function` 객체로 유지되므로 다른 함수에 자유롭게 전달할 수 있다.

```kotlin
inline fun <reified T> createTestProcessor(
    consumerName: String,
    noinline onProcess: (String) -> Unit,  // 함수 객체로 유지
): T {
    val processor = mockk<T>()
    every { processor.process(any()) } answers {
        onProcess(json)                     // OK: 함수 객체이므로 전달 가능
    }
    return processor
}
```

### 3.3 noinline이 필요한 경우

| 상황 | noinline 필요 여부 |
|------|:---:|
| 람다를 inline 함수 내에서 직접 호출 | 불필요 |
| 람다를 **변수에 저장** | 필요 |
| 람다를 **다른 non-inline 함수에 전달** | 필요 |
| 람다를 **컬렉션에 저장** | 필요 |

```kotlin
inline fun example(
    action: () -> Unit,               // 직접 호출만 → inline OK
    noinline callback: () -> Unit,    // 변수에 저장 → noinline 필요
) {
    action()                           // OK: 직접 호출
    val stored = callback              // OK: noinline이므로 객체로 존재
    someNonInlineFunction(callback)    // OK: 객체이므로 전달 가능
}
```

### 3.4 crossinline

참고로 `crossinline`이라는 키워드도 있다. 람다를 인라이닝하되, **non-local return을 금지**할 때 사용한다.

```kotlin
inline fun runAsync(crossinline action: () -> Unit) {
    thread {
        action()    // 인라이닝되지만, 여기서 return은 불가
    }
}
```

| 키워드 | 인라이닝 | 객체 생성 | non-local return |
|--------|:---:|:---:|:---:|
| (기본) | O | X | O |
| `noinline` | X | O | X |
| `crossinline` | O | X | X |

---

## 4. @JvmName 오버로딩

### 4.1 문제: inline reified 함수의 타입 파라미터 강제

`inline reified` 함수는 호출 시 반드시 **타입 파라미터를 명시**해야 한다.
구체 타입이 필요하지 않은 호출부에서도 매번 타입을 지정해야 하므로 코드가 장황해진다.

```kotlin
// reified 버전만 존재할 때 — 모든 호출부에서 타입 파라미터 필수
val processor = createTestProcessor<AbstractCampaignPromotionProcessor<Any, Any>>("consumer-0") {
    receivedMessages.add(it)
}
```

구체 타입(`CampaignPromotionVoucherProcessor` 등)이 필요한 곳에서는 `reified`가 유용하지만,
기본 추상 타입으로 충분한 곳에서는 불필요한 보일러플레이트가 된다.

### 4.2 해결: non-generic 오버로드 + @JvmName

타입 파라미터 없이 호출할 수 있는 **non-generic 오버로드**를 추가하면 된다.
단, JVM에서는 타입 소거로 인해 두 함수의 시그니처가 동일해지므로 `@JvmName`으로 충돌을 회피한다.

```kotlin
// 1) non-generic 오버로드 — 타입 파라미터 없이 호출 가능
protected fun createTestProcessor(
    consumerName: String,
    onProcess: (String) -> Unit,
): AbstractCampaignPromotionProcessor<*, *> =
    createTestProcessor<AbstractCampaignPromotionProcessor<Any, Any>>(consumerName, onProcess)

// 2) reified 오버로드 — 구체 타입이 필요할 때 사용
@JvmName("createTestProcessorReified")
protected inline fun <reified T : AbstractCampaignPromotionProcessor<*, *>> createTestProcessor(
    consumerName: String,
    noinline onProcess: (String) -> Unit,
): T {
    val processor = mockk<T>()
    every { processor.process(any()) } answers {
        val json = firstArg<String>()
        onProcess(json)
    }
    return processor
}
```

### 4.3 @JvmName이 필요한 이유

JVM의 타입 소거 후 두 함수의 바이트코드 시그니처가 동일해진다:

```
// non-generic 오버로드 (소거 후)
createTestProcessor(String, Function1): AbstractCampaignPromotionProcessor

// reified 오버로드 (소거 후) — inline이지만 Java interop용 바이트코드가 생성됨
createTestProcessor(String, Function1): Object
```

Kotlin 컴파일러는 반환 타입만 다른 두 메서드를 **JVM 레벨에서 구분할 수 없다**.
`@JvmName`으로 바이트코드상의 메서드 이름을 변경하면 충돌이 해소된다.

```
// @JvmName 적용 후
createTestProcessor(String, Function1)         → non-generic
createTestProcessorReified(String, Function1)  → reified (바이트코드 이름만 변경)
```

Kotlin 소스 코드에서는 **둘 다 `createTestProcessor`로 호출**할 수 있다.
JVM 이름은 바이트코드에만 영향을 미치며, Kotlin 호출부의 사용법은 변하지 않는다.

### 4.4 오버로드 해소 규칙

Kotlin 컴파일러는 다음 규칙으로 어떤 오버로드를 선택할지 결정한다:

| 호출 방식 | 선택되는 오버로드 | 이유 |
|----------|:---:|------|
| `createTestProcessor("name") { ... }` | non-generic | 타입 파라미터 추론 불필요 → 더 구체적 |
| `createTestProcessor<VoucherProcessor>("name") { ... }` | reified | 명시적 타입 파라미터 → generic만 해당 |

```kotlin
// Consumer/Producer 테스트 — 타입 파라미터 불필요, 간결한 호출
val processor = createTestProcessor("consumer-$i") { json ->
    receivedMessages.add(json)
}

// Manager 테스트 — 구체 타입 필요, 타입 파라미터 명시
mockVoucherProcessor = createTestProcessor<CampaignPromotionVoucherProcessor>("voucher") {
    processedMessages.add(it)
}
```

### 4.5 non-generic 오버로드의 위임 패턴

non-generic 오버로드는 구현을 **reified 오버로드에 위임**한다.
이렇게 하면 mock 생성 로직이 한 곳에만 존재하여 중복이 없다.

```kotlin
// non-generic → reified에 위임 (구체 타입을 직접 지정)
protected fun createTestProcessor(
    consumerName: String,
    onProcess: (String) -> Unit,
): AbstractCampaignPromotionProcessor<*, *> =
    createTestProcessor<AbstractCampaignPromotionProcessor<Any, Any>>(consumerName, onProcess)
//                      ↑ 호출 시점에 구체 타입이 확정 → reified가 정상 동작
```

이것이 가능한 이유: `inline` 함수는 호출 지점에 인라이닝되므로,
non-generic 함수 **내부에서** reified 함수를 호출하면 그 시점의 구체 타입(`AbstractCampaignPromotionProcessor<Any, Any>`)이 삽입된다.

---

## 5. 실제 적용 사례

### 5.1 적용 전: mock 보일러플레이트

```kotlin
// RedisStreamIntegrationTestBase.kt
protected fun createTestProcessor(
    consumerName: String,
    onProcess: (String) -> Unit,
): AbstractCampaignPromotionProcessor<*, *> {
    val processor = mockk<AbstractCampaignPromotionProcessor<Any, Any>>()
    every { processor.process(any()) } answers {
        val json = firstArg<String>()
        onProcess(json)
    }
    return processor
}
```

반환 타입이 `AbstractCampaignPromotionProcessor<*, *>`로 고정되어, 구체 타입(`CampaignPromotionVoucherProcessor` 등)이 필요한 곳에서 사용할 수 없었다.

```kotlin
// PromotionStreamManagerIntegrationTest.kt — 12줄의 보일러플레이트
mockVoucherProcessor = mockk<CampaignPromotionVoucherProcessor>()
every { mockVoucherProcessor.process(any()) } answers {
    val json = firstArg<String>()
    println("[TEST][${Thread.currentThread().name}] voucher received: $json")
    processedMessages.add(json)
}

mockPointProcessor = mockk<CampaignPromotionPointProcessor>()
every { mockPointProcessor.process(any()) } answers {
    val json = firstArg<String>()
    println("[TEST][${Thread.currentThread().name}] point received: $json")
    processedMessages.add(json)
}
```

### 5.2 적용 후: inline reified + noinline + @JvmName 오버로딩

```kotlin
// RedisStreamIntegrationTestBase.kt

// non-generic 오버로드 — 타입 파라미터 없이 호출 가능
protected fun createTestProcessor(
    consumerName: String,
    onProcess: (String) -> Unit,
): AbstractCampaignPromotionProcessor<*, *> =
    createTestProcessor<AbstractCampaignPromotionProcessor<Any, Any>>(consumerName, onProcess)

// reified 오버로드 — 구체 타입이 필요할 때 사용
@JvmName("createTestProcessorReified")
protected inline fun <reified T : AbstractCampaignPromotionProcessor<*, *>> createTestProcessor(
    consumerName: String,
    noinline onProcess: (String) -> Unit,
): T {
    val processor = mockk<T>()                        // reified: 구체 타입으로 mock 생성
    every { processor.process(any()) } answers {
        val json = firstArg<String>()
        println("[TEST][${Thread.currentThread().name}] $consumerName received: $json")
        onProcess(json)                                // noinline: 함수 객체로 전달
    }
    return processor
}
```

```kotlin
// PromotionStreamConsumerIntegrationTest.kt — 타입 파라미터 불필요, 기존 호출 유지
val processor = createTestProcessor("consumer-$i") { json ->
    receivedMessages.add(json)
}

// PromotionStreamManagerIntegrationTest.kt — 구체 타입 필요, 4줄로 축소
mockVoucherProcessor = createTestProcessor<CampaignPromotionVoucherProcessor>("voucher") {
    processedMessages.add(it)
}
mockPointProcessor = createTestProcessor<CampaignPromotionPointProcessor>("point") {
    processedMessages.add(it)
}
```

### 5.3 각 키워드의 역할

```
inline fun <reified T> createTestProcessor(
│              │
│              └── T를 런타임에 사용 (mockk<T>() 가능)
│                  → inline이 함수 본문을 호출 지점에 복사하므로 가능
│
└── 함수 본문을 호출 지점에 복사
    → reified의 전제 조건

    noinline onProcess: (String) -> Unit
    │
    └── 이 람다만 인라이닝 제외 → 함수 객체로 유지
        → answers { onProcess(json) } 처럼 non-inline 함수에 전달 가능
```

---

## 6. 요약

| 키워드 | 적용 대상 | 핵심 역할 |
|--------|----------|----------|
| `inline` | 함수 | 함수 본문 + 람다를 호출 지점에 복사 |
| `reified` | 타입 파라미터 | 타입 소거 우회, 런타임에 구체 타입 사용 가능 |
| `noinline` | 람다 파라미터 | 인라이닝 제외, 함수 객체로 유지하여 다른 함수에 전달 가능 |
| `@JvmName` | 함수 | 바이트코드 메서드 이름 변경, 타입 소거로 인한 시그니처 충돌 회피 |

**`reified`는 `inline` 없이 사용 불가**하고, **`noinline`은 `inline` 함수 안에서만 의미가 있다**.
세 키워드는 독립적이 아니라 `inline`을 중심으로 연결된 관계이다.

`@JvmName`은 inline reified 함수와 non-generic 함수를 **같은 이름으로 오버로딩**할 때,
JVM 레벨의 시그니처 충돌을 해소하는 데 사용된다.
