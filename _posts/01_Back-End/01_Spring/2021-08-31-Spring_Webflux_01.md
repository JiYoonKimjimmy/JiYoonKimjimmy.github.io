---
layout: post
title : Spring Webflux step.1 - Overview
date  : 2021-08-31
image : springwebflux_01.jpg
tags  : java spring webflux
---

## Spring Webflux 에 대해서
**Spring Framework5** 에서 추가된 모듈로서, `Reactive Programming` 의 `Server`, `Client` Application 개발 API 라고 할 수 있다. `Non-blocking` 과 `Reactive Stream` 을 지원해주고 있기 때문에 비동기적인 프로그램 개발이 가능하다.

- Reactive Stream ? `Subscriber` 가 `Publisher` 의 데이터 발행 속도를 제어하는 것

### Reactive API
`Reactive Stream` 은 Component 간 상호 작용에서 중요한 역할을 하지만, Application 개발 `API` 로 사용하기엔 저수준이다. 비동기 로직을 개발하기 위해선 고수준 함수형 `API` 가 필요하였고, **Webflux** 에서 선택한 것은 **Reactor** 라는 `Reactive library` 이다.<br>
**Reactor** 는 `Mono` 와 `Flux` 라는 `API` 타입을 제공하고 있고, 모든 연산자는 `Non-blocking(BackPressure)` 을 지원한다.

### Webflux 의 Programming Models
**Spring Webflux** 는 크게 2가지의 프로그래밍 모델을 지원한다.

- Annotated Controllers : **Spring WebMvc** 와 동일한 모듈에 있는 `Annotation` 을 사용한다. `WebMvc` 와 `Webflux` 모두 `Reactive` 타입의 데이터를 반환할 수 있게 지원하고 있기 때문에 구분하기 어렵지만, `Webflux` 에서는 `@RequestBody` 로도 `Reactive` 타입의 데이터를 요청 받을 수 있다.
- Functional Endpoints : `Lambda` 기반 함수형 프로그래밍 모델으로 요청을 `Routing` 해주는 **Router** 를 이용한 함수형 방식이다.

### Applicability
![spring webflux applicability](https://godekdls.github.io/images/reactivespring/spring-mvc-and-webflux-venn.png)

- **Webflx** 는 이미 **Spring MVC** 로 정상적으로 동작하고 있는 프로그램을 **굳이 변경할 필요는 없다** 고 한다.
- `Non-blocking` 방식의 Web Stack 이나 `Functional` Web Framework 를 찾고 있다면 **Webflux** 를 선택이 도움이 된다고 말한다.
- **Spring MVC** 와 **Webflux** 모두 `Annotation` 기반의 프로그래밍이 가능하고, 서로 조합하여 구성이 가능하기 때문에 따로 학습이 없더라도 적용이 가능하다.
- **Webflux** 로도 `Blocking` 방식의 API 호출이 가능하나, 이는 `Non-blocking` Web Stack 활용이 어렵다.
- **Spring MVC** 프로그램에서 외부 서비스를 호출하는 경우, 먼저 **Reactive WebClient** 부터 적용해볼 수 있다. 이는 `Flux` 와 `Mono` 타입의 데이터를 반환되며, `Reactive Programming` 에 대한 이해의 시작이 될 것이다.

### Servers
**Webflux** 는 Tomcat, Jetty, Servelt 3.1+ Container 에서도, Servelt 기반이 아닌 Netty, Undertow 환경에서도 동작한다. Spring Boot 의 Starter 에서는 기본으로 **Netty** 로 설정하고 있는데, **Netty** 는 `Async` & `Non-blocking` 을 많이 사용하고, 서버와 클라이언트가 리소스를 공유할 수 있다는 이유이다.

### Concurrency Model
**Webflux** 는 실행중인 `Thread` 가 중지되지 않는 특징이 있기 때문에, *WebMvc 보다 작은 Thread Pool 를 가지게 된다.*

#### Mutable State
`Reactor` 와 `RxJava` 에서 로직은 연산자로 표현하고, 연산자를 사용하면 `Runtime`에 분리된 환경에서 `Reactive` 파이프라인을 만들어 데이터를 순차적으로 처리 가능하다. 파이프라인 안에 있는 코드는 절대 동시에 실행될 수 없어서 `Mutable State` **(상태 공유)** 를 신경쓰지 않아도 된다.

#### Threading Model
- **Webflux** 는 최소한의 설정을 하면 서버는 `Thread` 한 개를 운영하고, 보통은 CPU 코어 수만큼의 `Thread` 로 요청을 처리할 수 있다.
- `Reactive WebClient` 는 이벤트 루프를 사용하고 있으며, 적은 `Thread` 를 고정하여 사용한다. 단, 클라이언트와 서버에서 모두 `Reactor Netty` 를 사용한다면 이벤트 루프 리소스를 공유한다.
- `Reactor` & `RxJava` 는 `Scheduler` 라는 추상화된 `Thread Pool` 전략을 제공한다. 코드에서 전략을 설정할 수 있는 것을 알 수 있다.

#### Configuring
**Webflux** 설정은 각 서버에 맞는 설정 API 참고 또는 `Spring Boot` 옵션을 통해 설정할 수 있다.

---

#### 출처
- [Spring Webflux 공식 레퍼런스 - Web on Reactive Stack](https://docs.spring.io/spring-framework/docs/5.2.6.RELEASE/spring-framework-reference/web-reactive.html)
- [토리맘의 한글라이즈 프로젝트 - Spring Web on Reactive Stack](https://godekdls.github.io/Reactive%20Spring/springwebflux/)
