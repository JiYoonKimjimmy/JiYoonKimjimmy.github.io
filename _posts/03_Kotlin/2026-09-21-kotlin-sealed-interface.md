---
layout: post
title : Kotlin sealed interface 정리
date  : 2026-09-21
image : kotlin-overview.png
tags  : kotlin sealed sealedclass sealedinterface exhaustive
---

## 목차

1. [sealed란 무엇인가](#1-sealed란-무엇인가)
2. [sealed class와 sealed interface의 차이](#2-sealed-class와-sealed-interface의-차이)
3. [봉인의 범위와 제약](#3-봉인의-범위와-제약)
4. [when exhaustiveness와의 관계](#4-when-exhaustiveness와의-관계)
5. [enum과의 관계](#5-enum과의-관계)
6. [data object로 상태 없는 케이스 표현](#6-data-object로-상태-없는-케이스-표현)
7. [언제 sealed interface를 쓰는가](#7-언제-sealed-interface를-쓰는가)
8. [요약](#8-요약)

---

## 1. sealed란 무엇인가

`sealed`는 클래스·인터페이스의 **하위 타입 집합을 컴파일 시점에 고정**하는 제어자다. `open`/`abstract`/`final`과 같은 자리에 오는 상속 제어자이지, 가시성 제어자가 아니다.

```kotlin
sealed interface PaymentResult

data class Approved(val txId: String) : PaymentResult
data class Declined(val reason: String) : PaymentResult
object Pending : PaymentResult
```

봉인의 값은 사실 하나로 요약된다. 하위 타입 집합이 닫혀 있으면 컴파일러가 `when`의 **exhaustiveness(망라성) 검사**를 해줄 수 있다는 것이다.

## 2. sealed class와 sealed interface의 차이

Kotlin 1.5.0에서 `sealed`가 인터페이스로 확장되면서 생긴 차이가 세 가지 있다.

### 2.1 다중 구현 가능 여부

클래스는 단일 상속이지만 인터페이스는 다중 구현이 가능하다. 그래서 `sealed interface`를 쓰면 한 타입이 **여러 봉인 계층에 동시에 소속**될 수 있다.

```kotlin
sealed interface Failure
sealed interface Retryable

data class Timeout(val elapsedMs: Long) : Failure, Retryable
data class Rejected(val field: String) : Failure
```

`Timeout`은 `Failure`이면서 동시에 `Retryable`이다. 서로 직교하는 분류축을 각각 봉인해두고 한 타입이 여러 축에 걸치게 설계할 수 있다는 뜻이다. `sealed class`로는 불가능한 조합이다.

### 2.2 생성자 유무

`sealed class` 자체는 항상 abstract이고 직접 인스턴스화할 수 없다. 생성자를 가질 수는 있는데, 가시성이 `protected`(기본) 또는 `private` 둘뿐이다. `public`·`internal` 생성자는 컴파일 에러다.

```kotlin
sealed class Shape(val id: String) {
    // public constructor(id: String) : this(id) // 컴파일 에러
}
```

`sealed interface`에는 애초에 생성자라는 개념이 없으니 이 제약 자체가 없다.

### 2.3 컴파일 표현 방식

`PermittedSubclasses` JVM 속성이 방출되기 전(구버전 컴파일 타깃 기준), 두 봉인의 보호 방식이 서로 다르다. `sealed class`의 봉인은 protected/private 생성자라는 **런타임 장치**로 뒷받침되지만, `sealed interface`는 애초에 그런 장치가 없어서 **일반 인터페이스처럼 컴파일**되고 컴파일러·IDE 검사에만 의존한다. 즉 sealed interface 쪽이 봉인의 "강제력"이 상대적으로 약했던 셈인데, PermittedSubclasses 속성이 방출되기 시작한 뒤로는 두 경우 모두 JVM 레벨에서 봉인 정보를 갖게 됐다.

| 항목 | sealed class | sealed interface |
|------|:---:|:---:|
| 다중 상속 | X (단일 상속) | O (다중 구현 가능) |
| 생성자 | protected/private만 가능 | 생성자 없음 |
| 인스턴스 | 항상 abstract, 직접 생성 불가 | 해당 없음 |

## 3. 봉인의 범위와 제약

봉인의 제약은 **모듈 + 패키지** 단위다. 같은 모듈·패키지 밖에서는 새 하위 타입을 만들 수 없다. 1.5.0부터는 같은 패키지의 여러 파일에 나눠 선언할 수 있게 완화됐다(그 전에는 같은 파일 강제).

가장 놓치기 쉬운 지점은, 이 제약이 **직접 하위 타입에만** 걸린다는 것이다.

```kotlin
sealed interface Error
sealed class IOError() : Error   // Error를 확장하지만 자신도 sealed — 봉인이 이어짐
open class CustomError() : Error // open — 여기서부터 다시 열림
```

계층 중간에 `open`을 두면 그 지점 아래로는 봉인이 없다. 반대로 중간에 `sealed`를 하나 더 두면 봉인 자체는 이어지지만, 그 중간 노드는 exhaustiveness 검사의 케이스로 세어지지 않는다(아래 4장).

멀티플랫폼 프로젝트에는 한 겹 제약이 더 있다. 원칙적으로 직접 하위 타입은 같은 source set에 있어야 하는데, `expect`/`actual` 조합에는 예외가 있어서 양쪽 모두 자기 source set에 하위 타입을 둘 수 있다. 대신 common 코드의 `when`은 그 대가로 `else` 분기가 여전히 필요하다 — actual 플랫폼 구현이 common에서는 알 수 없는 하위 타입을 추가할 수 있기 때문이다.

## 4. when exhaustiveness와의 관계

exhaustive란 "분기가 대상 값의 모든 경우를 덮었는가"다. 이게 성립하려면 컴파일러가 값의 집합을 **열거**할 수 있어야 하는데, `sealed`·`enum`·`Boolean`이 이 조건을 만족한다.

```kotlin
fun describe(result: PaymentResult): String = when (result) {
    is Approved -> "승인: ${result.txId}"
    is Declined -> "거절: ${result.reason}"
    Pending -> "대기중"
    // else 없이도 컴파일 통과 — 모든 케이스를 다뤘기 때문
}
```

여기서 실무 함정이 두 가지 있다.

**① 검사 대상은 "직접 하위 타입"이 아니라 "직접 non-sealed 하위 타입"이다.** 3장의 `IOError`처럼 중간에 `sealed`를 또 둔 노드는 케이스로 안 세어진다. 그 아래 리프 타입들이 각각 케이스가 된다.

**② `else`를 한 번 붙이면 그 `when`의 exhaustiveness 검사는 영구히 꺼진다.** 새 케이스를 추가해도 컴파일러가 조용히 넘어가고, 새 타입은 소리소문없이 `else`로 흘러간다.

```kotlin
// 나쁜 예 — else가 새 케이스 누락을 가려버림
when (result) {
    is Approved -> "승인"
    else -> "기타"
}

// 좋은 예 — 케이스를 나열하면 새 케이스 추가 시 계속 컴파일 에러로 잡힘
when (result) {
    is Approved -> "승인"
    is Declined, Pending -> "미완료"
}
```

여러 케이스를 하나로 묶고 싶다면 `else` 대신 케이스를 나열하는 편이 안전하다. 그래야 나중에 `PaymentResult`에 새 하위 타입이 추가됐을 때 컴파일 에러로 바로 드러난다.

참고로 이 검사는 컴파일 시점의 **근사**일 뿐이다. 분리 컴파일 때문에, 모듈 경계를 넘어 공개한 봉인 계층에 하위 타입을 추가하면 재컴파일하지 않은 소비자 코드가 런타임에 `NoWhenBranchMatchedException`을 만날 수 있다. 라이브러리로 공개하는 sealed 계층이라면 이 점을 염두에 둘 필요가 있다.

## 5. enum과의 관계

sealed와 enum은 대체재가 아니라 보완재다. enum 상수는 인스턴스가 하나뿐이지만, sealed 하위 타입은 여러 인스턴스를 가질 수 있다.

상속 방향은 비대칭이다. `enum class`는 다른 클래스를 확장할 수 없으므로 `sealed class`는 상속이 불가능하지만, **`sealed interface`는 구현할 수 있다**.

```kotlin
sealed interface ErrorType
enum class NetworkError : ErrorType { TIMEOUT, DNS_FAILURE }

fun handle(e: ErrorType) = when (e) {
    is NetworkError -> "네트워크 오류"   // 엔트리를 나열하지 않고 한 줄로 덮어도 exhaustive
}
```

이 조합이 쓰이면 enum의 모든 엔트리가 exhaustiveness 분석 대상에 통째로 편입된다. 그래서 `is NetworkError ->` 한 줄로 덮어도 되고, `TIMEOUT`/`DNS_FAILURE`를 각각 나열해서 덮어도 된다.

## 6. data object로 상태 없는 케이스 표현

sealed 계층에서 상태가 없는 케이스는 `object` 대신 `data object`(1.9.0 Stable)를 쓰는 게 좋다.

```kotlin
sealed interface UiState
data class Success(val data: List<String>) : UiState
data class Error(val message: String) : UiState
data object Loading : UiState
```

`data class`와 평범한 `object`가 섞인 계층에서는 `toString()` 출력이 한쪽만 클래스명+해시로 튀는 비대칭이 생긴다. `data object`를 쓰면 `toString()`/`equals()`/`hashCode()`가 자동으로 생겨서 이 비대칭이 사라진다.

## 7. 언제 sealed interface를 쓰는가

세 가지 상황에 적합하다.

- **하위 타입 집합이 미리 정해져 있고 고정적일 때** — 결제 수단, API 응답 타입처럼 "이게 전부다"라고 말할 수 있는 경우.
- **타입 안전한 상태 관리가 필요할 때** — UI 상태(`Loading`/`Success`/`Error`)처럼 상태 전이를 컴파일러가 강제해주길 원하는 경우.
- **공개 API를 닫힌 형태로 유지하고 싶을 때** — 라이브러리 사용자가 임의로 하위 타입을 추가하지 못하게 막아, 라이브러리 쪽에서 모든 케이스를 통제하고 싶은 경우.

반대로 **케이스가 계속 늘어나고 제3자가 구현체를 추가해야 하는 문제**(플러그인, 핸들러 등록 같은)라면 sealed가 아니라 SPI 같은 열린 확장점이 맞다. sealed는 "새 케이스 추가는 비용이 있지만 누락 지점을 컴파일러가 다 찾아준다"는 트레이드오프를 의도적으로 택하는 것이고, 열린 계층은 그 반대다. 둘 중 뭘 고를지는 "이 타입 집합이 앞으로도 닫혀 있을 것인가"라는 질문 하나로 갈린다.

## 8. 요약

| 항목 | 내용 |
|------|------|
| 목적 | 하위 타입 집합을 컴파일 시점에 고정해 `when` exhaustiveness를 가능하게 함 |
| sealed class 대비 장점 | 다중 구현 가능(여러 봉인 계층에 동시 소속) |
| sealed class 대비 차이 | 생성자 없음, (구버전 컴파일 타깃 기준) 보호 장치가 컴파일러·IDE 검사에 의존 |
| 봉인 범위 | 같은 모듈 + 패키지, 직접 하위 타입에만 적용 |
| exhaustiveness 함정 | `else`를 붙이면 검사가 영구히 꺼짐 — 케이스 나열을 우선 고려 |
| enum과의 관계 | `enum class`가 `sealed interface`는 구현 가능(상속은 불가) |
| 적합한 상황 | 하위 타입 집합이 고정적이고, 타입 안전한 분기가 중요하고, 공개 API를 닫힌 형태로 유지하고 싶을 때 |

`sealed interface`는 "닫힌 세계를 선언하는 대가로 컴파일러의 도움을 받는" 기능이다. 계층이 계속 열려 있어야 하는 문제에 쓰면 오히려 발목을 잡으니, 하위 타입 집합이 정말 고정적인지부터 먼저 따져보는 게 순서다.
