---
layout: post
title : ArgumentCaptor.java
date  : 2026-01-22
image : java_log.png
tags  : java argumentcaptor mockito test
---

## ArgumentCaptor란?

`ArgumentCaptor`는 Mockito 라이브러리에서 제공하는 클래스로, **Mock 객체의 메서드 호출 시 전달된 인자를 캡처하여 나중에 검증**할 수 있게 해주는 도구입니다.

일반적인 `verify()` 검증에서는 `eq()`, `any()` 등의 matcher를 사용하여 인자를 검증하지만, ArgumentCaptor를 사용하면 실제로 전달된 인자 객체를 직접 가져와서 더 세밀한 검증이 가능합니다.

---

## 언제 사용하면 좋은가?

ArgumentCaptor는 다음과 같은 상황에서 특히 유용합니다:

1. **복잡한 객체 검증**: 메서드에 전달된 객체의 여러 필드를 상세히 검증하고 싶을 때
2. **동적으로 생성된 인자 검증**: 테스트 대상 코드 내부에서 생성된 객체를 검증할 때
3. **다중 호출 검증**: 같은 메서드가 여러 번 호출될 때 각 호출의 인자를 개별적으로 검증하고 싶을 때
4. **람다/콜백 검증**: 전달된 람다나 콜백 함수를 검증할 때

---

## 기본 사용법

### ArgumentCaptor 생성

```java
// 방법 1: forClass() 정적 메서드 사용
ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

// 방법 2: captor() 정적 메서드 사용 (Mockito 4.0+)
ArgumentCaptor<User> userCaptor = ArgumentCaptor.captor();
```

### 핵심 메서드

| 메서드 | 설명 |
|--------|------|
| `capture()` | verify() 내에서 인자를 캡처할 위치 지정 |
| `getValue()` | 마지막으로 캡처된 값 반환 |
| `getAllValues()` | 캡처된 모든 값을 List로 반환 |

---

## 기본 예제

### 단일 인자 캡처

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 사용자_저장시_전달된_객체를_검증한다() {
        // given
        String name = "홍길동";
        String email = "hong@example.com";

        // when
        userService.createUser(name, email);

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getName()).isEqualTo("홍길동");
        assertThat(capturedUser.getEmail()).isEqualTo("hong@example.com");
        assertThat(capturedUser.getCreatedAt()).isNotNull();
    }
}
```

위 예제에서 `userService.createUser()` 내부에서 생성된 `User` 객체를 캡처하여 각 필드를 검증하고 있습니다.

---

## @Captor 어노테이션

필드 레벨에서 `@Captor` 어노테이션을 사용하면 더 간결하게 ArgumentCaptor를 선언할 수 있습니다.

```java
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<EmailMessage> emailCaptor;

    @Test
    void 알림_발송시_이메일_내용을_검증한다() {
        // given
        String userId = "user123";

        // when
        notificationService.sendWelcomeEmail(userId);

        // then
        verify(emailSender).send(emailCaptor.capture());

        EmailMessage captured = emailCaptor.getValue();
        assertThat(captured.getSubject()).contains("환영");
        assertThat(captured.getBody()).contains(userId);
    }
}
```

### @Captor 사용의 장점

- 제네릭 타입을 명시적으로 선언하여 타입 안전성 확보
- 여러 테스트 메서드에서 재사용 가능
- 코드가 더 간결해짐

---

## 다중 호출 캡처

같은 메서드가 여러 번 호출될 때 `getAllValues()`를 사용하여 모든 호출의 인자를 검증할 수 있습니다.

```java
@Test
void 배치_처리시_각_항목별_로그를_검증한다() {
    // given
    List<Order> orders = List.of(
        new Order("ORD-001", 10000),
        new Order("ORD-002", 20000),
        new Order("ORD-003", 30000)
    );

    // when
    orderService.processBatch(orders);

    // then
    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logger, times(3)).info(logCaptor.capture());

    List<String> allLogs = logCaptor.getAllValues();
    assertThat(allLogs).hasSize(3);
    assertThat(allLogs.get(0)).contains("ORD-001");
    assertThat(allLogs.get(1)).contains("ORD-002");
    assertThat(allLogs.get(2)).contains("ORD-003");
}
```

---

## 복잡한 객체 검증

ArgumentCaptor는 복잡한 중첩 객체나 컬렉션을 포함한 인자를 검증할 때 특히 강력합니다.

```java
@Test
void 주문_생성시_전달된_주문_상세를_검증한다() {
    // given
    List<CartItem> cartItems = List.of(
        new CartItem("PROD-001", 2, 5000),
        new CartItem("PROD-002", 1, 15000)
    );

    // when
    orderService.createOrder("USER-001", cartItems);

    // then
    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository).save(orderCaptor.capture());

    Order captured = orderCaptor.getValue();
    
    // 기본 정보 검증
    assertThat(captured.getUserId()).isEqualTo("USER-001");
    assertThat(captured.getStatus()).isEqualTo(OrderStatus.PENDING);
    
    // 주문 항목 검증
    assertThat(captured.getItems()).hasSize(2);
    assertThat(captured.getTotalAmount()).isEqualTo(25000);
    
    // 첫 번째 항목 상세 검증
    OrderItem firstItem = captured.getItems().get(0);
    assertThat(firstItem.getProductId()).isEqualTo("PROD-001");
    assertThat(firstItem.getQuantity()).isEqualTo(2);
}
```

---

## ArgumentCaptor vs ArgumentMatcher

| 구분 | ArgumentCaptor | ArgumentMatcher (eq, any 등) |
|------|----------------|------------------------------|
| 용도 | 인자를 캡처하여 나중에 검증 | 호출 시점에 인자 조건 검증 |
| 유연성 | 캡처 후 다양한 assertion 가능 | 단순 조건 검증에 적합 |
| 가독성 | 검증 로직이 분리됨 | 한 줄로 간결하게 표현 |
| 권장 사용 | 복잡한 객체 검증 | 단순 값 검증 |

### 비교 예제

```java
// ArgumentMatcher 사용 - 단순한 경우
verify(userRepository).save(argThat(user -> 
    user.getName().equals("홍길동")
));

// ArgumentCaptor 사용 - 복잡한 검증이 필요한 경우
ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
verify(userRepository).save(captor.capture());

User captured = captor.getValue();
assertThat(captured.getName()).isEqualTo("홍길동");
assertThat(captured.getEmail()).matches(".*@example\\.com");
assertThat(captured.getRoles()).containsExactly(Role.USER);
assertThat(captured.getCreatedAt()).isAfter(LocalDateTime.now().minusMinutes(1));
```

---

## 주의사항

### 1. Stubbing보다는 Verification에서 사용

ArgumentCaptor는 stubbing(`when()`)보다 verification(`verify()`)에서 사용하는 것이 권장됩니다.

```java
// 권장하지 않음 - stubbing에서 사용
ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
when(mockService.process(captor.capture())).thenReturn("result");

// 권장 - verification에서 사용
verify(mockService).process(captor.capture());
```

stubbing에서 `capture()`를 사용하면 실제 테스트 실행 전에 캡처가 발생하여 예상치 못한 동작이 발생할 수 있습니다.

### 2. getValue()는 마지막 값만 반환

여러 번 호출된 경우 `getValue()`는 마지막 캡처된 값만 반환합니다. 모든 값이 필요하면 `getAllValues()`를 사용하세요.

```java
// 3번 호출된 경우
verify(service, times(3)).process(captor.capture());

// getValue()는 마지막(3번째) 호출의 인자만 반환
String lastValue = captor.getValue();

// getAllValues()는 모든 호출의 인자를 순서대로 반환
List<String> allValues = captor.getAllValues();
```

### 3. 제네릭 타입과 타입 안전성

제네릭 컬렉션을 캡처할 때는 `@Captor` 어노테이션을 사용하면 타입 안전성을 확보할 수 있습니다.

```java
// 컴파일 경고 발생 가능
ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);

// @Captor 사용으로 타입 안전성 확보
@Captor
private ArgumentCaptor<List<String>> listCaptor;
```

---

## 실전 활용 패턴

### 이벤트 발행 검증

```java
@Test
void 주문_완료시_이벤트가_발행된다() {
    // given
    Order order = new Order("ORD-001", 50000);

    // when
    orderService.complete(order);

    // then
    ArgumentCaptor<OrderCompletedEvent> eventCaptor = 
        ArgumentCaptor.forClass(OrderCompletedEvent.class);
    verify(eventPublisher).publish(eventCaptor.capture());

    OrderCompletedEvent event = eventCaptor.getValue();
    assertThat(event.getOrderId()).isEqualTo("ORD-001");
    assertThat(event.getAmount()).isEqualTo(50000);
    assertThat(event.getOccurredAt()).isNotNull();
}
```

### 외부 API 호출 검증

```java
@Test
void 결제_요청시_올바른_파라미터로_API를_호출한다() {
    // given
    PaymentRequest request = new PaymentRequest("USER-001", 10000);

    // when
    paymentService.pay(request);

    // then
    ArgumentCaptor<HttpEntity<PaymentApiRequest>> captor = 
        ArgumentCaptor.forClass(HttpEntity.class);
    verify(restTemplate).postForEntity(
        eq("https://api.payment.com/pay"),
        captor.capture(),
        eq(PaymentApiResponse.class)
    );

    PaymentApiRequest apiRequest = captor.getValue().getBody();
    assertThat(apiRequest.getMerchantId()).isEqualTo("MY_MERCHANT");
    assertThat(apiRequest.getAmount()).isEqualTo(10000);
    assertThat(apiRequest.getSignature()).isNotBlank();
}
```

---

## 정리

ArgumentCaptor는 Mockito를 사용한 단위 테스트에서 메서드에 전달된 인자를 상세하게 검증할 때 매우 유용한 도구입니다.

**핵심 포인트:**
- `capture()`로 인자를 캡처하고 `getValue()` 또는 `getAllValues()`로 조회
- `@Captor` 어노테이션으로 간결하게 선언 가능
- 복잡한 객체나 동적으로 생성된 인자 검증에 적합
- stubbing보다는 verification에서 사용하는 것이 권장됨

---

## 출처

- [Mockito JavaDoc - ArgumentCaptor](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/ArgumentCaptor.html)
- [Baeldung - Using Mockito ArgumentCaptor](https://www.baeldung.com/mockito-argumentcaptor)