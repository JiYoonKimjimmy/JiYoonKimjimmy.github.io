---
layout: post
title : Spring Batch step.3 - Step
date  : 2021-11-15
image : spring-batch-step-basic.png
tags  : spring springbatch kotlin step
---

## Step
`Spring Batch` 에서 실질적으로 데이터를 처리하는 단계인 `Step` 에 대해서 알아보자.
`Step` 의 데이터 처리 방식은 3단계로 나눠진다.

1. `ItemReader`
2. `ItemProcessor`
3. `ItemWriter`

위와 같은 3개의 단계를 거쳐 데이터를 **1. DB 또는 파일에서 읽고, 2. 가공/처리하고, 3. DB 또는 파일에 데이터를 저장**하는 프로세스를 가지게 된다.

---

### Chunk-Oriented Processing
`Step` 에 대해 학습하기 앞서 **Chunk-Oriented** 에 대한 이해가 필요하다.
Chunk-Oriented Processing 은 데이터를 한번에 하나씩 읽어와서 가공/처리 후 트랜잭션에 의해 쓰여지기 전까지 하나의 `Chunk` 를 만드는 것을 의미한다.

1. `ItemReader` 에서 데이터를 하나 읽어오고 `ItemProcessor` 로 전달된다.
2. `ItemProcessor` 에서 데이터를 가공 및 처리한다.
3. `ItemProcessor` 의 데이터는 목록화하여 합쳐진다.
4. 읽어온 데이터의 개수가 `commit interval` 과 같아지면, `ItemWriter` 가 `Chunk` 전체를 `write` 하고, 트랜잭션은 `commit` 된다.

![Chunk Oriented Processing](/images/spring-batch-chunk-oriented-processing.png)

---

### Configuring a Step



---

#### 출처
- [jojoldu 님의 Spring Batch 가이드](https://jojoldu.tistory.com/324?category=902551)
- [Spring Batch 공식 Reference](https://docs.spring.io/spring-batch/docs/4.2.x/reference/html/index-single.html#spring-batch-intro)
