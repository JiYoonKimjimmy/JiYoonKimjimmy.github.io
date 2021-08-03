---
layout: page
title: About
permalink: /about/
image: about.jpg
---

# Jimmyberg's 개발 소개서
금융 Hybrid App Application 개발 프로젝트를 통해 **Web UI/UX Front-End 개발**과 **Java 기반의 Back-End 개발**을 기반으로, 현재는 서비스의 인프라 구축과 시스템 아키텍처에 대한 관심으로 **Back-End 개발자**로서 일하고 있습니다.<br>
*지금까지의 경험과 꾸준한 노력을 바탕으로 주체적으로 가지고 있는 기술력을 활용하여 생활 속의 불편함을 찾아 해결하고, 조그만한 변화로 세상을 바꿀 수 있는 서비스의 개발자가 되는 꿈을 가지고 있습니다.*

**작은 고민이라도 게을리하지 않으며,<br>항상 자신의 것에 만족하지 않으며,<br>새로운 변화를 두려워 하지않으며,<br>나와 다른 의견일지라도 겸허하게 받아들이는<br>개발자가 되겠습니다.**

---

## Experience

### <strong>코나아이(Konai)</strong> <small>2020.11 ~ 현재</small>
#### 기술연구소 Server Developer
* 코나카드 서비스 개발/운영
* 지자체 지역화폐 서비스 개발/운영
* MSA 구조의 Core Server System 개발/운영
* Spring Boot 기반 Rest API Core Sever 개발


#### Projects
##### 부산 지역화폐 동백전 이관 프로젝트 <small>2021.02 ~ 2021.05</samll>
* 기존 부산 지역화폐 사업 이관 프로젝트 수행
* 회원 정보, 잔액 정보, 거래 내역 포함한 모든 Database Migration 처리
* 신규 회원 가입 or 기존 부산 지역화폐 사용 회원 정보 연동 처리
* `AS-IS` 부산 지역화폐 잔액 및 캐시백 정보 이관 처리
* 지역화폐 연동 체크 카드 정보 Migration 처리
* 체크 카드 BIN 정보를 코나 선불 카드와 연동 처리
* 1일 1회 체크 카드사 결제 취소 전문 파일 수신 및 회원 잔액 충전 Batch 시스템 개발

##### 휴면 회원 정책 적용 프로젝트 <small>2021.01 ~ 2021.03</small>
* 코나카드 전체 회원 1년 이상 미사용자 휴면 전환 정책 적용
* 1일 1회 휴면 전환 예정 안내/전환 실행 Batch 시스템 개발
* 회원 가입, Login, JWT Token 갱신 등 휴면 회원 여부 확인 API 수정 개발
* 회원 본인 인증 및 거래(카드 사용, 충전 등)를 통한 휴면 해제 API 신규 개발
* 휴면 전환/해제 History 관리

#### 담당 Server Core Component 관리 <small>2020.11 ~ 현재</small>
##### 회원 관리 Server Core Component
* 코나카드 전체 시스템 회원 관리 Rest API 신규/운영 개발
<br><br>
**Project Specs**
  * Spring Boot
  * JPA
  * QueryDSL
  * Oracle
  * Redis
  * RabbitMQ
  * Gradle
  * Jenkins

##### 부산 지역화폐 관리 Sever Core Component
* AS-IS 부산 지역화폐 이관 관리 Rest API 신규/운영 개발
* 부산 지역화폐 기존 회원 정보 연동
* 부산 지역화폐 기존 회원 잔액 이관 처리
* KT 구동백전 원본 DB 보관 및 관리
* 신규 DB 모델링 설계/운영
<br><br>
**Project Specs**
  * Spring Boot
  * Spring Batch
  * JPA
  * Oracle
  * Gradle
  * Jenkins

---

### 아톤(ATON) <small>2016.03 ~ 2020.10</small>
##### 금융 플랫폼 Server Developer
* KB국민은행, IBK기업은행, KEB하나은행 제1금융권 프로젝트 참여
* 자회사 아톤모빌리 중고차 유통 플랫폼 시스템 고도화 프로젝트 참여
* HTML, JavaScript, jQuery, Vue.js 를 활용한 Web Front-End 개발 경험
* Java, Spring Framework, JPA 등 Java 기반의 Back-End 개발 경험
* 각각 다른 협력사의 개발 환경에 맞춰 다양한 플랫폼 및 서비스 개발 경험
* 대형 프로젝트의 개발 프로세스 경험

#### Projects
##### 하나원큐 2.0 고도화 <small>KEB하나은행 (2020.03 ~ 2020.08)</small>
* 하나원큐 전체 서비스 UI/UX 개편 및 고도화 프로젝트 참여
* 고객이 선택한 Image와 문구가 담긴 메시지 카드와 전자지갑 기술을 접목한 **연락처 이체** 프로세스 UI/UX 개발
* CMS 시스템과 연동 후 Server에 저장되어있는 JSON File를 조회하여 HashMap형태의 Response Data를 반환하는 **공통 API** 개발

##### Liiv 및 리브똑똑 오픈뱅킹 구축 <small>KB국민은행 (2019.09 ~ 2020.01)</small>
* 타은행 계좌 등록하여 은행 업무를 이용할 수 있는 **오픈뱅킹** 등록 프로세스 UI/UX 개발
* 어카운트인포 및 오픈뱅킹 공통 표준 정책에 따른 타은행 계좌 이체 프로세스 개발
* 제로페이/뱅크페이/리브페이 이용 결제 프로세스 UI/UX 고도화

##### 리브똑똑 비대면인증 구축 <small>KB국민은행 (2019.06 ~ 2019.09)</small>
* 비대면 입출금 계좌 개설 프로세스 신규 개발
* 상품 안내 API 조회 후 상세 안내부터 **비대면 인증**, 개설 완료까지 UI/UX 개발
* 신분증 촬영 및 영상 통화 Native Module 연동

##### iONE Bank 2.0 고도화 <small>IBK기업은행 (2018.12 ~ 2019.06)</small>
* iONE Bank 전체 서비스 UI/UX 개편 및 고도화 프로젝트 참여
* 입출금 계좌 개설 및 카드 상품 **비대면 인증/가입** 프로세스 개발
* 메인 계좌뷰 설정 신규 개발
* 고객센터 전체 UI/UX 개발

##### 중고차 데이터 연동 REST API 개발 <small>아톤모빌리티 (2018.08 ~ 2018.12)</small>
* 하나캐피탈, 아주캐피탈 중고차 데이터 연동 인터페이스 구축
* Spring Boot 2.0, JPA, JOOQ, MySQL 활용한 연동 API Server 개발
* Jenkins, Docker 이용하여 배포 및 Server 관리
* **Vue.js**기반의 ADMIN 백오피스 개발

##### 카매니저 서비스 고도화 <small>아톤모빌리티 (2018.05 ~ 2018.08)</small>
* 중고차 딜러 서비스 **카매니저** UI/UX 전체 개편 및 API Server 재구축
* HTML, CSS, JavaScript, jQuery 활용한 UI/UX 개발
* PHP 기반 API Server를 **Spring Boot** 기반 API Server로 재구축

##### 리브똑똑 2.0 KB그룹 전체 조직도 통합 <small>KB국민은행 (2017.11 ~ 2018.02)</small>
* 약 7만명이 등록되어있는 KB금융그룹 전체 조직도 DB 통합 및 메신저 연동 서비스 개발
* DBLink를 활용한 각 계열사, 지주사 조직도 DB를 Oracle DB로 연동
* 7만건 데이터 조직도 테이블에 Merge Query 개발
* 조직별 조회 계층 Query 개발
* 1일 1회 새로운 조직도 연동 작업을 하는 **Batch** 프로그램 개발

##### KB스타뱅킹 5.0 고도화 <small>KB국민은행 (2017.09 ~ 2017.11)</small>
* KB스타뱅킹 5.0 전체 서비스 UI/UX 개편 및 고도화 프로젝트 공통 업무팀 참여
* **로그인 프로세스** 및 **전체 메뉴 구조** 개선
* Native 연동 JavaScript 공통 Interface 개발
* 보안매체(보안카드, OTP, 디지털OTP), ARS 인증 **공통 Tag Template** 개발

##### Liiv 2.0 고도화 <small>KB국민은행 (2017.05 ~ 2017.09)</small>
* Liiv 2.0 전체 서비스 UI/UX 개편 및 고도화 프로젝트 공통 업무팀 참여
* Native 연동 JavaScript 공통 Interface 개발
* 보안매체(보안카드, OTP, 디지털OTP), ARS 인증 **공통 Tag Template** 개발
* Main 화면, my Liiv(마이페이지) UI/UX 개발
* **Daum 지도 API** 활용한 영업점 찾기 UI/UX 개발

##### KB국민은행 FIDO 생체인증 시스템 구축 <small>KB국민은행 (2016.08 ~ 2017.05)</small>
* KB스타뱅킹, KB스타알림, KB스타뱅킹미니, Liiv와 같은 KB국민은행 모든 APP 서비스에 FIDO 생체인증 시스템 신규 개발 프로젝트의 PMO 업무 담당
* 프로젝트 **프로그램 파일 목록**, **업무 흐름도** 등 전체 설계 문서 작성
* Native 연동 JavaScript 공통 Interface 개발

---

### Skills
#### Back-End
* Java, Kotlin
* Spring Framework, Spring Boot, Spring Security, Spring Batch
* JPA, JOOQ
* Gradle, Maven

#### Front-End
* JavaScript ES5, ES6+
* Vue.js
* HTML, CSS, jQuery

#### DevOps
* Oracle, MySQL
* AWS - EC2, Elastic Cache for Redis, RDS(MariaDB), S3
* Jenkins, TravisCI, AWS Code Deploy
* Nginx, Tomcat
* Linux

#### Dev-Tool
* IntelliJ
* Eclipse
