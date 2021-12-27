---
layout: post
title : Java Skill UP 을 하자! step.2 - Reactive Programming
date  : 2021-07-21
image : java_02.jpg
tags  : java async non-blocking reactive-programming rxjava
---

`Functional Programming` 을 통해서 함수형 프로그래밍이 무엇인지 알겠는데, 최종적으로 알고 싶은 `Reactive Programming` 이란 무엇일까.<br>
`Reactive` 를 알기 위해선 또 먼저 `Async(비동기)` 와 `Non-blocking(논블로킹)` 의 이해가 필요하다. 그래서 이번에는 `Async & Non-blocking` 부터 간단하게 `Reactive Programming` 까지 살펴볼 계획이다.

- Async & Non-blocking
- Reactive Programming (with `RxJava`)

---

### Synchronous vs Asynchronous
**동기식 (Sync) 작업** 은 서로 다른 2개의 작업 주체가 각자의 작업 스케쥴에 따라 영향을 받는 방식인 반면에,<br>
**비동기식 (Async) 작업** 은 2개의 작업 주체가 서로의 작업 시작/종료 여부와 관계없이 각자만의 시작/종료 시간을 가질 수 있는 방식이다.
<br>
![sync vs async](/images/syncvsasync.jpeg)

### Blocking vs Non-Blocking
**Blocking** 은 첫번째 작업을 수행 중 두번째 작업이 들어온 경우, 첫번째 작업 진행은 두번째 작업이 시작되고 완료할 때까지 대기하다가 처리하는 방식이다.<br>
<br>
**Non-blocking** 은 다른 주체의 작업에 관계없이 자신의 작업 진행을 계속 하는 것이다. 첫번째 작업 중 두번째 작업의 요청이 와도 첫번째 작업은 계속 진행된다. 그 이후 두번째 작업이 완료가 되었을 때, *Call-back* 응답을 통해서 완료 처리가 된다.

![blocking and non-blocking](/images/blocking-and-non-blocking.png)

<small>[사진 출처 : DevEric 동기/비동기와 블로킹/논블로킹](https://deveric.tistory.com/99)</small>

---

#### 다양한 조합 방식
![total thread io process](/images/total_thread_io_process.png)

---

### Reactive Programming
*Reactive* 라는 의미는 변화에 대한 반응 중심의 프로그래밍은 **데이터의 흐름을 먼저 정의하고 데이터가 변경되었을 때 연관된 작업을 실행** 하는 방식의 프로그래밍이다.
기능을 직접 정해서 실행하는 것이 아니라, 이벤트가 발생하였을 때 기능이 실행되어 처리되는 방식으로 기존의 ***PULL*** 방식을 프로그래밍에서 ***PUSH*** 방식의 프로그래 개념으로 바뀌어야한다.<br>
<br>
`Java` 에서는 `Reactive` 방식의 프로그래밍을 위해서는 함수형 프로그래밍이 필요하다. `Call-back` 과 `Observer` 패턴은 서버가 1개이거나 단일 Thread 환경일 경우 적합하지만, Multi-Thread 환경일 경우에는 `Dead-lock` 또는 동기화 문제가 발생할 수 있다.<br>
<br>
그에 반해, 함수형 프로그래밍은 **순수 함수를 지향하기 때문에** Multi-Thread 환경에서도 안전하다. 그래서 `Java` 에서 `Reactive Programming` 을 하기 위해선 함수형 프로그래밍의 지원이 필요하다.

#### Thread Pool vs Event Loop
`Java` 에서의 `Thead` 관리 방식은 보통 `Thread Pool` 방식으로 많이 사용하고 있을 것이다.

![Blocking Thread Pool](/images/blocking-thread-pool.jpg)

> `Thread Pool` 관련 좋은 블로그 : [https://www.wrapuppro.com/programing/view/jAuG3VNBCbGnQWU](https://www.wrapuppro.com/programing/view/jAuG3VNBCbGnQWU)

`Thread Pool` 의 관리 방식으로 많은 어플리케이션이 `Thread` 생성하는 부하를 줄이고, 성능을 높일 수 있었지만, `blocking I/O` 를 사용한다는 점에서 미세한 성능 차이가 일어날 수 있다.

##### Reactive Programming 의 `Thread` 관리 방식
Reactive Programming 는 **단일 `Thread`** 방식으로 관리가 된다. (단일 `Thread` 방식이라고 하지만 `Core`수대로 `Thread` 를 가지고 있다.)

**먼저, 멀티 `Thread` 방식이 아닌 이유는** 데이터의 변경을 감지하고 동작하는 함수형 프로그래밍에서는 멀티 `Thread` 일 경우, 데이터가 공유되어 잘못된 결과가 처리될 수 있는 위험성이 있기 때문이다. 이런 현상을 부수 효과 **Side effect** 라고 한다.

같은 자원을 여러 `Thread` 에서 경쟁 조건(Race condition)에 빠지게 되면 결과가 꼬이고, 디버깅에도 어려움을 겪게 된다. 함수형 프로그래밍에서 **순수 함수** 가 조건에 해당되는 이유가 위와 같은 에러 상황을 방지하기 위함이다.

단일 `Thread` 조건인 Reactive Programming 이 고성능 프로그램을 구현하기 위해 **Event Loop** 모델 방식을 도입하게 되었다.

**Event Loop** 모델 방식은 *요청(Event)* 를 *Event Queue* 에 적재를 하고, *Event Loop* 의 알고리즘에 의해 함수 호출/처리 후 `Callback` 응답 처리한다.

![Event Loop](/images/event-loop.jpg)

> Spring Webflux & Evnet Loop 방식 이해를 위한 좋은 블로그 : [https://devahea.github.io/2019/04/21/Spring-WebFlux는-어떻게-적은-리소스로-많은-트래픽을-감당할까](https://devahea.github.io/2019/04/21/Spring-WebFlux%EB%8A%94-%EC%96%B4%EB%96%BB%EA%B2%8C-%EC%A0%81%EC%9D%80-%EB%A6%AC%EC%86%8C%EC%8A%A4%EB%A1%9C-%EB%A7%8E%EC%9D%80-%ED%8A%B8%EB%9E%98%ED%94%BD%EC%9D%84-%EA%B0%90%EB%8B%B9%ED%95%A0%EA%B9%8C/)

---

### RxJava
`RxJava` 는 2013년 `Neflex` 넷플릭스사의 기술 블로그에서 처음 소개되었다. 넷플릭스에서 `RxJava` 를 개발하게 된 이유로 3가지를 밝혔는데,

##### 1. 동시성을 적극적으로 끌어안을 필요가 있다. (Embrace Concurrency)
서비스 계층에서 동시성 처리를 위해 `client` 의 요청을 처리할 때, 다수의 비동기 실행 흐름을 생성하고 결과를 취합하여 최종 응답 처리하는 방식

##### 2. Java `Future` 를 조합하기 어렵다는 점을 해결해야 한다. (Java Futures are Expensive to Compose)
2013년 당시 `Java8` 에는 `CompletableFuture` 같은 class 가 지원되지 않았기 때문에 비동기 흐름을 조합할 방법을 찾기 위한 `RxJava` 의 `Operator` 라는 리액티브 연산자를 개발

##### 3. `Callback` 방식의 문제점을 개선해야 한다. (Callbacks Have Their Own Problems)
`Callback hell` 의 문제를 해결하기 위해 `Callback` 사용하지 않는 방향으로 설계

##### RxJava 살짝 맛보기
{% highlight groovy %}
implementation 'io.reactivex.rxjava2:rxjava:2.2.21'
{% endhighlight %}

{% highlight java %}
public static void main(String[] args) {
    emit();
}

public static void emit() {
    /*
     * Observable.class : 데이터의 변화가 발생하는 data source
     * just() : 기본적인 Observable 선언 방식으로, data source 에 String 정보 2개를 발행
     * subscribe() : Observable 를 구독하며 발행된 데이터를 소비
     */
    Observable
            .just("Hello", "RxJava")
            .subscribe(System.out::println);
    // out : Hello
    // out : RxJava
}
{% endhighlight %}

##### RxJava 학습 단계
1. `Observable` Class 이해
2. `map()`, `filter()`, `reduce()`, `flatMap()` 함수의 사용법
3. 생성 연산자, 결합 연산자, 변환 연산자 등 카테고리별 주요 함수 이해
4. 스케줄러의 의미, `subscribeOn()` 과 `observeOn()` 함수의 차이를 알아둔다.
5. 그 밖의 디버깅, 흐름 제어 함수 이해

---

#### 출처
- [DevEric 동기/비동기와 블로킹/논블로킹](https://deveric.tistory.com/99)
- [HERSTORY [RxJava] RxJava 이해하기 - 1. Reactive Programming 이란](https://4z7l.github.io/2020/12/01/rxjava-1.html)
- [길은 가면, 뒤에 있다. - [RxJava] RxJava 프로그래밍(1) - 리액티브 프로그래밍](https://12bme.tistory.com/570)
