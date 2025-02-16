---
layout: post
title : Spring Boot DataSource decorator
date  : 2025-02-16
image : spring_basic.png
tags  : springboot-datasource-decorator
---

## Spring Boot DataSource decorator 소개

`Spring Boot DataSource decorator` 는 기존 `DataSource` 를 데코레이팅하여 다양한 기능을 추가할 수 있도록 돕는 라이브러리이다.

`P6Spy` 와 `Datasource Proxy`, `FixyPool` 를 활용하여 데이터베이스와 상호 작용하는 동작을 모니터링하고, 성능을 최적화하며, 로깅을 개선할 수 있도록 기능 제공한다.

라이브러리의 다양한 기능을 제공하고 있으나, 기본 Spring Data JPA 에서 제공하는 **쿼리 로깅을 보완**하기 위한 P6Spy 기능 사용 목적으로 내용 정리할 예정이다.

### 주요 기능 정리

- **P6Spy** : SQL Query 로깅하고 분석 기능 제공
- **Datasource Proxy** : Query 로깅 & 성능 모니터링을 위한 Proxy 기능 제공
- **FixyPool** : 데이터베이스 Connection Pool 성능 최적화 & 모니터링 기능 제공

---

### P6Spy

`P6Spy` 는 데이터베이스로 요청하는 쿼리를 **로깅**하고, 분석하는 기능을 제공한다.

Spring Data JPA 를 사용한다면, 이미 요청 쿼리에 대한 로깅이 가능하지만, **쿼리의 `WHERE` 절에 있는 조건값에 대한 상세한 정보를 제공하지는 않는 점**에서 `P6Spy` 적용을 검토하게 되었다.

##### P6Spy 적용

```gradle
dependencies {
    implementation("com.github.gavlyukovskiy:p6spy-spring-boot-starter:${p6spyVersion}")
}
```

> `P6Spy` 최신 버전 : `1.10.0` (2025/02/16 기준)
> <br>
> `Spring Boot 3.x` 이상 버전은 `P6Spy 1.9.0` 이상 적용 권장

##### P6Spy 적용 전

```sql
select * from accounts where account_no='1234567890';
-- 실제 애플리케이션 로깅은 아래와 같이 출력된다.
select ae1_0.id,ae1_0.account_no,ae1_0.amount,ae1_0.before_amount from accounts ae1_0 where ae1_0.account_no=?
```

##### P6Spy 적용 후

```sql
2025-02-16T21:52:01.641+09:00  INFO 1054 --- [pool-1-thread-1] p6spy                                    : #1739710321641 | took 2ms | statement | connection 6| url jdbc:h2:mem:52b13ff0-b6f9-4c7d-98c6-4fa2e083f8b0
select ae1_0.id,ae1_0.account_no,ae1_0.amount,ae1_0.before_amount from accounts ae1_0 where ae1_0.account_no=?
select ae1_0.id,ae1_0.account_no,ae1_0.amount,ae1_0.before_amount from accounts ae1_0 where ae1_0.account_no='887db8dbd2';
```

#### P6Spy 설정

- `P6Spy` 설정은 라이브러리 의존성 추가만으로 **Auto-Configuration** 으로 자동설정된다.
- 상세한 설정이 필요한 경우, 프로젝트 Properties 파일에서 설정 가능하다.

```properties
# P6Spy 로깅 활성화
decorator.datasource.p6spy.enable-logging=true
# 멀티라인 포맷 사용
decorator.datasource.p6spy.multiline=true
# 로깅 방식 설정 (slf4j, sysout, file, custom)
decorator.datasource.p6spy.logging=slf4j
# 파일 로깅 시 로그 파일 경로 설정
decorator.datasource.p6spy.log-file=spy.log
# 커스텀 로거 클래스 설정 (custom 로깅 사용 시)
decorator.datasource.p6spy.custom-appender-class=my.custom.LoggerClass
# 커스텀 로그 포맷 설정
decorator.datasource.p6spy.log-format=
# 로그 메시지 필터링을 위한 정규식 패턴
decorator.datasource.p6spy.log-filter.pattern=
```

---

#### Reference

- [gavlyukovskiy - Spring Boot DataSource Decorator](https://github.com/gavlyukovskiy/spring-boot-data-source-decorator)

---