---
layout: post
title : Spring Webflux step.2
date  : 2021-09-17
image : springwebflux_02.jpg
tags  : java spring webflux
---

> [Spring Webflux step.1](/2021/08/31/Spring_Webflux_01)

## Reactive Core
**Spring Webflux** 에 대해서는 간략하게 정리를 해보았고, **Webflux** 에서는 `Reactive Web Application` 를 만들기 위해서는 **Reactive Core** 가 어떻게 구성되어있는지 살펴볼 필요가 있다.

- HttpHandler
- WebHandler
- Filters
- Exceptions
- Codecs
- Logging

---

### HttpHandler
**HttpHandler** 는 요청과 응답을 처리하는 메소드를 하나만 가지고 있으며, 유일한 역할은 여러 HTTP Server API 를 추상화한 것이다.

| HTTP Server | 사용하는 API |
| :---: | :---: |
| Netty | Netty API |
| Undertow | Undertow API |
| Tomcat | Servelt 3.1 Non-blocking I/O; `ByteBuffer` 로 `byte[]` 읽고 쓰는 Tomcat API |
| Jetty | Servelt 3.1 Non-blocking I/O; `ByteBuffer` 로 `byte[]` 읽고 쓰는 Jetty API |
| Servelt 3.1 Container | Servelt 3.1 Non-blocking I/O |

### WebHandler
**WebHandler** 는 `Web Application` 에서 사용하는 광범위한 기능들을 제공하고 있다.

- User Session & Session Attributes
- Request Attributes
- Locale, Principle
- Form data parsing, Cache
- Multipart data
- etc ..

### Filters
**WebFilter** 를 사용하면, 다른 `Filter Chain` 과 함께 `WebHandler` 전후로 요청을 가로채 원하는 로직을 처리할 수 있다. `Filter` 등록은 `Bean` 으로 생성하여 등록 가능하다.

### Exceptions
**WebExceptionHandler** 는 로직상 발생하는 에러를 처리해주며, `Bean` 생성하고 등록하여 사용 가능하다.

### Codecs
`Spring-web`, `Spring-core` 를 활용하면 `Reactive Non-blocking` 방식으로 바이트 컨텐츠를 고수준의 객체로 변환(직렬화, 역직렬화)시킬 수 있다.

- `Encoder`, `Decoder` - HTTP 와 관련 없는 컨텐츠 처리
- `HttpMessageReader`, `HttpMessageWriter` - HTTP 메시지를 encoding, decoding 처리
- `EncoderHttpMessageWriter`, `DecoderHttpMessageReader` - `Encoder`, `Decoder` 를 감싸고 있는 `Web Application` 에서 사용하는 객체

#### `Spring-core` 구현체 Module
- `byte[]`
- `ByteBuffer`
- `DataBuffer`
- `Resource`
- `String`

#### `Spring-web` 구현체 Module
- `Jackson JSON`
- `Jackson Smile`
- `JAXB2`
- `Protocol Buffers`
- `Form` & `Multipart` & `SSE` 등 처리하는 Web 전용 HTTP message 처리 reader/writer 제공

### Logging
**Webflux** 에서는 `DEBUG` 레벨의 로그로 꼭 필요한 정로만 로그로 담고 있기 때문에 읽기 편하게 설계되어있다고 한다.

#### LogId
`Reactor` 기반인 **Webflux** 에서는 단일 Thread 를 운영하기 때문에 *Thread ID 만 봐서는 로그 분석 파악하기 어려운 점이 있다.* 그래서 **Webflux** 에서는 로그 메시지마다 *요청 LogId* 를 추가하였다.

---

## DispatcherHandler
**Webflux** 도 **WebMvc** 와 비슷한 `Front Controller Pattern` 구조를 가지고 있다. 중앙 `WebHandler` 가 요청을 받고 실제 처리를 위한 각 `Component` 객체의 역할을 위임하는 패턴 구조이다. 


---

#### 출처
- [토리맘의 한글라이즈 프로젝트 - Spring Web on Reactive Stack](https://godekdls.github.io/Reactive%20Spring/springwebflux/)
