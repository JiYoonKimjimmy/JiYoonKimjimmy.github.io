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
- Reactive Programming `Spring Webflux`

---

### 동기 vs 비동기
*동기식 작업* 은 서로 다른 2개의 작업 주체가 각자의 작업 스케쥴에 따라 영향을 받는 방식인 반면에,<br>
*비동기식 작업* 은 2개의 작업 주체가 서로의 작업 시작/종료 여부와 관계없이 각자만의 시작/종료 시간을 가질 수 있는 방식이다.
<br>
![sync vs async](/images/syncvsasync.jpeg)

### Blocking vs Non-Blocking
*Blocking* 은 첫번째 작업을 수행 중 두번째 작업이 들어온 경우, 첫번째 작업 진행은 두번째 작업이 시작되고 완료할 때까지 대기하다가 처리하는 방식이다.<br>
*Non-blocking* 은 다른 주체의 작업에 관계없이 자신의 작업 진행을 계속 하는 것이다.
<br>
![blocking vs non-blocking](/images/blockingvsnonblocking.png)

---

### Reactive Programming
`Reactive`라는 의미는 변화에 대한 반응 중심의 프로그래밍 방식이다.

---

#### 출처
- [토리맘의 한글라이즈 프로젝트 Spring Web on Reactive Stack](https://godekdls.github.io/Reactive%20Spring/contents/)
- [DevEric 동기/비동기와 블로킹/논블로킹](https://deveric.tistory.com/99)
