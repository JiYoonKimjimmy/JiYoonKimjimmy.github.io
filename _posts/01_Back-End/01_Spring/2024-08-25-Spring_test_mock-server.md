---
layout: post
title : Spring 외부 서비스 연동 테스트
date  : 2024-08-25
image : spring_basic.png
tags  : spring springframework springboot mockwebserver testcode
---

## 외부 서비스 연동

만약 개발 서비스에서 외부 서비스와 연동하는 기능 개발이 필요한 경우, 
외부 서비스와 엮여있는 비즈니스 로직에 대한 테스트하기가 쉽지 않을 때가 있다. 

외부 서버, 컴포넌트 등 연동이 필요한 로직은 테스트 코드에서 대부분 `Mocking` 처리하여
단위 테스트를 작성하곤 하지만, 만약 `RestClient`, `WebClient` 등 HTTP 요청 라이브러리에 대한 테스트 코드는 검증할 수 없게 된다.

라이브러리의 기능을 `Mocking` 하기란 쉽지 않기 때문에, 
프로젝트 환경과 로직에 맞게 커스텀한 HTTP 라이브러리를 테스트 하기 위해서는
통합 테스트 코드를 작상하여 HTTP 라이브러리 `Bean` 주입받아 테스트하게 되었다.

문제는 통합 테스트 코드는 외부 서버가 네트워크 통신 불가 상태이거나 응답 지연에 따른 영향도로 
테스트 코드가 예상한대로 검증되지 않는 상황이 발생하였다.

그래서 외부 서버 자체를 `Mocking` 처리하는 `MockServer` 테스트를 위한 다양한 방법을
정리해보고자 한다.

---

### MockServer 테스트 방법

1. MockWebServer
2. WireMock
3. Mockito
4. REST-Assured
5. Spring Cloud Contract

> 예제 코드에서 활용되는 HTTP 요청 라이브러리는 `Spring 6` 에서 제공하는 `RestClient` 를 사용하였다.

---

### MockWebServer

- `Square` 에서 제공하는 HTTP 서버로, HTTP 테스트 요청을 `Mocking` 할 수 있다.

```groovy
dependencies {
    // ...
    implementation 'com.squareup.okhttp3:mockwebserver:4.11.0'
    // ...
}
```

```java
@RestClientTest
class NssRestClientHttpExchangeTest(
    private val nssRestClientHttpExchange: NssRestClientHttpExchange
) : StringSpec({

    lateinit var mockServer: MockWebServer

    beforeSpec {
        mockServer = MockWebServer()
        mockServer.start(8090)
    }

    afterSpec {
        mockServer.shutdown()
    }

    "(HttpExchange) SMS 알림 발송 요청하여 응답 정상 확인한다" {
        // given
        val request = NssSendSmsRequest(
            phoneNumber = "01012340001",
            title = "SMS 알림 발송 테스트",
            contents = "SMS 알림 발송 테스트 입니다."
        )

        mockServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatus.OK.value())
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(jacksonObjectMapper().writeValueAsString(NssSendNotificationResponse(ResultCode.SUCCESS)))
        )

        // when
        val response = try {
            nssRestClientHttpExchange.sendSMS(request)
        } catch (e: Exception) {
            NssSendNotificationResponse(ResultCode.FAIL)
        }

        // then
        response.resultCode shouldBeIn ResultCode.entries
        response.resultCode shouldBe ResultCode.SUCCESS
    }

})
```

---

#### 출처

- [우아한 기술블로그 - 서버사이드 테스트 파랑새를 찾아서](https://techblog.woowahan.com/14874/)
- [devkuma - WebClient 테스트 - MockWebServer를 이용해 외부API 호출 메서드 테스트](https://www.devkuma.com/docs/mock-web-server/)

---
