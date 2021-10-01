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
