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
기본적인 `Step` 의 설정은 `StepBuilderFactory` 를 활용한 설정 방법이다.

{% highlight java %}
@Bean
public Step sampleStep(PlatformTransactionManager transactionManager) {
	return this.stepBuilderFactory.get("sampleStep")
				.transactionManager(transactionManager)
				.<String, String>chunk(10)
				.reader(itemReader())
        .processor(ItemProcessor())
				.writer(itemWriter())
				.build();
}
{% endhighlight %}

- `.transactionManager()` : `Step` 을 수행하는 과정에서 *Transaction* 의 `begin/commit` 을 관리하는 `TransactionManager` 설정 (기본 구현체는 `PlatformTransactionManager`)
- `.chunk()` : `Step` 의 `commit interval` 크기를 지정. 해당 크기만큼 `Chunk` 를 형성하고 `write` 를 수행
- `.reader()` : 처리 대상 데이터 읽어오는 `ItemReader` 구현체 설정
- `.processor()` : 읽어온 데이터를 가공/처리하는 `ItemProcessor` 구현체 설정
- `.writer()` : 데이터를 쓰기/처리하는 `ItemWriter` 구현체 설정


위와 같은 `Step` 설정의 `element` 가 아니더라도, 다양한 설정이 가능하다. 관련해서는 프로젝트 개발 단계에서 필요한 부분을 학습하고 적용해볼 필요가 있다.

---

사실 `Step` 부분을 보면서 한번에 모든 내용을 정리할 수는 없을 정도로 많은 분량이기에 간단하게 `Step` 의 역할과 기본 구현 방식만 살펴보았고, 실제로 코드를 구현해보면서 세세한 부분을 삺펴봐야할 것 같다.

---

#### 출처
- [jojoldu 님의 Spring Batch 가이드](https://jojoldu.tistory.com/324?category=902551)
- [Spring Batch 공식 Reference](https://docs.spring.io/spring-batch/docs/4.2.x/reference/html/index-single.html#spring-batch-intro)
