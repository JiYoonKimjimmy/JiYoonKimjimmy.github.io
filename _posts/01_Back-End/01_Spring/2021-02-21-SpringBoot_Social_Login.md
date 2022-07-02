---
layout: post
title : Spring Boot + OAuth 2.0
date  : 2021-02-21
image : spring_oauth2.png
tags  : back-end spring
---

## OAuth 2.0
`OAuth` 는 인증을 위한 개방형 표준 프로토콜이다.

리소스 서버에서 제공하는 자원에 대한 접근 권한을 관리하고, 접근 인증을 해주는 기능을 수행한다.

구글, 네이버, 카카오 와 같은 다양한 기업에서 `Open-API` 제공하거나 `Social Login` 를 제공할 때, `token` 정보 제공하여 API 를 호출하도록 할텐데, `token` 인증 방식 중 `OAuth` 프로토콜 방식으로 개발 구현되어있다.

## OAuth 구조
### OAuth 구성
* Client : 사용자
* Resource Owner : API DB 사용자
* Resource Server : API 서버
* AUthorization Server : OAuth 인증 서버

### OAuth Flow
![OAuth Flow](/images/25238637583547EC0A.png)
<br>
(A) Client 가 Social Login 요청<br>
(B) Resource Owner 는 Social Login 할 수 있게 화면 이동<br>
(C) Client 는 Social Login 완료<br>
(D) 로그인이 성공하면 AUthorization Server 는 Client 에게 Access Token 발급<br>
(E) Client 는 발급받은 Token 으로 Resource Server 에게 Resource 를 요청<br>
(D) Resource Server 는 Token 유효한지 검증하고 응답 처리<br>

---

## Kakao Soical Login
### Token 종류
* Access Token : API 호출 권한 인증용 Token
* Refresh Token : Access Token 갱신용 Token

### Kakao Developer 관리
* [Kakao Developer 사이트](https://developers.kakao.com/)
* [계정 생성 및 등록 방법(velog blog)](https://velog.io/@magnoliarfsit/%EA%B7%B8%EB%A3%B9%ED%94%84%EB%A1%9C%EC%A0%9D%ED%8A%B8-%ED%94%84%EB%A6%BD-%EB%93%A4%EC%96%B4%EA%B0%80%EA%B8%B0-%EC%A0%84%EC%97%90-1)

### 구현 순서
[Kakao Login 구현 공통 가이드](https://developers.kakao.com/docs/latest/ko/kakaologin/common)

**RestTemplate Bean 등록**
* Main Application Class 에 RestTemplate Class Bean 등록

{% highlight java %}
@Bean
public RestTemplate getRestTemplate() { return new RestTemplate(); }
{% endhighlight %}

**GSON dependency 추가**

{% highlight groovy %}
implementation 'com.google.code.gson:gson'
{% endhighlight %}

**Social Login 관련 설정 추가**
{% highlight properties %}
spring.social.kakao.client_id=[앱생성시 받은 REST API Key]
spring.social.kakao.redirect=/social/login/kakao
spring.social.kakao.url.login=https://kauth.kakao.com/oauth/authorize
spring.social.kakao.url.token=https://kauth.kakao.com/oauth/token
spring.social.kakao.url.profile=https://kapi.kakao.com/v2/user/me
{% endhighlight %}

**Social Login 처리 Controller 추가**
* Kakao Login 화면으로 Fowording 할 수 있는 Demo 페이지 연동 처리
* Kakao 연동 후 redirect 처리

**로그인 화면**
* /resource/templates/social/login.ftl

{% highlight html %}
<button onclick="popupKakaoLogin()">KakaoLogin</button>
<script>
    function popupKakaoLogin() {
        window.open('${loginUrl}', 'popupKakaoLogin', 'width=700,height=500,scrollbars=0,toolbar=0,menubar=no');
    }
</script>
{% endhighlight %}

*(만약 View Mapping 안된다면, Freemarker Bean 등록!)*

**User Entity 수정**
* provoider 필드 추가
* Social Login 은 비밀번호가 필요없으므로 password 필드는 Null 허용으로 변경

**User JPA Repository 수정**
* Uid 와 Provider 로 회원 정보 조회하는 Method 추가

{% highlight java %}
Optional<User> findByUidAndProvider(String uid, String provider);
{% endhighlight %}

**Social Login 에 필요한 Model 객체 생성**
* RetKakaoAuth.java

{% highlight java %}
@Getter
@Setter
public class RetKakaoAuth {
    private String access_token;
    private String token_type;
    private String refresh_token;
    private long expires_in;
    private String scope;
}
{% endhighlight %}

* KakaoProfile.java

{% highlight java %}
@Getter
@Setter
@ToString
public class KakaoProfile {
    private Long id;
    private Properties properties;

    @Getter
    @Setter
    @ToString
    private static class Properties {
        private String nickname;
        private String thumbnail_image;
        private String profile_image;
    }
}
{% endhighlight %}

**Kakao 연동 Service 생성**
* Kakao Token 조회 method 구현
* Kakao profile 조회 method 구현

**Social Login 연동할 REST API 추가**
* Kakao 와 연동 여부 확인한 후 JWT Token 발급 처리

**Security Config 관련 설정 수정**
* "/social/**" API 권한 승인 처리

---

## 추가
#### Social Login TEST Flow
1. /social/login 접속
1. 소셜 로그인 요청
1. 소셜 로그인 완료 후 access_token 발급
1. /swagger-ui.html 접속
1. /api/sign/signup/{provider} 사용자 가입
    1. 발급받은 Social Login access_token 필요
1. /api/sign/singin 사용자 로그인하여 JWT Token 발급
1. 발급받은 JWT Token 으로 API 테스트

#### Social Login 관련 기타 Class 및 Interface2
##### RestTemplate Class
* Spring 에서 지원하는 REST API 를 호출하고 응답 받을 때까지 기다리는 동기 방식의 내장 Class ***(비동기식은 AsyncRestTemplate.class)***
* HTTP 프로토콜의 메서드들 제공

---

[관련 Github Repository](https://github.com/JiYoonKimjimmy/demo-rest-api)
