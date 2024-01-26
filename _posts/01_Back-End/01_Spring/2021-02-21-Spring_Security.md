---
layout: post
title : Spring Security (feat. JWT)
date  : 2021-02-21
image : spring_security.png
tags  : back-end spring jwt jsonwebtoken
---

## Spring Security?

* Spring 기반의 웹 애플리케이션의 웹 보안 제어를 위한 프레임워크
* 인증 및 권한 부여를 통해 요청에 대한 `Resource` 제어

---

## JWT ***(JSON Web Token)***

최근 Spring F/W 활용한 웹 애플리케이션에서는 `API` 유효성 검증을 위한 방식으로,
`Token` 인증 방식 중에서 **`JWT` 토큰 인증**을 많이 사용하고 있는 듯 하다.

### 특징

* `JSON` 형식의 데이터를 검증하기 위한 토큰 역할
* **웹 표준 기반** ***(RFC 7519)*** 의 다양한 환경 지원이 가능
* **Self-Contained** ***(자가 수용적)*** 으로서 JWT 자체가 모든 정보를 포함
* 자가 수용적인 특성을 이용해 **전달 방식이 비교적 간편**(Header 포함 or URL param 전달 가능)
* `JSON` 형식의 인증 정보를 쉽게 추가하는 등 높은 확장성 보장
* `Stateless 무상태` 의 서버 구현을 위한 수단으로 사용
* `Signature 서명` 정보를 통해 위조 & 변조 방지 가능 *(하지만, `CSRF` 공격 취약)*

> ###### `CSRF Cross-Site Request Forgery` 공격
> - 공격자가 피해자의 의도하지 않은 작업을 수행하도록 유도하는 해킹 공격
> - 공격자가 피해자의 `JWT` 토큰을 탈취하고 활용 가능

---

### JWT의 구조

### [Header].[Payload].[Signature]

| 구분 | 역할 |
| :---: | --- |
| **Header** | Token 생성 사용된 알고리즘, 토큰 종류 정보 포함 |
| **Payload** | 인증 정보 및 기타 *Claim 클레임* 정보 포함 |
| **Signature** | Header & Payload 정보를 서명한 정보 포함 |

#### Header

{% highlight js %}
{
  "typ": "JWT",     // "typ" : token 타입 정의
  "alg": "HS256"    // "alg" : 해싱 알고리즘 지정
}
// JSON string을 base 64로 Encoding 처리
{% endhighlight %}

#### Payload

* Payload 부분은 Token에 담을 정보(Claim)들을 포함
* Registerd(등록된) Claim : 이미 정해져있는 Token 정보

{% highlight js %}
{
  "iss": "jwttest.com",    // Token 발급자
  "sub": "jwttest",        // Token 제목
  "aud": "jwtuser",        // Token 대상자
  "exp": "20200090150630", // 만료 날짜로서 현재 날짜 이후로 지정 가능(NumericDate)
  "nbf": "20200831160000", // 활성화 날짜로서 해당 날짜가 지나야 Token 처리 가능(NumericDate)
  "iat": "20200831150630", // 발급 날짜(issued at)로서 Token의 age를 판단 가능(NumericDate)
  "jti": ""                // JWT의 고유 식별자로서 일회용 Token 사용할 때 유용
}
// JSON string을 base 64로 Encoding 처리
{% endhighlight %}

* Public(공개) Claim : 충돌 방지된(Collision-Resistant) 이름 형식인 URL 형식을 자기고 있는 정보

{% highlight js %}
{
  ...
  "https://jwttest.com": true
  ...
}
// JSON string을 base 64로 Encoding 처리
{% endhighlight %}

* Private(비공개) Claim : Registerd 나 Public 이 아닌 정보(충돌 가능)

{% highlight js %}
{
  ...
  "username": "jwtest"
  ...
}
// JSON string을 base 64로 Encoding 처리
{% endhighlight %}

#### Signature

* Header 와 Payload 값을 인코딩한 후 결합하여, 비밀키로 Hash하여 생성한 값

{% highlight js %}
const jwt = base64UrlEncode(header) + "." + base64UrlEncode(payload);
HMACSHA256(jwt, secret)
{% endhighlight %}

---

## Spring Security + JWT 토큰 인증 방식 구현

#### JwtTokenProvider
* JWT Token 생성 및 유효성 검증을 위한 Component 역할

#### JwtAuthenticationFilter
* 요청으로 들어온 Token의 유효성 인증을 위한 Filter 역할
* Security 설정 시, UsernamePasswordAuthenticationFilter 앞에 설정

#### SecurityConfiguration
* 서버의 보안 설정을 하는 Configuration 역할

**Resource 접근 제한 표현식**

| 표현식 | 의미 |
| :---: | :---: |
| hasIpAddress | IP주소가 매칭할 경우 |
| hasRole | 역할이 부여한 권한과 일치한 경우 |
| hasAnyRole | 부여된 역할 중 일치한 항목이 있는 경우 |
| permitAll | 모든 접근 승인 |
| denyAll | 모든 접근 거부 |
| anonymous | 익명의 사용자인지 확인 |
| authenticated | 인증된 사용자인지 확인 |
| rememberMe | 사용자가 'remember me' 사용해 인증인지 확인 |
| fullyAuthenticated | 사용자가 모든 Credential 갖춘 상태에서 인증했는지 확인 |

## User Service 구현
#### Custom UserDetailsService
* UserDetailsService class 재정의

#### User Entity
* UserDetails class 상속 받아 추가 정보 재정의

#### User JPA Repository
* findByUid method 추가

#### SignController
* 인증 성공시, 결과로 JWT token 발급
* 비밀번호 encoding 을 위해 PasswordEncoder 설정(기본 설정은 bcrypt encoding 사용)
***(Main Application class 에 PasswordEncoder Bean 추가)***

#### Swagger Header Field 추가
{% highlight java %}
@ApiImplicitParams({
  @ApiImplicitParam(name = "X-AUTH-TOKEN", value = "인증 성공 후 access_token", required = true, dataType = "String", paramType = "header")
})
{% endhighlight %}

---

## 추가
#### 예외 처리 보완
##### 예외 상황
1. JWT token 없이 API 요청한 경우 *- 403 Access Denied 에러*
2. 형식에 맞지 않거나 만료된 JWT token 으로 API 요청한 경우 *- 403 Access Denied 에러*
3. 유효한 JWT token 이지만, 권한이 없는 경우 *- 403 Forbidden 에러*

##### 403 Access Denied 예외 처리
* token 검증 단계에서 인증 처리가 불가능하기 때문에 끝나버리는 현상
* Spring Security 에서 제공하는 AuthenticationEntryPoint 를 상속 받아 redirect 처리

##### 403 Forbidden 예외 처리
* token 는 정상이지만 리소스에 대한 권한이 없는 경우
* Spring Security 에서 제공하는 AccessDeniedHandler 를 상속 받아 redirect 처리

---

#### Spring Security 관련 기타 Class 및 Interface
##### UserDetails Interface
* Spring Security 에서 사용자의 정보를 담는 Interface
* User Entity 를 UserDetails 상속을 받아 구현

{% highlight java %}
public class User implements UserDetails {
  private String userId;
  private String password;
  ...

  /**
   * Overiding method 들은 Security 환경에서 사용하는 회원 상태값이지만,
   * 사용하지 않기 때문에 모두 "true" 설정
   *
   * - isAccountNonExpired : 계정이 만료 안되었는지
   * - isAccountNonLocked : 계정이 잠긴 상태인지
   * - isCredentialsNonExpired : 계정 비밀번호가 만료된 상태인지
   * - isEnabled : 계정이 사용 가능한 상태인지
   */
  @Override
  public boolean isAccountNonExpired() { return true; }
  @Override
  public boolean isAccountNonLocked() { return true; }
  @Override
  public boolean isCredentialsNonExpired() { return true; }
  @Override
  public boolean isEnabled() { return true; }
}
{% endhighlight %}

##### UserDetailsService Interface
* DB 에서 사용자 정보를 조회하는 Interface
* loadUserByUsername() method 를 통해 UserDetails 형으로 사용자 정보를 저장

{% highlight java %}
public class TokenProvider {
  ...
  public Authentication getAuthentication(String userPk) {
    UserDetails userDetails = userDetailsService.loadUserByUsername(userPk);

    /**
     * UsernamePasswordAuthenticationToken.class
     * - AuthenticationFilter 등록하기 위한 Authentication 을 생성해주는
     *   Authentication Interface 의 구현체
     */
    return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
  }
  ...
}
{% endhighlight %}

##### SimpleGrantedAuthority Class
* Spring Security 에서 제공하는 권한 관리 Class
* 권한 명칭만 저장하는 구조로 설계

##### PasswordEncoder Class
* 단방향으로 변환하여 Password 를 안전하게 DB에 저장할 수 있는 Interface

{% highlight java %}
public class PasswordEncoderTest {

  private PasswordEncoder passwordEncoder;

  public PasswordEncoderTest() {
    passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  public void test() {
    String password = "password";
    String encode = passwordEncoder.encode(password);

    if (passwordEncoder.matches(password, encode)) {
      // True
    }
  }
}
{% endhighlight %}

---

[관련 Github Repository](https://github.com/JiYoonKimjimmy/demo-rest-api)
