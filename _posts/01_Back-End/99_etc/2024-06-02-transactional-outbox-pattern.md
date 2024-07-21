---
layout: post
title : Transactional Outbox Pattern
date  : 2024-06-02
image : transactional-outbox-pattern.jpg
tags  : transactional-outbox-pattern
---

## Transaction Outbox Pattern 무엇인가?

`Event Driven Architecture` 기반 서비스는 메시지 발행을 통해 다른 서비스와 통신한다.
**Message Broker** 를 통해 다양한 메시지를 발행(Publish)하고, 해당 메시지를 수신(Consume)하면서 관련된 연관 작업을 처리하게 된다.

하지만 비동기적으로 처리되는 `Event Driven Architecture` 는 **DB 트랜잭션**에 대한 `Atomicity 원자성` 을 보장하지 못할 수도 있다.

결제 승인을 하는 서비스에서 결제 승인 완료 처리 후 결제 승인 완료 메시지를 발행하는 경우, 
해당 메시지 발행이 실패하게 된다면 결제 승인 완료에 대한 **트랜잭션은 롤백이 되어야 할 것이다.** 
그렇지만, 보통 DBMS 와 다른 기종으로 구축된 메시지 브로커에서 해당 트랜잭션 롤백 처리하는 것은 쉽지 않을 수 있다.

이를 해결하기 위한 방법으로 `Transactional Outbox Pattern` 을 사용하게 된다.
`Transactional Outbox Pattern` 은 간단하게 설명한다면, **Message Broker 메시지 발행 처리를 DB 트랜잭션에 포함하여 원자성을 보장하는 패턴**을 의미한다.

> `Outbox` 는 보통 **보낼 편지함**이란 의미를 가지고 있다.

---

### Concept

`Transactional Outbox Pattern` 은 다음과 같은 개념을 가지고 있다.

![Transaction Outbox Pattern](/images/transactional-outbox-pattern-01.png)

1. `Order Service` 를 통해서 `ORDER` 테이블에 주문 정보가 저장된다.
2. 동시에 `OUTBOX` 테이블에도 주문 완료된 정보 메시지를 생성하여 저장한다.
3. `Message Relay` 서비스를 통해 `OUTBOX` 테이블 저장 정보를 읽어 메시지 발행한다.
4. 메시지 발행이 성공하면 `OUTBOX` 테이블에서 해당 데이터를 삭제한다.
5. 만약 메시지 발행이 실패하게 되면 `OUTBOX` 테이블에서 해당 데이터를 삭제하지 않고 보관한다.
6. 이후 `Message Relay` 서비스가 다시 실행될 때, 메시지 발행이 실패한 데이터를 다시 발행한다.
7. 이러한 과정을 반복하면서 메시지 발행이 성공하게 된다.

---

`Transactional Outbox Pattern` 을 구현할 수 있는 방법은 대표적으로 2가지가 있다.

- `Polling Publisher Pattern` **폴링 발행기 패턴**
- `Transaction Log Tailing Pattern` **트랜잭션 로그 테일링 패턴**

---

#### `Polling Publisher Pattern` 폴링 발행기 패턴

`Polling Publisher Pattern` 은 간단한 구현 방법으로 발행 메시지가 저장된 `OUTBOX` 테이블을 폴링하면서 조회하여 메시지를 발행하는 방법이다.

하지만, DBMS `Polling` 조회하는 비용이 생각보다 높은 단점이 있다.

![Polling Publisher Pattern](/images/polling-publishier-pattern.png)

---

#### `Transaction Log Tailing Pattern` 트랜잭션 로그 테일링 패턴

`Transaction Log Tailing Pattern` 은 높은 레벨의 구현 방법을 가지고 있지만, 성능 측면의 장점을 가지고 있다.

이름 그대로 **`DBMS` 의 트랜잭션 커밋 로그**를 테일링하여 하나씩 메시지를 발행하는 방법이다.
해당 패텬을 구현하기 위해서는 로그의 변경 데이터를 감지하는 **`CDC(Change Data Capture)`** 를 구현해야 한다.

![Transaction Log Tailing Pattern](/images/transaction-log-tailing-pattern.png)

> `CDC` 을 구현해주는 툴을 이용하는 것도 하나의 방법일 것이다.
> 예를 들어, `Debezium` 을 이용하여 `CDC` 를 구현할 수 있다.

---

### Conclusion

시스템 디자인을 하다보면 동기적 트랜잭션을 처리에 대한 문제를 맞닿을 때가 많다.

시스템 가용성을 고려한다면 비동기 서비스를 구축 방안이 좋은 선택인데, 트랜잭션의 원자성을 보장하기 위해서는 비동기 시스템 아키텍처 구축에 대한 딜레마에 빠지게 되었다.

`Transactional Outbox Pattern` 은 이러한 딜레마를 해결할 수 있는 해결책이 될 것 같다.

---

#### 출처

- [Microservices.io - Pattern: Transactional outbox](https://microservices.io/patterns/data/transactional-outbox.html)
- [Tech Primers Youtube - Transactional Outbox Pattern](https://www.youtube.com/watch?v=M-Fhb8LzhPo)
- [RIDI - Transactional Outbox 패턴으로 메시지 발행 보장하기](https://ridicorp.com/story/transactional-outbox-pattern-ridi/)
- [eastperson - Transaction Outbox Pattern 알아보기](https://velog.io/@eastperson/Transaction-Outbox-Pattern-%EC%95%8C%EC%95%84%EB%B3%B4%EA%B8%B0)

---