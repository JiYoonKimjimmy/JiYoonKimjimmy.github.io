---
layout: post
title : Java Skill UP 을 하자! step.2
date  : 2021-07-21
image : springwebflux.png
tags  : java oop reactive webflux
---



## Java Skill UP !! step.2
`Functional Programming` 을 통해서 함수형 프로그래밍이 무엇인지 알겠는데, 최종적으로 알고 싶은 `Reactive` 는 무엇일까.<br>
`Reactive` 를 알기 위해선 또 먼저 `Async(비동기)` 와 `Non-blocking(논블로킹)` 의 이해가 필요하다. 그래서 이번에는 `Async & Non-blocking` 부터 `Reactive Programming` 까지 살펴볼 계획이다.

- Async & Non-blocking
- Reactive Programming (with `RxJava`)

---

### Synchronous vs Asynchronous
*동기식 (Sync) 작업* 은 서로 다른 2개의 작업 주체가 각자의 작업 스케쥴에 따라 영향을 받는 방식인 반면에,<br>
*비동기식 (Async) 작업* 은 2개의 작업 주체가 서로의 작업 시작/종료 여부와 관계없이 각자만의 시작/종료 시간을 가질 수 있는 방식이다.
<br>
![sync vs async](/images/syncvsasync.jpeg)

### Blocking vs Non-Blocking
**Blocking** 은 첫번째 작업을 수행 중 두번째 작업이 들어온 경우, 첫번째 작업 진행은 두번째 작업이 시작되고 완료할 때까지 대기하다가 처리하는 방식이다.

![blocking](/images/blocking.png)

**Non-blocking** 은 다른 주체의 작업에 관계없이 자신의 작업 진행을 계속 하는 것이다. 첫번째 작업 중 두번째 작업의 요청이 와도 첫번째 작업은 계속 진행된다. 그 이후 두번째 작업이 완료가 되었을 때, *Call-back* 응답을 통해서 완료 처리가 된다.

![non-blocking](/images/non-blocking.png)

---

### Reactive Programming
`Reactive`라는 의미는 변화에 대한 반응 중심의 프로그래밍은 **데이터의 흐름을 먼저 정의하고 데이터가 변경되었을 때 연관된 작업을 실행** 하는 방식의 프로그래밍이다.
기능을 직접 정해서 실행하는 것이 아니라, 이벤트가 발생하였을 때 기능이 실행되어 처리되는 방식으로 기존의 ***PULL*** 방식을 프로그래밍에서 ***PUSH*** 방식의 프로그래 개념으로 바뀌어야한다.
<br>
`Java` 에서는 `Reactive` 방식의 프로그래밍을 위해서는 함수형 프로그래밍이 필요하다. `Call-back` 과 `Observer` 패턴은 서버가 1개이거나 단일 Thread 환경일 경우 적합하지만, Multi-Thread 환경일 경우에는 `Dead-lock` 또는 동기화 문제가 발생할 수 있다.
<br>
함수형 프로그래밍은 순수 함수를 지향하기 때문에 Multi-Thread 환경에서도 안전하다. 

### RxJava


---

#### 출처
- [토리맘의 한글라이즈 프로젝트 Spring Web on Reactive Stack](https://godekdls.github.io/Reactive%20Spring/contents/)
- [DevEric 동기/비동기와 블로킹/논블로킹](https://deveric.tistory.com/99)
- [HERSTORY [RxJava] RxJava 이해하기 - 1. Reactive Programming 이란](https://4z7l.github.io/2020/12/01/rxjava-1.html)
