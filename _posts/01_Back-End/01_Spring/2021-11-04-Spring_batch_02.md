---
layout: post
title : Spring Batch step.2 - Job
date  : 2021-11-04
image : spring-batch-stereotypes.png
tags  : spring springbatch kotlin
---

## Job
`Spring Batch` 에서 상위 개념인 `Job` 의 설정은 무엇이 있는지 살펴보자.

### Configuring a Job
`Job` 은 `Step` 을 가지고 있으며, 구현하기 위해서는 `JobRepository` 가 필요하다. 그리고 `JobBuilderFactory` 를 통해서 아래와 같은 설정을 할 수 있다.

- `Restartability` : Batch 의 `Job` 을 실행할 때 재시작과 관련된 동작을 설정한다.
- `Intercepting Job Execution` : `JobListener` 를 통하여 `Job` 실행 전/후 시점에 특정 동작을 설정한다.
- `JobParametersValidator` : `Runtime` 시점에 `JobParameters` 의 검증을 하는 `Validator` 를 설정한다. `Job` 의 실행에 꼭 필요한 `Parameter` 를 검증하는데 유용하다.

---

### BatchConfigurer
`BatchConfigurer` 는 `Job` 을 설정하는 핵심 인터페이스다. Default 구현체는 아래와 같은 `Bean` 설정을 제공하고 있다. (단, `Datasource` 설정은 필요하다.)

#### Default Bean's of BatchConfigurer
- `JobRepository`
- `JobLauncher`
- `JobRegistry`
- `PlatformTransactionManager`
- `JobBuilderFactory`
- `StepBuilderFactory`

`BatchConfigurer` 를 상속받아 직접 구현한다면, 위와 같은 Default `Bean` 들은 변경 및 구현 가능하다.

##### Default configuration
{% highlight java %}
@Import(DataSourceConfiguration.class)
@EnableBatchProcessing
@Configuration
public class AppConfig {

    @Autowired
    private JobBuilderFactory jobs;

    @Autowired
    private StepBuilderFactory steps;

    @Bean
    public Job job(@Qualifier("step1") Step step1, @Qualifier("step2") Step step2) {
        return jobs.get("myJob")
                   .start(step1)
                   .next(step2)
                   .build();
    }

    @Bean
    protected Step step1(ItemReader<Person> reader,
                         ItemProcessor<Person, Person> processor,
                         ItemWriter<Person> writer) {
        return steps.get("step1")
                    .<Person, Person> chunk(10)
                    .reader(reader)
                    .processor(processor)
                    .writer(writer)
                    .build();
    }

    @Bean
    protected Step step2(Tasklet tasklet) {
        return steps.get("step2")
                    .tasklet(tasklet)
                    .build();
    }
}
{% endhighlight %}

#### JobRepository
`JobRepository` 의 설정은 직접 구현하면 다음과 같은 설정을 구현 가능하다.

- `Datasource` 설정
- `TransactionManager` 설정
- Table 명의 Prefix 설정
- `In-Memory` 설정
- Non-standard Database Types 설정

#### JobLauncher
`JobLauncher` 의 기본 구현체는 `SimpleJobLauncher` 가 있다.

{% highlight java %}
@Override
protected JobLauncher createJobLauncher() throws Exception {
  SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
  jobLauncher.setJobRepository(jobRepository);
  jobLauncher.afterPropertiesSet();
  return jobLauncher;
}
{% endhighlight %}

`SimpleJobLauncher` 는 기본으로 생성되는 `JobExecution` 을 `Job` 에게 전달하여 실행하고, 마지막으로 호출된 곳으로 `JobExecution` 반환한다.
아래와 같은 그림처럼 호출한 곳에서 마지막 응답을 최종적으로 받는 것을 확인해볼 수 있다.

![JobLauncher sync flow](/images/job-launcher-sequence-sync.png)

하지만, 이와 같은 방법은 `HTTP` 요청에 대해 처리가 되면 다른 요청에 대한 처리가 지연이 발생할 수 있다. 그래서 비동기로 `JobExecution` 를 응답해주는 `TaskExecutor` 구현체가 필요하다.

{% highlight java %}
@Bean
public JobLauncher jobLauncher() {
  SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
  jobLauncher.setJobRepository(jobRepository());
  jobLauncher.setTaskExecutor(new SimpleAsyncTaskExecutor());
  jobLauncher.afterPropertiesSet();
  return jobLauncher;
}
{% endhighlight %}

`TaskExecutor` 는 비동기 방식으로 `HTTP` 요청에 대해 먼저 응답을 처리 후, `Job` 을 실행하여 처리를 한다.

![JobLauncher async flow](/images/job-launcher-sequence-async.png)

---

여기까지 보면 `Spring Batch` 에서 `Job` 을 구성하는 기본적인 구현 방법은 이해가 된다. 세부적인 `Job` 설정들은 구현 단계에서 공식 레퍼런스를 참고하면서 학습할 예정이다.

---

#### 출처
- [jojoldu 님의 Spring Batch 가이드](https://jojoldu.tistory.com/324?category=902551)
- [Spring Batch 공식 Reference](https://docs.spring.io/spring-batch/docs/4.2.x/reference/html/index-single.html#spring-batch-intro)
