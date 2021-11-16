---
layout: post
title : Spring Batch step.1 - Overview
date  : 2021-11-04
image : spring-batch-logo.png
tags  : spring springbatch kotlin
---

## Batch application 의 필요성
`Spring Batch` 를 살펴보기 전에 **"Batch application 은 왜 필요할까?"** 를 당연 먼저 생각해볼 필요가 있다.
**Batch application** 은 아래와 같은 조건의 상황을 개발할 때 효율적인 서비스라고 생각한다.

- 대용량 데이터 처리 : 1만건, 10만건 등 대용량의 데이터를 조회/전달/계산 등 처리가 필요한 경우
- 자동화 : 일정 주기적으로 동일한 작업이 필요한 경우

## Spring Batch?
`Spring Batch` 는 `Spring` 의 특성을 가지고 있으면서 `Batch` 작업을 할 수 있도록 지원하는 Framework 이다. 그리고 다양한 라이브러리를 지원하면서 DB, 전문 파일 등 다양한 데이터 형식의 정보를 조회 및 가공 처리가 가능하다.
이번 `Spring Batch` 를 정리하면서는 아직도 현업에서 많이 사용중인 전문 형식의 데이터를 처리하는 방식을 살펴보고 기록할 예정이다.

#### Dependencies
- Spring Batch
- H2 Database

> Database 의존성 필수? Spring Batch 는 Batch Job 에 대한 설정을 DB 에 Meta Table 을 생성하여 관리하고 있기 때문에 필수이다.

#### Overview
`Spring Batch` 의 간단한 구현 방식이다. `(with Kotlin)`

{% highlight kotlin %}
@EnableBatchProcessing
@Configuration
class BatchJobConfig(
    val jobBuilderFactory: JobBuilderFactory,
    val stepBuilderFactory: StepBuilderFactory
) {
    // logger
    private val logger = LoggerFactory.getLogger(BatchJobConfig::class.java)

    @Bean
    fun simpleJob(): Job = jobBuilderFactory.get("simpleJob").start(simpleStep()).build()

    @Bean
    fun simpleStep(): Step = stepBuilderFactory
                                .get("simpleStep")
                                .tasklet { contribution, chunkContext ->
                                    logger.info("==== This is Step !! ====")
                                    RepeatStatus.FINISHED
                                }
                                .build()
}
{% endhighlight %}

간단한 `simpleJob` 를 실행하여 `simpleStep` 에서 실행되는 아래와 같은 로그를 확인해볼 수 있다.

![spring Batch basic 01 테스트 결과](/images/spring-batch-basic-01.png)

### Job
![Job 구조](/images/job-hierarchy.png)

`Job` 은 **Batch 작업 하나의 단위**이고, `Job` 안에서 여러 개의 `Step` 들이 구성되어 Batch 작업을 수행한다. `Spring Batch` 구조에서 제일 큰 개념은 `Job` 이라 할 수 있다.

#### JobInstance
`JobInstance` 는 논리적인 `Job` 를 의미한다. 실제 1일 1회 실행되는 `Job` 이 존재한다면, 매일 1회 실행되는 새로운 `Job` 이 생성이 될 것이고, 이를 관리하고자 논리적인 `JobInstance` 가 필요하다.
따라서 각 `JobInstance` 는 여러 개의 실행 결과인 `JobExecution` 를 가질 수 있고, 특정 `Job` 과 식별 가능한 `JobParameters` 를 가질 수 있다. (단, `JobParameters` 별 `JobInstance` 는 1개 뿐이다.)

#### JobParameters
`JobParameters` 는 다수의 `Job` 를 구분할 수도 있는 `Job` 이 실행될 때 사용하는 Parameter Set 객체이다. `Job` 의 식별값 또는 참조 데이터로 사용 가능하다.

#### JobExecution
`JobExecution` 는 `Job` 의 실행을 의미한다. 실행이 성공적으로 종료되기 전까지는 완료되지 않은 것으로 판단하고, 관리한다.

---

### Step
![Job & Step 구조](/images/job-hierarchy-with-steps.png)

`Step` 은 `Job` 의 독립적이고 순차적으로 단계를 처리하기 위한 도메인 객체이다. 모든 `Job` 은 하나 이상의 `Step` 으로 구성되며, `Step` 은 실제 Batch 에서 수행해야하는 처리를 정의하고 있다.

#### StepExecution
`StepExecution` 은 한 번의 `Step` 실행 시도를 의미한다. `Step` 를 실행할 때마다 생성되지만, 이전 단계의 `Step` 이 실패되면 다음 `Step` 를 실행하지 않고 `StepExecution` 도 저장하지 않는다.

#### Job & Step 구조
![spring Batch job & step 구조](/images/spring-batch-job-step.png)

`Job` 은 항상 1개의 이상의 `Step` 으로 구성되어있다.
`Step` 를 구성할 수 있는 방법은 크게 2가지로 나눌 수가 있다.
- `Tasklet` : Batch 의 작업을 하나로 처리
- `Reader & Processor & Writer` : 조회 / 처리 / 전달 기능을 나눠서 처리

---

### 기타 중요 기능
#### JobRepository
`JobRepository` 는 `Spring Batch` 에서의 모든 저장(`persistence`) 메커니즘을 담당하고, `JobLauncher`, `Job`, `Step` 구현체에 `CRUD` 기능을 제공해준다.
`Job` 를 실행할 때 `repository` 에서 `JobExecution` 를 조회하고, 실행 중에는 `StepExecution` 과 `JobExecution` 구현체를 `repository` 에 전달/저장한다.

#### JobLauncher
`JobLauncher` 는 `JobParameters` 로 `Job` 을 실행시키는 인터페이스이다.

#### ItemReader
`ItemReader` 는 `Step` 에서 데이터를 조회하는 작업을 추상화한 개념이다.

#### ItemProcessor
`ItemProcessor` 는 `ItemReader` 에서 조회된 정보를 변환하거나 다른 비즈니스 처리를 담당한다.

#### ItemWriter
`ItemWriter` 는 `Step` 에서 `Batch` 또는 `chunk` 단위로 정보를 출력하는 작업을 추상화한 개념이다.

---

기본적인 `Spring Batch` 의 개념을 확인하였고, 세부적인 코드나 내용들은 계속 정리하면서 추가할 예정이다.

`Spring Batch` 는 앞서 설명한 것처럼 `@Configuration` 으로 `Bean` 등록하여 사용가능하지만, `jar` 가 실행될 때 수행하는 Batch 가 아닌 `Spring Web` 에서 특정 API 를 호출하여 Batch 의 `Job` 을 실행시키는 방법도 있다.
하지만, `Spring Web` 에서 실행시키는 방법은 권장하지 않는 방법이기에 일단 `jar` 로 실행시키는 방법을 정리할 예정이다.

---

#### 출처
- [jojoldu 님의 Spring Batch 가이드](https://jojoldu.tistory.com/324?category=902551)
- [Spring Batch 공식 Reference](https://docs.spring.io/spring-batch/docs/4.2.x/reference/html/index-single.html#spring-batch-intro)
