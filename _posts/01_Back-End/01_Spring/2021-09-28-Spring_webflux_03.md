---
layout: post
title : Spring Webflux step.3 - Functional Endpoints
date  : 2021-09-28
image : springwebflux_03.png
tags  : java spring webflux functional-endpoints
---

## Functional Endpoints
**Webflux** 의 `API` 를 구현함에 있어 *2가지* 방식이 있다.

- `@Contoller` 어노테이션을 사용하는 **Annotated Contollers 방식**
- `Router` 를 활용한 **Functional Endpoints 방식**

**Annotated Contollers 방식** 은 `WebMVC` 에서 자주 사용하고 있는 방식이기 때문에, 이왕 배워볼 `Webflux` 에서는 새로운 방식인 **Functional Endpoints 방식** 에 대해서 학습해볼 계획이다.<br>
살짝 살펴봤을 때는, `Router` 라는 객체를 사용해서 매핑되는 `URL` 를 알맞는 `Handler` 로 라우팅해주는 것으로 보이고, 이러한 방식은 `Front-end Framwork` 들에서 자주 사용하는 구조로 생각된다.<br>
<br>
`Webflux` 에서 경량화된 함수형 프로그래밍 모델인 **Webflux.fn** 을 지원한다. **Webflux.fn** 모델은 요청을 함수로 라우팅하고, 핸들링하기 때문에 불변성(immutablitity)를 보장한다.<br>
<br>
**Webflux.fn** 모델에서는 요청으로 `ServerRequest` 객치를 받아 비동기 `ServerResponse` 를 반환하는 구조이며, `RouterFunction` 에서 요청에 대한 처리를 위해 `HandlerFuction` 에 라우팅한다. (`RouterFunction` 이 `@RequestMapping` 과 동일한 역할)
