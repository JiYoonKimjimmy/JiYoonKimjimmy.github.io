---
layout: post
title : Redis Architecture
date  : 2025-02-12
image : redis.jpg
tags  : redis redis-architecture
---

# Redis 아키텍처

- Replication 아키텍처
- Sentinel 아키텍처
- Cluster 아키텍처

![redis-architecture](/images/redis-architecture.png)

---

## Replication 아키텍처

- 기본적인 **`Master - Slave`** 구조
- `Master` 노드에서 모든 쓰기 작업 수행
- `Slave` 노드는 읽기 작업만 수행

#### 장점

- 향상된 읽기 성능 보장

#### 단점

- **수동 Failover 작업 필요**

---

## Sentinel 아키텍처

- 여러 개의 `Master - Slave` 구조의 Redis 인스턴스를 **`Sentinel`** **중앙 관리자**가 관리하는 아키텍처

### Sentinel 노드

- `Sentinel` 노드는 Redis 서버와 별도로 실행되는 프로세스
- 여러 개의 `Sentinel` 노드로 구성하여 Redis 인스턴스를 모니터링 및 장애 감지
- `Sentinel` 노드의 장애 대응을 위해 3개 이상 노드로 구성 필요
- `Master` 노드가 장애 발생 시, `Slave > Master` 승격 작업 **자동 처리**
  - `Master` 노드 장애 발생 시점, `Sentinel` 노드 중 과반수 이상의 동의(Quorum)되어야 Failover 작업 수행

#### 장점

- **자동 Failover 작업 처리가 가능하여 HA(High Availability) 보장**

#### 단점

- `Sentinel` 노드 수에 따라 장애 감지 및 복구 불안정
- 복잡한 설정 및 과도한 리소스 확보 필요

---

## Cluster 아키텍처

- *Reids 3.0 버전* 이후 제공하는 `Cluster` 기능
- 여러 개의 Redis 노드가 서로 통신하며 **HA** 를 유지/보장하는 `Cluster` 구성
- 같은 `Cluster` 구성된 Redis 노드는 `Master` or `Slave` 역할 분담
- 각 Redis 노드는 각자의 특정 범위의 `Hash Slot` 을 가지고 있어, 데이터 분산 처리 가능
  - `Hash Slot` 은 각 Redis 노드에 할당된 데이터 범위를 나타내는 개념
  - Redis Cluster 는 총 16384개의 `Hash Slot` 보유
  - 각 Redis 노드는 자신의 `Hash Slot` 범위 내에서 데이터 저장 및 조회 처리

### Cluster 아키텍처 작동 방식

1. 데이터 파티셔닝
  - `Cluster` 는 Key 를 `Hash Slot` 에 할당하여 데이터를 분산 저장 처리
  - 각 `Master` 노드는 특정 `Hash Slot` 을 소유하여, 해당 슬롯의 데이터를 처리
2. 자동 장애 조치(Failover)
  - `Master` 노드가 장애 발생 시, `Slave` 노드가 `Master` 노드로 승격되어 장애 조치 처리
3. 수평적 확장(Scale-out)
  - `Cluster` 는 수평적으로 확장 가능하여, 데이터 처리 능력 향상
  - 새로운 노드가 추가된 경우, 기존 노드로부터 `Hash Slot` 을 재분배받아 데이터 관리
4. 데이터 일관성
  - `Cluster` 는 데이터 일관성을 유지하기 위해, 데이터 복제 및 장애 조치 기능 제공
  - 각 `Master` 노드는 자신의 `Hash Slot` 을 복제하여 `Slave` 노드에 전달
  - 장애 발생 시, `Slave` 노드가 `Master` 노드로 승격되어 데이터 일관성 유지

#### 장점

- 수평적 확장을 통한 대규모 데이터 처리 & 높은 성능 지원 가능
- 자동 장애 조치 기능으로 높은 가용성 제공
- 데이터 파티셔닝 기능을 통해 부하 분산 처리 & 성능 최적화

#### 단점

- 복잡한 설정 및 과도한 리소스 확보 필요
- 데이터 일관성 모델이 최종 일관성으로, 즉각적인 일관성이 필요한 서비스 특성에는 부적합

---

#### Reference

- [NHN FORWORD 2021 레디스 야무지게 사용하기!](https://www.youtube.com/watch?v=92NizoBL4uA)
- [banggeunho.log - 레디스 알고 쓰자. - 정의, 저장방식, 아키텍처, 자료구조, 유효 기간](https://velog.io/@banggeunho/%EB%A0%88%EB%94%94%EC%8A%A4Redis-%EC%95%8C%EA%B3%A0-%EC%93%B0%EC%9E%90.-%EC%A0%95%EC%9D%98-%EC%A0%80%EC%9E%A5%EB%B0%A9%EC%8B%9D-%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98-%EC%9E%90%EB%A3%8C%EA%B5%AC%EC%A1%B0-%EC%9C%A0%ED%9A%A8-%EA%B8%B0%EA%B0%84)

---