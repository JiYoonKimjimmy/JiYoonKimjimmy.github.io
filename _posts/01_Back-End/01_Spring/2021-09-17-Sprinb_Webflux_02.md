---
layout: post
title : Spring Webflux step.2
date  : 2021-09-17
image : springwebflux_02.png
tags  : java spring webflux reactive-core dispatcherhandler
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
**WebExceptionHandler** 는 로직상 발생하는 에러를 처리해주며, `Bean` 생성하고 등록하여 사용 가능하다.

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
**Webflux** 도 **WebMvc** 와 비슷한 `Front Controller Pattern` 구조를 가지고 있고, 중앙 `WebHandler` 가 요청을 받고 실제 처리를 위한 각 `Component` 객체의 역할을 위임하는 패턴 구조로 구성되었다.

### Special Bean Types
**Webflux** 의 `DispatcherHandler` 가 요청을 처리하고 그에 맞는 응답을 생성할 때 사용하는 `Bean` 들이 있다. 이 `Special Bean` 이란, **Webflux** 를 동작하는데 필요하고, **Spring** 이 관리하는 `Object` 객체들이다.

#### Special Beans

| Bean | 내용 |
| :---: | --- |
| `HandlerMapping` | 요청을 `Handler` 에 맞게 `Mapping` 한다. `HanlderMapping` 구현체마다 다르긴 하지만, 주로 `@RequestMapping` 을 선언한 함수를 찾는 `RequestMappingHandlerMapping` 객체, 함수형 End-Point 를 Rounting 해주는 `RouterFunctionMapping` 객체, URI path 패턴으로 `WebHandler` 를 찾는 `SimpleUrlHandlerMapping` 등이 있다. |
| `HandlerAdapter` | `DispatcherHandler` 가 어떤 `Handler` 를 받든지 실행할 수 있도록 도와주는 역할을 하고 있다. |
| `HandlerResultHandler` | `Handler` 로부터 받은 결과를 처리하고 응답을 종료한다. |

### Processing
`DispatcherHandler` 가 요청을 처리하는 방식은,

1. `HandlerMapping` 에서 요청에 `Mapping` 되는 `Handler` 를 찾는다. 제일 첫 번째로 일치한 `Handler` 를 사용한다.
1. `Handler` 를 찾으면 적당한 `HandlerAdapter` 를 사용해서 `Handler` 를 실행하고, `HandlerResult` 를 반환 받는다.
1. `HandlerResult` 를 적절한 `HandlerResultHandler` 에 전달하고, 응답 처리를 완료한다.

### Result Handling
`HandlerAdapter` 에서 `Handler` 실행을 완료하면, 결과와 `Context` 정보를 감싸고 있는 `HandlerResult` 를 반환한다. `HandlerResult` 는 `HandlerResultHandler` 가 받아서 요청의 응답을 처리 완료한다.

#### `HandlerResultHandler` 구현체

| Result Handler Type | Return Values | 비고 |
| :---: | --- | --- |
| `ResponseEnityResultHandler` | `ResponseEntity` | `@Controller` 에서 주로 사용 |
| `ServerResponseResultHandler` | `ServerResponse` | 함수형 End-Point 에서 주로 사용 |
| `ResponseBodyResultHandler` | `@ResponseBody` | `@RestController` 에서 주로 사용 |
| `ViewResolutionResultHandler` | `CharSequence`, `View`, `Model`, `Map`, `Rendering` .. | `Object` 를 `Model attribute` 로 처리 |

### View Resolution
`View Resolution` 은 특정 `View` 기술에 국한되지 않고, `HTML` Template 이나 Model 을 사용해 브라우저에 Rendering 하는 기법이다. **Webflux** 는 `HandlerResultHandler` 에서 `ViewResolver` 객체를 사용해서 `View` 의 논리적인 이름을 나타내는 `String` 과 `View` 객체를 Mapping 한다.

---

## Summary
**Webflux** 에는 **Spring WebMVC** 의 `DispatcherServlet` 과 같은 기능의 `DispatcherHandler` 객체를 사용하여 `Handler` 가 요청을 받고, 응답을 처리를 하고 있다. 이런 기본적인 동작 원리를 알고 있다면, 개발할 때 발생하는 에러들을 차근차근 분석해볼 수 있는 기초 지식이 될 것 같다.

---

#### 출처
- [Spring Webflux 공식 레퍼런스 - Web on Reactive Stack](https://docs.spring.io/spring-framework/docs/5.2.6.RELEASE/spring-framework-reference/web-reactive.html)
- [토리맘의 한글라이즈 프로젝트 - Spring Web on Reactive Stack](https://godekdls.github.io/Reactive%20Spring/springwebflux/)
