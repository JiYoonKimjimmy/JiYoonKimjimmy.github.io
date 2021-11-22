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

### ItemReader
`ItemReader` 는 데이터를 읽어오는 간단한 인터페이스이지만, 다양한 입력을 읽어올 수 있다.
예를 들면 `Flat`, `XML`, `Database` 등 있을 수 있는데, 이 외에도 많은 데이터 정보를 읽을 수 있다.

{% highlight java %}
public interface ItemReader<T> {

    T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException;

}
{% endhighlight %}

기본적인 `ItemReader` 인터페이스는 `read` 함수를 구현해야 한다. 이 함수는 `Item` 하나를 반환하거나 더 이상 반환할 `Item` 이 없는 경우, `null` 를 반환한다. `Item` 은 읽어오는 파일의 한줄이거나 데이버테이스의 한 `row` 를 의미한다.

`ItemReader` 는 더이상 읽을 데이터가 없을 때 별도의 `exception` 처리를 하지 않는다. 단순히 `null` 를 반환할 뿐이다.

---

### ItemWriter
`ItemWriter` 는 `ItemReader` 에서 하는 일과는 정반대인 데이터를 `write` 하는 일을 한다. `write` 를 한다고하지만, 이 기능은 `insert`, `update`, `send` 등 여러가지 기능을 의미할 수 있다.

{% highlight java %}
public interface ItemWriter<T> {

    void write(List<? extends T> items) throws Exception;

}
{% endhighlight %}

`ItemWriter` 는 하나의 데이터가 아니라 리스트 형식의 `Item` 묶음을 받아 처리한다. 최대 `Chunk` 사이즈만큼 데이터 리스트를 받고, 리스트를 모두 `write` 한 후에는 결과를 반환하기 전에 `flush` 처리르 한다.

---

### ItemProcessor
`ItemProcessor` 는 읽어온 데이터를 변경이 필요한 비즈니스 로직을 정의할 수 있는 인터페이스이다. 해당 로직을 `ItemWriter` 에서 처리할 수도 있지만, 굳이 `ItemWriter` 에서 처리할 필요가 없다면 별도의 `ItemProcessor` 를 정의하여 처리할 수 있다.

{% highlight java %}
public interface ItemProcessor<I, O> {

    O process(I item) throws Exception;

}
{% endhighlight %}

위 코드와 같인 `ItemProcessor` 에서는 하나의 데이터 입력을 받아 하나의 데이터로 변환하여 반환한다. 새로 반환하는 객체의 타입은 같은 타입일 수도 있고, 다른 타입일수도 있다.

#### Chaining ItemProcessors
보통은 `Item` 은 한번의 변환으로 충분할 수도 있지만, 여러 번 변환이 필요한 경우도 있을 수 있다. 그럴 때는 `Compsite Pattern` 를 활용하여 구현이 가능하다.

{% highlight java %}
CompositeItemProcessor<Foo, Foobar> compositeProcessor = new CompositeItemProcessor<Foo, Foobar>();

List itemProcessors = new ArrayList();
itemProcessors.add(new FooTransformer());
itemProcessors.add(new BarTransformer());

compositeProcessor.setDelegates(itemProcessors);
{% endhighlight %}

---

### ItemStream



---

#### 출처
- [jojoldu 님의 Spring Batch 가이드](https://jojoldu.tistory.com/324?category=902551)
- [Spring Batch 공식 Reference](https://docs.spring.io/spring-batch/docs/4.2.x/reference/html/index-single.html#spring-batch-intro)
