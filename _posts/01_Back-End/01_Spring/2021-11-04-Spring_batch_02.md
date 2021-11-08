---
layout: post
title : Spring Batch step.2 - Job & Step
date  : 2021-11-04
image : spring-batch-stereotypes.png
tags  : spring springbatch kotlin
---

## Job
### Configuring a Job
`Job` 은 보통 `Step` 을 가지고 있으며, 구현하기 위해서는 `JobRepository` 가 필요하다. 그리고 `JobBuilderFactory` 를 통해서 아래와 같은 설정을 할 수 있다.

- `Restartability` : Batch 의 `Job` 을 실행할 때 재시작과 관련된 동작을 설정한다.
- `Intercepting Job Execution` : `JobListener` 를 통하여 `Job` 실행 전/후 시점에 특정 동작을 설정한다.
- `JobParametersValidator` : `Runtime` 시점에 `JobParameters` 의 검증을 하는 `Validator` 를 설정한다. `Job` 의 실행에 꼭 필요한 `Parameter` 를 검증하는데 유용하다.




---

#### 출처
- [jojoldu 님의 Spring Batch 가이드](https://jojoldu.tistory.com/324?category=902551)
- [Spring Batch 공식 Reference](https://docs.spring.io/spring-batch/docs/4.2.x/reference/html/index-single.html#spring-batch-intro)
