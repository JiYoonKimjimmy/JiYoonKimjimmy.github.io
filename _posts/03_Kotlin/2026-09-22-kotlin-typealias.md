---
layout: post
title : Kotlin typealias 정리 — 별칭의 쓸모와 한계
date  : 2026-09-22
image : kotlin-type-system-logo.png
tags  : kotlin typealias funinterface generics valueclass
---

## 목차

1. [typealias란 무엇인가](#1-typealias란-무엇인가)
2. [문법과 선언 위치](#2-문법과-선언-위치)
3. [주요 용도](#3-주요-용도)
4. [typealias가 해주지 않는 것](#4-typealias가-해주지-않는-것)
5. [실전 사례 — 제네릭 타입 인자를 닫아두는 함수 타입](#5-실전-사례--제네릭-타입-인자를-닫아두는-함수-타입)
6. [typealias vs fun interface](#6-typealias-vs-fun-interface)
7. [무엇을 언제 쓰는가](#7-무엇을-언제-쓰는가)
8. [요약](#8-요약)

---

> 이 글은 **Kotlin 2.3.0** 기준으로 작성했다. 선언 위치(2장)와 `fun interface` 비교(6장)는 버전에 따라 내용이 달라지므로, 다른 버전을 쓴다면 해당 릴리즈 노트를 함께 확인하는 편이 좋다.

## 1. typealias란 무엇인가

`typealias`는 기존 타입에 **다른 이름을 붙이는 선언**이다. 새로운 타입을 만드는 것이 아니라, 컴파일 시점에 원래 타입으로 **치환(expansion)**된다. 런타임에는 존재하지 않는다.

```kotlin
typealias UserId = String

val id: UserId = "u-1"
val raw: String = id        // OK — 완전히 같은 타입
```

이 한 줄이 typealias에 대한 오해와 쓸모를 동시에 설명한다. 이름만 바뀔 뿐 타입 체계에는 아무 일도 일어나지 않는다.

## 2. 문법과 선언 위치

```kotlin
typealias Name = Type                       // 기본
typealias Predicate<T> = (T) -> Boolean     // 제네릭 파라미터 가능
private typealias Handler = (Int) -> Unit   // 가시성 제어자 가능 (파일 범위)
```

### 선언할 수 있는 자리

공식 문서는 선언 위치를 **두 곳**으로 나열한다 — 최상위(top-level), 그리고 클래스·인터페이스·object 내부(nested). 금지되는 곳은 **로컬 스코프 하나**다.

> "You can't declare a type alias in a local scope, such as inside a function or lambda expression."

```kotlin
typealias UserIndex = Map<Long, User>       // 최상위 — 가능

class UserRepository {
    typealias CachedUsers = List<User>      // 중첩 — 2.3.0부터 Stable
}

fun process() {
    typealias Local = Int                   // 컴파일 에러
}
```

중첩 별칭은 Kotlin 2.2.0에서 Beta(`-Xnested-type-aliases`)로 들어와 **2.3.0에서 Stable**로 승격됐다. 2.2.0 이전 기준으로 쓰인 자료는 "최상위에서만 가능"이라고 설명하는데, 지금은 맞지 않는다.

중첩 별칭에는 제약이 둘 더 붙는다. **바깥 클래스의 타입 파라미터를 참조할 수 없고**(자기 타입 파라미터는 따로 선언 가능), **멀티플랫폼 `expect`/`actual`에서는 지원되지 않는다.**

### 타입 파라미터에 붙일 수 없는 것

KEEP의 한 문장이 두 가지를 동시에 금지한다.

> "Variance and constraints for type parameters of the generic type aliases are not allowed."

```kotlin
typealias Producer<out T> = () -> T         // 컴파일 에러 — variance 불가
typealias NumberBox<T : Number> = Box<T>    // 컴파일 에러 — 상한 불가
```

variance 제약은 6장에서 다시 나온다.

선언이기 때문에 다른 파일에서도 `import`해서 쓸 수 있다. 이 점이 `import ... as`와 갈리는 지점이다.

```kotlin
import com.example.api.Payment as DtoPayment   // 해당 파일 안에서만 유효
typealias DtoPayment = com.example.api.Payment // 선언이므로 다른 파일에서 import 가능
```

일회성 이름 충돌 해소면 `import as`, 여러 파일에서 공유할 이름이면 `typealias`가 맞다.

## 3. 주요 용도

### 3.1 긴 제네릭 타입 축약

```kotlin
typealias PaymentIndex = MutableMap<String, MutableList<PaymentEvent>>

fun buildIndex(): PaymentIndex = mutableMapOf()
```

### 3.2 함수 타입에 의미 있는 이름 부여

```kotlin
typealias PaymentValidator = (Payment) -> ValidationResult

class PaymentService(private val validate: PaymentValidator)
```

`(Payment) -> ValidationResult`라고 쓰면 시그니처는 보이지만 역할은 안 보인다. 함수 타입 파라미터가 늘어날수록 이름의 값어치가 커진다.

### 3.3 중첩 타입 경로 축약

```kotlin
class Outer {
    inner class Inner
    companion object { fun create() = Outer() }
}

typealias OuterInner = Outer.Inner
```

별칭으로도 원래 타입의 생성자와 companion을 그대로 쓸 수 있다.

```kotlin
typealias Out = Outer

val a = Out()          // 생성자 호출
val b = Out.create()   // companion 함수 호출
```

### 3.4 이름 충돌 해결

패키지가 다른 동명 클래스를 한 파일에서 함께 다룰 때 쓴다.

```kotlin
typealias DomainPayment = com.example.domain.Payment
typealias DtoPayment = com.example.api.Payment
```

### 3.5 멀티플랫폼 actual typealias

`expect` 선언을 플랫폼별 기존 타입에 연결한다.

```kotlin
// common
expect class AtomicCounter

// jvm
actual typealias AtomicCounter = java.util.concurrent.atomic.AtomicInteger
```

## 4. typealias가 해주지 않는 것

### 4.1 타입 안전성

가장 흔한 오해다. 별칭은 잘못된 대입을 하나도 막지 못한다.

```kotlin
typealias UserId = String
typealias OrderId = String

fun find(userId: UserId) {}

val orderId: OrderId = "o-1"
find(orderId)   // 컴파일 통과 — 둘 다 그냥 String
```

식별자를 타입으로 구분하고 싶다면 `value class`를 써야 한다.

```kotlin
@JvmInline
value class UserId(val value: String)

fun find(userId: UserId) {}
find(UserId("u-1"))   // OrderId를 넘기면 컴파일 에러
```

| 구분 | typealias | value class |
|------|:---:|:---:|
| 새 타입 생성 | X | O |
| 잘못된 대입 차단 | X | O |
| 런타임 비용 | 없음 | 대부분 없음(인라인), 일부 상황에서 박싱 |
| 목적 | 가독성 | 타입 안전성 |

### 4.2 오버로드 구분

원래 타입이 같으면 별칭이 달라도 같은 시그니처다.

```kotlin
typealias A = String
typealias B = String

fun handle(v: A) {}
fun handle(v: B) {}   // 컴파일 에러: 시그니처 충돌
```

### 4.3 확장 함수의 범위 한정

별칭에 확장 함수를 붙이면 **원래 타입 전체**에 붙는다.

```kotlin
typealias UserId = String

fun UserId.masked() = take(2) + "***"

"anything".masked()   // String 전체에서 호출 가능
```

### 4.4 Java 가시성

`typealias`는 Kotlin 컴파일러 수준의 개념이라 Java에서는 항상 원래 타입으로만 보인다.

## 5. 실전 사례 — 제네릭 타입 인자를 닫아두는 함수 타입

여기까지는 "긴 이름을 줄인다" 수준이지만, typealias가 **가독성 이상의 역할**을 하는 경우가 있다. 외부 연동 클라이언트를 빌더로 조립하는 구조를 예로 든다.

요구는 이렇다. 연동처마다 기능(조회/승인/취소)을 골라 등록하고, 오류 코드 매핑표와 장애 격리 장치는 기능 전체가 공유한다. 기능별 요청·응답 타입은 서로 다르다.

```kotlin
private typealias Assembler<P> = (ErrorCodeMapper, CallGuard) -> P

class ExternalClientBuilder private constructor(private val vendorCode: String) {

    private var errorMapper: ErrorCodeMapper? = null
    private var callGuard: CallGuard? = null

    // 조각만 받아두고 조립은 build() 에서 — 공통 의존성이 그때 확정됨
    private var inquiry: Assembler<InquiryClient<*, *>>? = null
    private var charge: Assembler<ChargeClient<*, *>>? = null
    private var cancel: Assembler<CancelClient<*, *>>? = null

    fun errorMapper(errorMapper: ErrorCodeMapper) = apply { this.errorMapper = errorMapper }

    fun callGuard(callGuard: CallGuard) = apply { this.callGuard = callGuard }

    fun <REQ : Request, RES : Response> inquiry(operation: InquiryOperation<REQ, RES>) = apply {
        this.inquiry = { m, g -> InquiryClient(vendorCode, operation, m, g) }
    }

    fun build(): ExternalClient {
        val errorMapper = requireNotNull(errorMapper) { "$vendorCode 의 errorMapper 가 없습니다" }
        val callGuard = requireNotNull(callGuard) { "$vendorCode 의 callGuard 가 없습니다" }

        return ExternalClient.create(
            vendorCode = vendorCode,
            inquiryExecutor = inquiry?.invoke(errorMapper, callGuard)?.let { it::execute },
            // ...
        )
    }
}
```

### 5.1 첫 번째 역할 — 생성 시점을 미룬다

`inquiry(operation)`을 호출하는 시점에는 `errorMapper`와 `callGuard`가 아직 없을 수 있다. 그래서 객체를 만들지 않고 **"공통 의존성만 주면 객체를 만들어줄 함수"**를 저장해둔다. 부분 적용된 생성자인 셈이다.

덕분에 빌더 메서드 호출 순서가 자유로워진다. `.inquiry(...)`를 `.errorMapper(...)`보다 먼저 써도 동작하고, 누락 검증은 객체가 하나도 만들어지기 전에 `build()`에서 한 번만 돈다.

### 5.2 두 번째 역할 — 제네릭 타입 인자를 닫는다

이게 본론이다. 필드는 `REQ`/`RES`를 가질 수 없어서 스타 프로젝션으로 선언되어 있다.

```kotlin
private var inquiry: Assembler<InquiryClient<*, *>>? = null
```

만약 람다 대신 operation을 그대로 저장했다면 컴파일이 되지 않는다.

```kotlin
// 동작하지 않는 대안
private var inquiryOp: InquiryOperation<*, *>? = null

fun build() {
    InquiryClient(vendorCode, inquiryOp!!, m, g)
    //                        ^ Foo<*, *> 를 Foo<REQ, RES> 자리에 넣을 수 없음
}
```

`REQ`/`RES`가 살아 있는 곳은 **제네릭 함수 `inquiry()`의 본문 안뿐**이다. 람다를 거기서 만들어두면 타입 관계가 성립하는 상태로 생성 코드가 **클로저 안에 봉인**되고, 바깥에서는 `<*, *>`만 보면 된다.

이 대입이 그냥 되는 이유는 Kotlin의 함수 타입이 `Function2<A, B, out R>`, 즉 **반환 위치에서 공변**이기 때문이다. `Assembler<InquiryClient<REQ, RES>>`가 `Assembler<InquiryClient<*, *>>`에 자동으로 들어간다. typealias는 이 내장 공변성을 그대로 빌려 쓴다.

정리하면 흐름은 이렇다.

```
.inquiry(op)    ─▶ Assembler 저장 (vendorCode + op 캡처, 생성 보류)
.errorMapper(m) ─▶ 공통 의존성 수집
.callGuard(g)   ─▶ 공통 의존성 수집
                      │
build()         ─▶ 누락 검증
                ─▶ assembler(m, g) 호출 → 인스턴스 생성
                ─▶ ::execute 메서드 참조만 꺼내 전달
```

## 6. typealias vs fun interface

같은 자리에 `fun interface`(SAM 인터페이스)를 쓸 수도 있다.

```kotlin
private fun interface Assembler<out P> {
    fun assemble(errorMapper: ErrorCodeMapper, callGuard: CallGuard): P
}
```

```kotlin
this.inquiry = Assembler { m, g -> InquiryClient(vendorCode, operation, m, g) }

// build()
inquiryExecutor = inquiry?.assemble(errorMapper, callGuard)?.let { it::execute }
```

### 6.1 out을 빠뜨리면 깨진다

가장 실질적인 함정이다. 앞서 본 대로 함수 타입은 반환 공변성을 **공짜로** 갖지만, `fun interface`는 직접 선언해야 한다. `out P`를 안 붙이면 불변(invariant)이 되어, **이미 `Assembler<InquiryClient<REQ, RES>>`로 타입이 정해진 값을** `Assembler<InquiryClient<*, *>>` 자리에 넣을 때 타입 에러가 난다.

다만 생략이 항상 깨지는 건 아니다. 위 예시처럼 `Assembler { m, g -> ... }`로 그 자리에서 직접 만들면 타입 인자가 대입 대상에서 추론될 수 있어 `out` 없이도 통과할 수 있다. 확실히 필요해지는 쪽은 **이미 타입이 붙은 값을 넘길 때**다.

### 6.2 항목별 비교

| 항목 | typealias | fun interface |
|------|---|---|
| 타입의 정체 | `Function2` 그 자체 (별칭) | 독립된 명목 타입 |
| 같은 시그니처 함수 대입 | 무조건 가능 | SAM 변환 필요, 다른 SAM과 호환 안 됨 |
| 오버로드 구분 | 불가 | 가능 |
| 파라미터 이름 | **가능** — `(i: Int) -> Boolean`처럼 이름을 적을 수 있다 | 가능 |
| 전용 확장 함수 | 불가 (붙이면 원 타입 전체에) | 가능 |
| 생성자 참조 `::Name` | 불가 | 가능 (1.6.20~) |
| 공변성 | 공짜 | `out` 직접 선언 필요 |
| 호출 | `invoke(m, g)` | `assemble(m, g)` — 의미가 드러남 |
| 기본 구현·인터페이스 상속 | 불가 | 가능 |
| 선언 비용 | 1줄 | 3줄 (+ JVM에서는 인터페이스 하나) |
| Java에서 보임 | X (`Function2`로 보임) | O |

마지막 두 줄은 JVM 기준이다.

### 6.3 이 사례에서의 손익

**얻는 것**은 호출 이름과 확장 여지다. `assemble(errorMapper, callGuard)`는 `invoke(...)`보다 하는 일이 드러나고, 나중에 조립 단계에 기본 구현을 얹을 자리가 생긴다.

여기서 흔한 오해 하나를 짚어야 한다. **"파라미터 이름을 주려면 fun interface여야 한다"는 말은 사실이 아니다.** 함수 타입 별칭도 이름을 가질 수 있고, 공식 문서 자신이 그렇게 쓴다.

```kotlin
typealias IntPredicate = (i: Int) -> Boolean
```

따라서 가독성만으로는 승격의 근거가 되지 않는다.

**얻지 못하는 것**은 타입 안전성이다. 이 별칭은 `private`이고 생성 지점이 빌더 안 제네릭 메서드로 완전히 닫혀 있다. 엉뚱한 함수가 흘러들 경로가 애초에 없으니, 명목 타입으로 바꿔도 막아줄 것이 없다.

그래서 이 코드에서는 typealias 유지가 낫다. `fun interface`로 승격할 가치가 생기는 시점은 따로 있다.

1. **이 타입이 파일 밖으로 나갈 때** — 외부 모듈이 직접 구현해 넘기는 구조가 되면, 시그니처만 맞으면 통과하는 별칭보다 명목 타입이 안전하다.
2. **같은 시그니처의 다른 개념이 생겨 오버로드가 필요할 때** — 별칭끼리는 확장 후 시그니처가 같아 충돌한다.
3. **그 계약에만 붙는 확장 함수나 기본 구현이 필요할 때** — 공식 문서가 fun interface의 고유 능력으로 지목하는 항목이다.

## 7. 무엇을 언제 쓰는가

세 선택지를 한 축에 놓으면 판단이 쉬워진다.

| 목적 | 선택 |
|------|------|
| 긴 타입 이름을 줄이고 싶다 | typealias |
| 함수 타입에 역할 이름을 주고 싶다 | typealias |
| 값을 다른 값과 섞이지 않게 하고 싶다 | value class |
| 구현체를 외부가 제공하고, 이름 있는 계약이 필요하다 | fun interface |
| 그 계약에만 붙는 확장 함수·기본 구현이 필요하다 | fun interface |

typealias를 타입 안전성 목적으로 쓰려 한다면 그건 거의 항상 `value class` 자리다. 반대로 `fun interface`를 단지 이름 하나 붙이려고 쓴다면 typealias로 충분한 경우가 많다.

## 8. 요약

| 항목 | 내용 |
|------|------|
| 정체 | 새 타입이 아닌 별칭. 컴파일 시점에 원래 타입으로 치환 |
| 선언 위치 | 최상위 + 중첩(2.3.0 Stable). 로컬 스코프만 불가 |
| 타입 파라미터 | variance·상한 모두 불가 |
| 주 용도 | 긴 제네릭·함수 타입 축약, 이름 충돌 해소, actual typealias |
| 안 되는 것 | 타입 안전성, 오버로드 구분, 확장 함수 범위 한정 |
| 숨은 쓸모 | 함수 타입의 반환 공변성을 빌려 제네릭 타입 인자를 닫아두기 |
| fun interface와의 차이 | 명목 타입 여부, 전용 확장·기본 구현, `out` 명시 필요 여부 |
| 대체 후보 | 타입 안전성이 목적이면 value class |

typealias는 "타입 체계를 건드리지 않고 이름만 준다"는 점이 약점이자 강점이다. 약점 쪽만 보면 쓸모없는 기능처럼 보이지만, 5장처럼 **타입을 바꾸지 않아야 성립하는 자리**가 분명히 있다. 별칭에 무엇을 기대하고 있는지부터 구분하면 typealias·value class·fun interface 중 무엇을 꺼낼지는 자연히 정해진다.
