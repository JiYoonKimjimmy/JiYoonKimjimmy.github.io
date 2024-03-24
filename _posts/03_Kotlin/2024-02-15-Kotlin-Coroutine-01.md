---
layout: post
title : Coroutine (feat. Kotlin)
date  : 2024-02-15
image : kotlin_coroutines.png
tags  : coroutine 코루틴
---

`Coroutine 코루틴` 은 동시성 처리를 위한 비즈니스 로직에서 많이 사용되고 있는 프로그래밍 기법이라 할 수 있다.

코루틴은 코틀린 언어뿐만 아니라 Go-lang, C++, Javasacript 등 많은 언어에서 지원하고 있고, 코틀린 진영에서도 많은 개발자들이
적극 활용하여 비동기 프로그래밍을 구현하고 있다.

> [당근마켓 밋업 - Kotlin Coroutines 톺아보기](https://www.youtube.com/watch?v=eJF60hcz3EU)

---

## Coroutine 코루틴에 대해서

먼저 코루틴은 간단히 정리해보자면, **프로세스를 일시 정리할 수 있도록 하는 하나의 함수이다.**
프로세스를 일시 정지한다는 의미는 실행되고 있는 서브 루틴을 **일시 정지**하고 다시 **재개**할 수 있다고 할 수 있다.

`Thread-1` 에서 실행되고 있는 `task-1` 이 **일시 중단**되면, `Thread-1` 은 다른 `task-2` 를 수행할 수 있고,
`task-1` 의 나머지 일은 `Thread-2` 에서 다시 **재개**할 수 있다.

코루틴은 이와 같은 성질을 **Suspension** 이라고 한다.

> `task-1` 작업이 일시 중단되는 예시는, DB 조회, 외부 API 연동 등 외부와의 통신과 같은 작업이 될 수 있다.

위처럼 코루틴을 사용하면 `Multi-Thread 멀티-스레드` 환경에서 `Thread 스레드` 에 대한 I/O 작 처리를 극대화할 수 있다.

---



---

#### 출처

- [나무위키 - 코루](https://namu.wiki/w/%EC%BD%94%EB%A3%A8%ED%8B%B4)
- [Ecem Okan - Introduction to Kotlin Coroutines](https://medium.com/@ecemokan/introduction-to-kotlin-coroutine-458353fb4c70)

---