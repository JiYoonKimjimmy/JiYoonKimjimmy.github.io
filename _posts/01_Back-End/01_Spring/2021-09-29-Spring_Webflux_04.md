---
layout: post
title : Spring Webflux Reactor - Mono
date  : 2022-10-25
image : springreactor_mono.png
tags  : java spring webflux mono
---

## Mono
Spring Webflux 의 Reactive Stream 를 구현하고 있는 Reactor 는 **`Mono`** 와 **`Flux`** 이다.

그 중 **`Mono`** 는 **0 ~ 1 개의 데이터를 전달** 하는 역할을 한다.

### Mono 인스턴스 생성 방식
#### Just
![Mono just 생성 방식](/images/springreactor_mono_just.png)

- `.just()` 는 구독하는 순간. 특정 값을 바로 반환 `emit` 하는 함수이다.
- *인스턴스화 되는 시점(`instantiation time`)* 에 캡쳐되는 값을 반환한다. (`eager` 방식과 유사)

#### Defer
![Mono defer 생성 방식](/images/springreactor_mono_defer.png)

- `.defer()` 는 `Mono Supplier` 를 인자로 받아 구독하는 순간, 해당 Supplier 를 구독하여 `emit` 하는 함수이다.
- `.just()` 와의 차이점은 해당 값이 *인스턴스화 되는 시점* 은 **구독하는 순간** 이다.

#### Callable
![Mono callable 생성 방식](/images/springreactor_mono_callable.png)

- `.fromCallable()` 는 `Callable Supplier` 를 인자로 받아 `Mono Supplier` 인스턴스를 생성하는 함수이다.
- 생성된 `Mono Supplier` 를 구독하는 순간, `Callable Supplier` 를 통해 **단순 값** 을 제공받아 `emit` 한다.

---

{% highlight kotlin %}
var data = 99;
private fun getData(caller: String): Int {
    println("Called by $caller")
    return data
}

@Test
fun monoInstantiationTest() {
    println("==========================")
    println(getData("HI"))
    println("==========================")

    val just = Mono.just(getData("JUST"))
    val defer = Mono.defer { Mono.just(getData("DEFER")) }
    val callable = Mono.fromCallable { getData("CALLABLE") }

    just.subscribe { println("JUST data : $it") }
    defer.subscribe { println("DEFER data : $it") }
    callable.subscribe { println("CALLABLE data : $it") }

    println("==========================")

    // changed value
    println("Changed value $data -> ${++data}")

    println("==========================")

    just.subscribe { println("JUST data : $it") }
    defer.subscribe { println("DEFER data : $it") }
    callable.subscribe { println("CALLABLE data : $it") }

    println("==========================")
}
{% endhighlight %}

{% highlight logging %}
==========================
Called by HI
99
==========================
Called by JUST
JUST data : 99
Called by DEFER
DEFER data : 99
Called by CALLABLE
CALLABLE data : 99
==========================
Changed value 99 -> 100
==========================
JUST data : 99
Called by DEFER
DEFER data : 100
Called by CALLABLE
CALLABLE data : 100
==========================
{% endhighlight %}

---

#### 출처
- [Spring Webflux 공식 레퍼런스 - Web on Reactive Stack](https://docs.spring.io/spring-framework/docs/5.2.6.RELEASE/spring-framework-reference/web-reactive.html)
- [KimDoubleB - Reactor just, defer, fromCallable 에 대하여](https://binux.tistory.com/135)
