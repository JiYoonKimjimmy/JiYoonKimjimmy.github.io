---
layout: post
title : Spring Webflux step.1
date  : 2021-08-31
image : springwebflux_01.jpg
tags  : java spring webflux
---

## Spring Webflux
**Spring Framework5** 에서 추가된 모듈로서, `Reactive Programming` 의 `Server`, `Client` Application 개발 API 라고 할 수 있다. `Non-blocking` 과 `Reactive Stream` 을 지원해주고 있기 때문에 비동기적인 프로그램 개발이 가능하다.

- Reactive Stream ? `Subscriber` 가 `Publisher` 의 데이터 발행 속도를 제어하는 것

### Reactive API
`Reactive Stream` 은 Component 간 상호 작용에서 중요한 역할을 하지만, Application 개발 `API` 로 사용하기엔 저수준이다. 비동기 로직을 개발하기 위해선 고수준 함수형 `API` 가 필요하였고, **Webflux** 에서 선택한 것은 **Reactor** 라는 `Reactive library` 이다.<br>
**Reactor** 는 `Mono` 와 `Flux` 라는 `API` 타입을 제공하고 있고, 모든 연산자는 `Non-blocking(BackPressure)` 을 지원한다.

### Webflux 의 Programming Models
**Spring Webflux** 는 크게 2가지의 프로그래밍 모델을 지원한다.

- Annotated Controllers : **Spring WebMvc** 와 동일한 모듈에 있는 `Annotation` 을 사용한다. `WebMvc` 와 `Webflux` 모두 `Reactive` 타입의 데이터를 반환할 수 있게 지원하고 있기 때문에 구분하기 어렵지만, `Webflux` 에서는 `@RequestBody` 로도 `Reactive` 타입의 데이터를 요청 받을 수 있다.
- Functional Endpoints : `Lambda` 기반 함수형 프로그래밍 모델으로 요청을 `Routing` 해주는 **Router** 를 이용한 함수형 방식이다.




---

#### 출처
- [토리맘의 한글라이즈 프로젝트 - Spring Web on Reactive Stack](https://godekdls.github.io/Reactive%20Spring/springwebflux/)
