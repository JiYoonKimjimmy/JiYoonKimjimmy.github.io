---
layout: post
title : Token Authorization
date  : 2021-02-21
image : token_authorization.jpg
tags  : back-end etc authorization
---

## 서버 기반 인증의 문제점
### Session 문제
Session에 사용자의 인증 정보를 계속 저장해둔다면, 서버의 RAM 과부하의 문제와 Session 정보를 Database화 하더라도 Database 성능 저하 문제가 발생한다.
### 서버의 확장성
여러개의 서버 프로세스를 진행할 때의 성능 저하가 발생 및 방대한 트래픽 감당 힘들 수 있다.
### CORS ***(Cross Origin Resource Sharing)***
웹 어플리케이션에서 Session을 관리할 때 사용되는 Cookie는 단일 도메인 및 서브 도메인에서만 동작이 가능하나, 이런 Cookie를 여러 도메인에서 관리가 번거롭다.

## Token 기반 인증?
### Token 기반의 시스템 동작 원리
1. 사용자가 로그인을 요청
2. 해당 계정 정보를 서버에서 검증
3. 계정 정보가 정확하다면, ***singed*** **Token**을 발급
4. 클라이언트에서 **발급받은 Token을 저장**
5. 클라이언트에서 서버로 요청시 **저장한 Token정보를 함께 전달**
6. 서버는 **Token정보를 검증하고,** 요청에 대한 응답 처리

### Token 인증의 장점
* **Stateless(무상태)** : 서버에 사용자의 상태를 유지하지 않는 구조로서, 사용자의 정보를 서버나 Session에 저장하지 않는다.
* **Scalability(확장성)** : 서버 기반 인증과는 달리 서버를 계속 확장하여 어느 서버로 요청을 하더라도 상관이 없다.
* **Security(보안성)** : 서버로 요청시 Cookie를 사용하지 않으므로 Cookie 사용에 대한 보안 취약성 해결하지만, ***Token 인증에 대한 취약성을 보완해야한다.***
* **Extensibility(확장성)** : 계정 정보에 대한 확장성을 가지고 있기 때문에, Token별 선택적인 권한 부여가 가능하며, 다양한 플랫폼과 도메인의 환경에 영향받지 않고 인증 프로세스 진행이 가능하다.
* **웹 표준 기반** : Token 기반 인증 구현체인 **JWT** 는 웹 표준 ***RFC 7519*** 에 등록되어 있기 때문에 다양한 환경 지원이 가능하다.
