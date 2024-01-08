---
layout: post
title : Kotlin - takeIf & takeUnless 함수
date  : 2024-01-05
image : kotlin-overview.png
tags  : kotlin takeIf takeUnless
---

## Kotlin 의 `.takeIf()` & `.takeUnless()` 함수

> Kotlin `v1.9.22` 버전 기준 블로그 작성

### `.takeIf()`

**`takeIf()` 함수**는 프로그래밍 역사상 오래 전부터 즐겨 사용하는 `if` 문과 같은 객체의 상태를 체크하는 로직에서 대체하여 활용할 수 있다.

`if` 문에 들어가는 조건식을 `takeIf()` 함수에서는 람다 파라미터로 전달되는데,

- **람다 조건식이 `true` 라면, 수신 객체를 반환**하고,
- **람다 조건식이 `false` 라면, `null` 을 반환**한다.

{% highlight java %}
// 일반 if 문
if (condition) {
    doSomething()
}
// takeIf() 문
takeIf { condition }?.apply { doSomething() }
{% endhighlight %}


### `.takeUnless()`

**`takeUnless()` 함수**는 `takeIf()` 함수의 **반대 동작**하는 함수이다. 

- **람다 조건식이 `true` 라면, `null` 을 반환**하고,
- **람다 조건식이 `false` 라면, 수신 객체를 반환**한다. 

---

### `takeIf()` 함수의 내부

{% highlight java %}
/**
 * Returns `this` value if it satisfies the given [predicate] or `null`, if it doesn't.
 *
 * For detailed usage information see the documentation for [scope functions](https://kotlinlang.org/docs/reference/scope-functions.html#takeif-and-takeunless).
 */
@kotlin.internal.InlineOnly
@SinceKotlin("1.1")
public inline fun <T> T.takeIf(predicate: (T) -> Boolean): T? {
    contract {
        callsInPlace(predicate, InvocationKind.EXACTLY_ONCE)
    }
    return if (predicate(this)) this else null
}
{% endhighlight %}

해당 코드에서도 확인할 수 있듯이 `if` 문을 통해서 아래와 같이 동작한다.

- `predicate(this) == true` 인 경우, `this` 반환
- `predicate(this) == false` 인 경우, `null` 반환

---

### `takeIf()` 함수 활용

`Kotlin` 을 활용하다보면, 불변 또는 가변 제약으로 인해 `if` 문을 통한 객체 변환 로직에서 불가피하게 또다른 빈 객체가 필요할 때가 있다.

`takeIf()` 함수를 잘 사용하면 위와 같은 상황 또는 복잡한 `if` 문에 대한 가독성을 높일 수 있는 리팩토링이 가능할 것 같다.

#### 기본 함수 사용법

{% highlight java %}
@Test
fun `takeIf() 함수 기본 사용법`() {
    val status = true
    takeIf { status }?.apply { println("Hello World") }
    takeIf { false }?.apply { println("Hello World") } ?: println("Hell World")

    val str = "Hello !"
    str.takeIf { status }?.apply { println("$this World !") }

    val str2 = ""
    str2.takeIf { it.isNotEmpty() }?.apply { println("$this World !!") } ?: println("Nothing !!")
}
{% endhighlight %}

###### Output
{% highlight text %}
Hello World
Hell World
Hello ! World !
Nothing !!
{% endhighlight %}

#### 확장 함수 활용한 사용법

{% highlight java %}
@Test
fun `String 확장 함수 이용한 takeIf() 함수 사용법`() {
    stringTakeIfWithWorld("Hello")?.apply { println(this) }
    stringTakeIfWithWorld(null)?.apply { println(this) } ?: println("Nothing !!")
}

private fun stringTakeIfWithWorld(str: String?): String? {
    return str?.takeIf { "Hello" == str }?.add("World")
}

private fun String.add(str: String): String {
    return "$this $str"
}
{% endhighlight %}

###### Output
{% highlight text %}
Hello World
Nothing !!
{% endhighlight %}

> `String` 예시 외에도 다양한 객체를 활용할 수 있을 것이다.
> 몇가지 예시는 Kotin 공식 홈페이지에서도 확인 가능하다.
> 
> [Kotlin.org - takeIf and takeUnless](https://kotlinlang.org/docs/scope-functions.html#takeif-and-takeunless)

---

#### 정리

중첩 `if` 문 또는 복잡한 조건으로 인한 가독성 저하되는 `if` 문에 대해서 항상 고민을 가지고 있었다.

`takeIf()` 문을 잘 활용한다면, 위와 같은 고민을 조금 더 해결할 수 있을 것 같은 느낌이다. 😁

---

#### 출처
- [Kotlin.org - takeIf and takeUnless](https://kotlinlang.org/docs/scope-functions.html#takeif-and-takeunless)
- [GM.Lim - 코틀린 의 takeIf, takeUnless 는 언제 사용하는가?](https://medium.com/@limgyumin/%EC%BD%94%ED%8B%80%EB%A6%B0-%EC%9D%98-takeif-takeunless-%EB%8A%94-%EC%96%B8%EC%A0%9C-%EC%82%AC%EC%9A%A9%ED%95%98%EB%8A%94%EA%B0%80-f6637987780)
