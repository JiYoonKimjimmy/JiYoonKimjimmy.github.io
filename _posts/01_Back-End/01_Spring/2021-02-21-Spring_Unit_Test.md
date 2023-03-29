---
layout: post
title : Spring Unit Test
date  : 2021-02-21
image : spring_unit_test.png
tags  : back-end spring
---

## Dependency 추가

{% highlight groovy %}
testImplementation 'org.springframework.security:spring-security-test'
testImplementation 'org.springframework.boot:spring-boot-starter-test'
{% endhighlight %}

## JPA Test
JPA Test 를 위하여 **@DataJpaTest** Annotation 사용하여 Test 환경 구성
#### @DataJpaTest
* @Entity 를 조회하여 JpaRepository 테스트할 수 있는 환경 제공
* @Transactionla 를 포함하고 있어 테스트 완료 후 rollback 을 할 필요가 없다.


## Spring Boot Test
**@SpringBootTest** Annotation 를 이용하여 Spring Boot 의 Configuration 자동 설정된 Test 환경 구성
#### @SpringBootTest
* classes 설정을 통해 별도의 class Bean 만 설정 가능(설정하지 않은 경우, 등록된 모든 Bean 설정)

{% highlight java %}
@SpringBootTest(classes = {SecurityConfiguration.class, CustomUserDetailsService.class})
...
{% endhighlight %}

#### @AutoConfigureMockMvc
* Controller 테스트 시, MockMvc 를 이용할 경우 사용

#### MockMvc
* Spring MVC 의 동작을 재현할 수 있는 Class
* Spring DispatcherServlet 에게 요청하여 테스트를 진행
##### 관련 주요 함수

| 함수명 | 역할 |
|:---|:---|
| `perform()` | 주어진 url 을 수행할 수 있는 환경을 구성 |
| `andDo()` | perform 의 요청을 처리 |
| `andExpect()` | 검증 내용을 확인 |
| `andReturn()` | 테스트 완료 후 결과 객체를 확인 |

## Spring Security Test
@WithMockUser Annotation 를 이용하여 사용자에게 Resource 의 사용 권한 유무에 대한 Test 환경 구성

{% highlight java %}
// 가상의 ADMIN mock 사용자 대입
@WithMockUser(username = "mockUser", roles = {"ADMIN"})
{% endhighlight %}
