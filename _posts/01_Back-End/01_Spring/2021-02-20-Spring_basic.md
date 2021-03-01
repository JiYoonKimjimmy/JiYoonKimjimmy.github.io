---
layout: post
title : Spring Basic
date  : 2021-02-20
tags  : back-end spring
---

## IoC(Inversion of Control)
* **제어권 역전**? 의존성을 직접 선언하는 것이 아닌, 어떤 방법을 통해 선언된 객체를 주입받아 사용하는 것

### IoC Container
#### ApplicationContext (BeanFactory)
* Container 내부에서 생성한 Bean 들만 관리
* Bean 을 생성하고 엮어주며 제공

---

## Bean
* Spring IoC Container 가 관리하는 객체

### Bean 등록하는 방법
#### 1. @ComponentScan
* **@Component** 이 있는 Class 를 모두 찾아서 등록하는 방식

#### 2. @Bean
* **@Configuration** 이 있는 Class 안에 **@Bean** 을 사용하여 등록하는 방식

---

## DI(Dependency Injection)
* **의존성 주입**? 등록된 Bean 들을 사용할 수 있게 주입받는 것

### 의존성 주입받는 방법
#### 1. @Autowired or @Inject
* 생성자 & 필드 & Setter 에 **@Autowired** 를 추가하면 Bean 의존성 주입 가능
```java
@RestController
public class ApiController {
  @Autowired
  private final ApiService apiService;
}
```

#### 2. 생성자의 인자값으로 Bean 등록된 Class 일 경우 의존성 주입 가능
```java
@RestController
public class ApiController {
  private final ApiService apiService;

  public ApiController(ApiService apiService) {
    this.apiService = apiService;
  }
}
```

---

## AOP(Aspect-Oriented Programming)
* **관점 지향 프로그래밍**? 어플리케이션 전체에서 사용되는 기능을 재사용하는 것
* 핵심적인 기능에서 **부가적인 기능(Aspect)** 을 분리

### Aspect
* 부가적인 기능을 정의한 **Advice** 와 Advice를 어디에 적용할지 결정하는 **PointCut** 을 합친 개념

#### 예제
* LogExecutionTime : Annotation 역할
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {
  // Annotation 역할
}
```

* LogAspect : 실제 Annotation 구현체
```java
@Component
@Aspect
public class LogAspect {

    Logger logger = LoggerFactory.getLogger(LogAspect.class);

    @Around("@annotation(LogExecutionTime)")
    public Object LogExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();

        stopWatch.start();

        Object proceed = joinPoint.proceed();

        stopWatch.stop();

        logger.info(stopWatch.prettyPrint());

        return proceed;
    }
}
```

***! 필요한 Method 위에 @LogExecutionTime 추가하면 Log 확인 가능 !***

---

## PSA(Portable Service Abstraction)
* **휴대성이 있는(잘 갖춰진) 서비스 추상화**? 인터페이스를 이용해서 개발한 서비스. 잘 갖춰진 인터페이스를 이용해 개발하는 것이 서비스의 확장성을 높일 수 있는 방법.

### PSA 예시
* Spring Transaction : **@Transactional**
* Cache : **@Cacheable**, **@CacheEvict**
* Spring MVC : **@Controller**, **@RequestMapping**
