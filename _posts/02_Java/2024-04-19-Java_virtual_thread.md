---
layout: post
title : Java Virtual Thread
date  : 2024-04-19
image : java_virtual_thread.png
tags  : java virtualthread virtual-thread
---

## Java 의 Virtual Thread 가상-스레드

`Virtual Thread` 가상-스레드는 JDK 19에서 도입된 새로운 스레드 모델로, 기존의 `Platform Thread` 플랫폼-스레드와는 달리 **경량화된 스레드**이다.
이는 수천 개의 스레드를 효율적으로 관리할 수 있도록 설계되었고, 처리량이 높은 **Concurrent** 동시성 작업이 필요한 애플리케이션에서 스레드 생성 비용을 줄일 수 있다.

### Java 의 Platform Thread

`Platform Thread` 플랫폼-스레드는 자바의 기본 스레드 모델로, 운영체제의 스레드와 1:1 매핑되어 작업에 필요한 
스레드를 생성하고, 실행하고, 제어하는 등의 작업을 수행한다.

스레드는 생성 비용이 높으며, 많은 수의 스레드를 생성하면 성능 저하와 메모리 부족 이슈가 발생할 수 있다.

#### Context Switching

`Context Switching` 컨텍스트 스위칭은 운영체제가 하나의 CPU에서 여러 스레드를 실행할 때 발생하는 과정이다. 
운영체제는 현재 실행 중인 스레드의 상태를 저장하고, 다음에 실행할 스레드의 상태를 복원하는 작업을 수행한다. 

이 과정은 CPU 자원을 소모하며, 많은 수의 스레드가 존재할 경우 성능 저하를 초래할 수 있다.

---

### Virtual Thread

`Virtual Thread` 가상-스레드는 `Platform Thread` 플랫폼-스레드와는 달리 경량화된 스레드이다. 

#### Virtual Thread 는 Context Switching 비용이 훨씬 낮다. 

가상-스레드는 자바 런타임이 관리하며, **운영체제의 스레드와 1:N 매핑되어** 하나의 플랫폼-스레드가 **여러 개의 가상-스레드를 실행**할 수 있다. 
가상-스레드는 더 많은 수의 스레드를 효율적으로 관리할 수 있으며, 컨텍스트-스위칭 비용을 줄일 수 있다.

#### Virtual Thread 는 블로킹 작업을 수행할 때도 효율적으로 동작한다. 

기존의 플랫폼-스레드는 블로킹 작업을 수행할 때 스레드가 블로킹 상태로 전환되지만, 
가상-스레드는 블로킹 작업을 수행할 때 **다른 가상-스레드로 전환되어** 자원을 효율적으로 사용할 수 있다.

#### Virtual Thread 는 특히 I/O 작업이 많은 애플리케이션에서 유용하다. 

예를 들어, 웹 서버와 같은 애플리케이션은 많은 수의 클라이언트 요청을 처리해야 하며, 
**각 요청마다 스레드를 생성하는 대신 가상-스레드를 사용하면** 더 많은 요청을 효율적으로 처리할 수 있다.

---

### Virtual Thread 동작 원리

---

### Virtual Thread 장점

---

### Virtual Thread 사용 방법

---

#### 출처

- [Oracle - Virtual Threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html#GUID-2BCFC2DD-7D84-4B0C-9222-97F9C7C6C521)
- [네이버 D2 - Virtual Thread의 기본 개념 이해하기](https://d2.naver.com/helloworld/1203723)
- [우아한기술블로그 - Java의 미래, Virtual Thread](https://techblog.woowahan.com/15398/)
- [LinkedIn - Java 21 Virtual Threads](https://www.linkedin.com/pulse/java-21-virtual-threads-filipe-sanches-geniselli-ymrnf/)

---
