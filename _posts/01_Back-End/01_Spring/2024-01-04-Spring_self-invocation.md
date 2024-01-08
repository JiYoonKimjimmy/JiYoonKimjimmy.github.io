---
layout: post
title : Self-Invocation 이슈
date  : 2024-01-04
image : spring_basic.png
tags  : spring springaop self-invocation
---

## `@Transactional` 의 Self-Invocation 이슈?

처음 `Self-Invocation` 이슈를 맞이한 건, `@Service` 클래스 함수에서 같은 클래스의 `@Transactional` 가 붙은 다른 함수를 호출하였던 순간이었다.

하지만 `Self-Invocation` 이슈를 하다보니, 사실 `@Transactional` 사용에 대한 이슈라기 보단 `Spring AOP` 와 관련된 이슈라는 것을 알 수 있었다.
`AOP` 적용을 위해 **Proxy** 객체가 사용되지만, 잘못된 방법 때문에 `Self-Invocation` 이슈를 보게 된 것이다.

`Self-Invocation` 이슈는 런타임/컴파일 에러는 발생하지 않지만, 개발자 의도와는 다른 동작을 하게 되어 
서비스 운영 환경에서 장애를 발생시킬 수 있는 코드가 될 수 있다. 그런 경우를 대비하기 위해 정리를 시작해보자.

---

### Spring AOP **Proxy**

먼저 Spring AOP 의 **Proxy** 객체에 대한 간략한 정리가 필요할 것 같다.

Spring 은 **관점 지향 프로그래밍** `AOP` 지원하고 있으며, `AOP` 구현을 위해 **Proxy** 기법을 적용하고 있다.

> 간단하게 **Proxy** 는 원본 객체를 감싸 **공통적인 부가 기능** 을 구현 및 처리 하는 패턴 기법이다.

![Spring AOP Proxy](/images/spring-aop-proxy.png)

이렇게 `@Transactional` 애노테이션이 있는 **Proxy** 함수의 접근 방식은 **Proxy 객체 외부에서 접근(호출)해야 한다.**
그래야 원본 함수의 코드를 감싸고 있는 **Proxy** 함수 코드가 정상 동작하기 때문이다.

---

### **Proxy** 객체내 함수 호출 = Self-Invocation 자기 호출

위에서 설명한 것처럼 `AOP` 적용을 위한 함수는 **Proxy** 객체 외부에서 호출해야 한다고 했는데, 
**Proxy** 객체 내부에서 `AOP` 적용을 원하는 함수를 호출한다면, **실제 트랜잭션이 연결되지 않을 수 있다.**

이 부분이 바로 `Self-Invocation` **자기 호출** 이슈가 된다.

다음과 같이 `User` Entity 를 조회하고, 수정하는 서비스 로직이 있다.

1. `User` 정보 조회
2. `User` 의 `name` 변경
3. `User` 객체 정보 저장
4. `User` 객체 정보 변경 이력 저장

- ***단, 회원 정보 변경 이력 저장 실패 시, 회원 정보 원복 처리***

{% highlight java %}
@Service
class UserService(
    private val userRepository: UserRepository,
    private val userHistoryRepository: UserHistoryRepository
) {

    fun updateUserName(id: String, name: String) {
        val user = userRepository.findById(id)
        
        updateUserName(user, name)
        
        saveUser(user)
    }

    fun updateUserName(user: User, name: String) {
        user.setName(name)
    }

    @Transactional
    fun saveUser(user: User) {
        val saved = userRepository.save(user)
        saveUserHistory(saved)
    }

    @Transactional
    fun saveUserHistory(user: User) {
        userHistoryRepository.save(user) // 예외 발생..!!
    }

}
{% endhighlight %}

위 코드에서 장애는 `saveUser()` 함수에서 변경된 회원 정보를 저장하였지만, `saveUserHistory()` 함수에서 ***예외가 발생했다***는 점이다.

회원 정보 변경 이력 저장이 실패되었다면, 변경한 회원 정보도 다시 **Roll-back 롤백** 되어야할 것이다.

하지만, 위 코드에서는 회원 정보 **롤백되지 않고,** 변경된 정보가 그대로 저장되어
`User` 정보와 `UserHistory` 정보의 싱크는 틀어지고, 변경 이력 정보에 대한 적합성이 떨어진다.

해당 코드의 문제점은 **바로 `saveUser()` 함수 호출이 `Self-Invocation` 자기 호출 이슈라는 것이다.**

`saveUser()` 함수는 **Trnasaction 처리**를 위해 `@Transactional` 애노테이션을 추가하였지만, Proxy 객체 내부에서 호출된 함수이다.

`updateUserName()` 함수는 `UserService` 라는 **Proxy 객체 외부**에서 호출되는 함수이기 때문에 `AOP` 적용되는 반면, 
`saveUser()` 함수는 **Proxy 객체 내부** 함수 호출이므로 `AOP` 적용되지 않기 때문에 원하는 Transaction 처리는 동작하지 않게 된다.

> `Self-Invocation` 이해를 위해 `@Transactional` 예시를 정리하였지만, 꼭 `@Transactional` 문제만은 아닐 것이다.

---

### 해결 방안

그렇다면 `Self-Invocation` 이슈를 해결할 수 있는 방법은 무엇이 있을까?

해당 이슈 해결 방법은 여러 가지가 있을 수 있다.

- `Self-Injection` 방식으로 **Proxy** 객체 변환 사용
- `AopContext` or `AspectJ` 라이브러리 활용한 **Proxy** 객체 변환 사용
- 기타 등등..

위 방법처럼 해당 서비스 로직 클래스를 다시 **Proxy** 객체로 만들어 사용하는 방법이 많이 사용되지만,
라이브러리 의존하기 보단~~(물론 Java 또는 Spring 에서 지원하는 라이브러리지만)~~, 좀 더 직관적으로 해결할 수 있는 방법을 찾고 싶었다.

보통 비즈니스 로직 개발하다보면, 트랜잭션 처리에 대해서 크게 2가지 정도 요구 사항이 있다.

1. 첫번째는, 부모 트랜잭션 아래 있는 자식 트랜잭션 중 하나라도 예외 발생하면 **전부 롤백 처리**
2. 두번째는, 부모 트랜잭션 아래 있는 자식 트랜잭션 중 각각 **별도 트랜잭션 처리**

위 2가지 요건을 모두 충족할 수 있고, `Self-Invocation` 이슈 없는 로직 구현을 위해,
**별도 서비스 클래스 분리**하는 방식으로 해결하고자 한다.

별도 서비스 클래스로 분리하여 `Bean` 주입하여 함수 호출한다면 `AOP` 적용은 될 것이다.

> 사실 서비스 클래스 분리하지 않더라도, `updateUserName()` 함수에도 `@Transactional` 애노테이션을 추가한다면, 트랜잭션 처리는 가능하다.
> 
> 하지만, 요구 사항에 대한 변경과 다양한 로직 수행에 대한 리팩토링을 위해서라도, 클린-아키텍처 개념 구현을 위해서라도,
> 서비스 클래스 분리 방식 구현은 나름 괜찮은 방안이 아닐까 싶다.

---

#### 1. 하나의 Transaction 관리

하나의 트랜잭션에 많은 로직들이 있을 수 있다. 
그리고 요구 사항에 따라 많은 로직 중 하나라도 예외가 발생한다면 실패 응답과 롤백 처리가 필요한 경우도 있을 것이다.

서비스 클래스 분리 방식으로 처음 예제 코드를 한번 리팩토링 해보면,

{% highlight java %}
@Service
class UserService(
    private val userPersistenceService: UserPersistenceService
) {

    fun updateUserName(id: String, name: String) {
        val user = userPersistenceService.findUserById(id)

        updateUserName(user, name)

        userPersistenceService.saveUser(user)
    }

    fun updateUserName(user: User, name: String) {
        user.setName(name)
    }

}

@Service
class UserPersistenceService(
    private val userRepository: UserRepository,
    private val userHistoryRepository: UserHistoryRepository
) {

    fun findUserById(id: String): User {
        return userRepository.findById(id)
    }
    
    @Transactional
    fun saveUser(user: User): User {
        val saved = userRepository.save(user)
        saveUserHistory(saved)
        return saved
    }

    @Transactional
    fun saveUserHistory(user: User): User {
        userHistoryRepository.save(user) // 예외 발생..!!
    }

}
{% endhighlight %}

`User` 객체를 조회 & 저장 관리하는 `UserPersistenceService` 클래스를 분리하여 `Self-Invocation` 이슈는 다음과 같이 해결된다.

- `UserPersistenceService.saveUser()` 함수는 **Proxy 객체 외부**에서 호출되는 함수가 되므로, `AOP` 적용될 것이다.
- 그리고, `saveUserHistory()` 함수에서 예외가 발생하면 `saveUser()` 함수 로직까지 **롤백 처리**된다.

이번 리팩토링을 통해서 **부모 트랜잭션 아래 있는 자식 트랜잭션 중 하나라도 예외 발생하면 전부 롤백 처리** 요건에 대한 부분을 충족시킬 수 있었다.

---

#### 2. 별도 Transaction 관리

서비스 요구 사항 중에 회원 정보 변경 후 **정보 변경 안내 알림을 발송하는 기능**이 추가된다고 해보자.

단, 안내 알림 발송이 *실패*하더라도 회원 정보 변경은 **성공**한다는 요건이 있다면, 각 로직 별 트랜잭션 처리가 필요할 것이다.

{% highlight java %}
@Service
class UserService(
    private val userPersistenceService: UserPersistenceService,
    private val noticeService: NoticService
) {

    fun updateUserName(id: String, name: String) {
        val user = userPersistenceService.findUserById(id)

        updateUserName(user, name)

        userPersistenceService.saveUser(user)

        // 알림 발송 기능 추가
        noticeService.noticeToUser(user)
    }

    fun updateUserName(user: User, name: String) {
        user.setName(name)
    }

}

@Service
class UserPersistenceService(
    private val userRepository: UserRepository,
    private val userHistoryRepository: UserHistoryRepository
) {

    fun findUserById(id: String): User {
        return userRepository.findById(id)
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveUser(user: User): User {
        val saved = userRepository.save(user)
        saveUserHistory(saved)
        return saved
    }

    @Transactional
    fun saveUserHistory(user: User) {
        userHistoryRepository.save(user)
    }

}

@Service
class NoticService {

    fun noticeToUser(user: User) {
        // 알림 발송 처리
        // 예외 발생..!
    }

}
{% endhighlight %}

이번 코드에서 중요한 점은 `UserPersistenceService.saveUser()` 함수에 있는 `@Transactional` 애노테이션 옵션이다.
`REQUIRES_NEW` 옵션을 추가하면서 `saveUser()` 함수는 **새로운 트랜잭션을 생성하여 처리하도록 하였다.**

- `Propagation.REQUIRES_NEW` 옵션은 부모 트랜잭션을 그대로 전파받지 않고, 새로운 트랜잭션을 생성하여 처리한다.
- `NoticeService.noticeToUser()` 함수 처리 중 장애가 발생하더라도, `saveUser()`, `saveUserHistory()` 함수 로직은 **롤백 되지 않는다.**

> `@Transactional` 애노테이션의 트랜잭션 전파 방식에 대한 옵션에 대한 설명은 생략하겠다.

---

### 정리

백엔드 서비스 개발할 때 조심해야하고, 중요한 부분 중에 트랜잭션 관리도 포함일 것이다.

장애/예외 상황 고려하지 않고, 정상 로직만 고민하여 개발하여 트랜잭션 관리에 대한 고민이 없다면 큰 장애을 경험해볼 수 있을 것이다.

`Self-Invoation` 이슈는 비록 트랜잭션 관리에 대한 이슈라고 할 순 없지만, `Spring` 을 활용하거나 `AOP` 와 같은 Proxy 패턴 객체와 같은 기술을 활용할 때는
조심해야하는 이슈라는 것을 알 수 있다.

---

#### 출처

- [이로운 개발하기 - @Transactional 사용 시 자기 호출(Self-Invocation) 이슈 - 실습으로 배우는 JPA 3편](https://stir.tistory.com/175)
- [Moon - Self Invocation은 왜 발생할까?](https://gmoon92.github.io/spring/aop/2019/04/01/spring-aop-mechanism-with-self-invocation.html)
- [kiarash shamaii - Proxy and Dynamic Proxy in Spring Java](https://medium.com/@kiarash.shamaii/proxy-and-dynamic-proxy-in-spring-java-6256ddd6e09f)
