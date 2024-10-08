# KONAi 프로젝트 정리 for Interview

## 회원 정보 관리 서비스

### 회원 가입

1. 통신사/통합 인증 정보 기준 회원 검증
   - 중복 회원 여부
     - 1차 : CI 기준 중복 회원 여부 확인
     - 2차 : 실명 & 휴대폰 번호 & 생년월일 & 성별 기준 중복 회원 여부 확인
   - 부정 사용자 등록 여부
     - 정책에 따른 부정 사용자 등록 식별자 기준 등록 여부 확인
     - SHA256(CI or 개인정보) Hash 데이터 식별자 생성
   - 부정 재가입 요청 여부
     - 체리-피커 회원 방지
     - `현재 일자 - 마지막 탈퇴 일자 < 재가입 방지 정책 일자` 인 경우, 가입 불가 처리
   - 개선 사항
     - 동일한 회원 인증 정보 수집에 대한 캐싱 처리
       - 이미 한번 수집된 회원 인증 정보로 회원 가입 검증 결과를 바로 응답 처리
       - 회원 가입 완료 시점 해당 캐시 정보 삭제 처리 필요
       - 캐시 구성 : { “<요청시간:CI:휴대폰번호>” :  “<검증 결과>” }
       - 캐시 만료 시간 : 1~5분 정도
2. 회원 정보 등록
   - 인증 정보 기준 회원 데이터 저장
   - 회원 간편 PIN 비밀번호 저장
     - PIN 비밀번호는 SHA256 단방향 암호화 저장
     - SHA256 암호화 문자열 Hex String 변환하여 저장(64바이트)
   - 서비스 이용 필수 약관 수집 & 동의 처리
3. 회원 부가 정보 등록
   - 회원 이용 단말 기기 정보 저장
   - 회원 가입 완료 메시지 발행 처리

---

### 회원 정보 관리

- 회원 정보 자체를 따로 캐싱 하고 있진 않음
- 승인, 카드 발급 등 주요한 서비스에서 필요한 회원 정보 조회는 모두 PK 인 회원코드로 조회하기 때문
- 휴대폰번호와 같은 조회 조건 컬럼 경우 인덱스 사용하여 성능 최적화

---

### 회원 로그인

- 회원 가입 로그인ID & PIN 비밀번호 기준 로그인 인증
- 로그인 인증 완료 후 JWT 토큰 발행
- JWT 토큰 인증 정보 구성
  - 회원 코드
  - 회원 로그인 ID
  - 휴대폰 번호
  - 현재 일자 timestamp
- JWT 생성 알고리즘
  - HS512 알고리즘 단방향 암호화
  - HMAC 단방향 암호화 Secret Key 자체 생성하여 APIGateway 공유
- JWT 토큰 인증 방식
  - JWT 토큰 APIGateway에서 Signature 인증 처리
  - 요청 토큰 Payload 회원 코드 & 요청 Header 회원 코드와 일치한다면, API 라우팅 처리
- JWT 만료 시간 설정
  - Access-Key : 30분
  - Refresh-Key : 2년
- 개선 사항
  - 서버 발급 JWT 토큰 검증 여부
    - 발급 완료된 JWT 토큰 Signature 정보 캐싱 처리
    - APIGateway 에서 인증 시 Signature 캐시 정보 없다면 API 요청 차단
    - API 요청 차단된 회원 다시 로그인 인증 처리 필요

---

### 회원 컴포넌트 개선 사항

- 회원 KYC 인증 체계 구축
  - 회원 등급별 가입을 위한 인증 체계 정규화
  - 회원 가입 인증 정보 KYC 저장
  - KYC 별 이용 가능한 서비스 제어 편의성 제공
- 회원 로그인 중복 요청 취약점 보완
  - 로그인 API 동일 네트워크 패킷 중복 요청에 대한 취약점 발견
  - Nonce 적용하여 중복 로그인 요청 취약점 보완

#### 개선 필요 사항

- OAuth 기반 회원 가입 & 로그인
  - OAuth 리소스 서버 제공 데이터 필수 항목 여부 확인
  - 허용 트래픽 제어 관리 필요

---

## 지역화폐 이관 지원 서비스

### ASIS 데이터 이관

1. ASIS 서비스 제공 업체 DB 데이터 파일 덤프 이관
2. ASIS 업체 DB 기준 데이터 마이그레이션
   - 회원 정보
   - 회원 잔액 정보
   - 회원 이용 은행/카드사 카드 정보

---

### 회원 이관 정보 연동

1. 신규 서비스 회원 가입 완료 시점 CI 정보 기준 ASIS 서비스 회원 정보 존재 여부 확인
2. ASIS 회원 정보 신규 서비스 회원 정보 연동
   - 회원 정보 연동
   - 회원 잔액 정보 신규 카드 충전 처리
   - ASIS 서비스 이용 내역 조회 API 제공

---

#### 개선 피료 사항

- 무중단 이관 프로세스
  - 특정 시점 기준 데이터 일괄 이관
  - 추가 생성 데이터 API 정의하여 데이터 연동

---

## 코나플레이트 APIGateway 서비스

- 선불카드 서비스 OpenAPI 제공
- OpenAPI 환경 API 인가/인증/라우팅을 위한 신규 APIGateway 개발
- Spring Cloud Gateway 기반 애플리케이션 구현
- Reactive Programming 활용한 비동기 처리 구현

### 제휴사 API 연동 관련 Key 발급

- API 접근 허용 인가를 위한 AccessKey 발급
- API 요청/응답 데이터 암복호화를 위한 CryptoKey 발급

---

### API 인가/인증 라우팅 처리

1. AccessKey 기준 요청 인가 확인
   - AccessKey 정보 DB 확인
   - AccessKey 상태 확인
   - API URL Hash 정보 DB 확인
2. 요청 HTTP Body 위변조 방지 토큰 확인
   - Body 데이터 String 변환 > MD5 Hash 암호화 > 요청 위변조 방지 토큰 일치 여부 확인
3. 요청 HTTP Body 복호화 처리
   - CryptoKey 사용하여 HTTP Body 복호화 처리
4. API 라우팅 처리
5. 응답 HTTP Body 암호화 처리
   - CryptoKey 사용하여 HTTP Body 암호화 처리
6. API URL 요청 호출 건수 집계 처리
   - "{<AccessKey>:<URL Hash>:<요청 일자>}" Cache Key 기준으로 일일 요청 건수 집계 저장

---

### 주요 API 서킷-브레이커 적용

- 회원, 카드, 승인 등 주요한 API 선별 서킷-브레이커 적용
- `Spring Cloud CircuitBreaker` 적용
  - `Resilience4j` 기반 서킷-브레이커 구현체 활용 가능
  - `Sliding Window` 알고리즘 기반 서킷-브레이커 제어 가능
  - `Properties` 설정대로 동적 API 별 서킷-브레이커 적용
- `Window size: 1000` 기준 `failure-rate: 20%` 인 경우 서킷-브레이커 **OPEN** 처리하도록 설정
 
---

### 주요 API 처리율-제한기 적용

- 주요한 API 기준 `Rate Limiter` 처리율 제한기 적용
- `TPS 2000` 초과 트래픽 `429 Too Many Requests` 에러 발생
  
#### 동적 트래픽 최대 허용치 제어

- 서킷-브레이커 상태 **OPEN to CLOSED** 변경되는 최초 시점 `TPS 1000` 적용
- 장애 다시 발생하여 2차 서킷-브레이커 상태 변경 시점 `TPS 500` 적용
- 서킷-브레이커 **CLOSED** 상태 20초 유지될 때마다 `TPS 500 > 1000 > 2000` 순서로 복원 처리

##### RedisRateLimiter 구현체 개발

- Lua 스크립트 활용한 RedisRateLimiter 구현체 개발
- 기본 토큰-버킷 알고리즘 기반 + 장애 카운트 캐시 Key 조합
- 장애 카운트 캐시 Key 증가할 때마다 허용 트래픽 절반 감소
- 장애 해소 시 20초 단위로 장애 카운트 캐시 Key 감소시켜 허용 트래픽 복구
  - Reactor 의 interval() 함수를 통해서 20초 단위 cache.decrement 구현
  - takeUntil() 함수를 통해서 조건 일치한 경우, interval() 함수 수행 종료
  - boundedElastic() 스켸쥴 전략으로 Blocking 작업을 비동기적으로 처리

---

## ES 기반 주소 연계 정보 연동 서비스

- 카드 매입사 가맹점 주소 기준 좌표 정보 수집을 위한 주소정보누리집 데이터 연동
- 주소정보누리집 수동 다운로드하여 데이터 마이그레이션 진행
- 주소정보누리집 연계 정보 수집 자동화를 위한 Batch 프로그램 & ELK 기반 데이터 파이프라인 구축
    
### ELK 기반 주소정보누리집 주소연계정보 수집

- 주소 연계 정보 물리 파일 다운로드 > Filebeat + Logstash 를 통해 ES 데이터 적재
- ES 선택한 이유
  - 비정형적인 주소 검색어를 최대한 정형화할 수 있는 애널라이저 적용 가능
  - 역인덱싱을 통해 RDB 보다 다양항 검색 조건으로 주소 조회 기능 제공 가능
    - RDB 는 성능 최적화를 위해 인덱스를 사용할 수 있지만, 무분별한 인덱스 사용은 역효과 발생

---

### 도로명/지번 주소 ES 조회 API 제공

- 도로명 / 지번 주소 각각 분리된 필드로 ES 인덱스 구성
- 클라이언트는 주소 조회 전 도로명 or 지번 주소 구분하여 ES 질의 요청
- 기본 주소와 상세 주소를 질의 필드 구분
  - 기본 주소가 더 많이 match 되는 주소가 더 score 높게 조회되도록 쿼리 구성

---

## 가상계좌 연동 서비스

- 제1금융권 은행사에서 발급/관리하는 가상계좌와 코나 결제플랫폼의 선불카드 연동/연계 서비스
- 코나카드 회원의 가상계좌 입금 금액을 선불카드 잔액 충전 기능 제공

### 가상계좌 정보 벌크 등록

- 가상계좌 정보 목록 파일 어드민 사이트 통하여 등록
- 어드민 사이트 전달받은 가상계좌 목록 `row` 별 **가상계좌 등록 Queue** 메시지 발행
- 서버에서 가상계좌 등록 처리 후 **가상계좌 등록 결과 Queue** 메시지 발행
- 어드민 사이트 비동기 데이터 처리 결과 확인

![가상계좌 등록 Flow](/images/vams_flow_01.png)

### 우리은행 전문 연동

- 우리은행과는 TCP 프로토콜 기반 전문 연동
- TCP 대외계 컴포넌트 통해 전문 연동 처리
- 명확한 트랜잭션 처리를 위해 `VAMS` 시스템 장애 발생 시, `FEP` 즉각적인 에러 처리

![가상계좌 등록 Flow](/images/vams_flow_02.png)


### 가상계좌 연동 선불카드 충전 거래

- 우리은행 가상계좌 입금 전문 요청 기준 선불카드 충전 처리
- 가상계좌 연동 카드 정보 조회하여 시스템 충전 요청 처리
- 충전 요청 성공인 경우, 충전 금액, 건수 등 데이터 집계 처리
- 충전 요청 실패인 경우, 실패 사유 기준 에러 코드 응답 처리
- 전문 요청에 대한 모든 정보 로그성 이력 저장

### 가상계좌 입금/취소 데이터 집계

- 가상계좌 입금/취소 전문 요청 성공 시, 충전/취소 금액 & 건수 집계
- 일자별 Cache Key 생성하여 Redis 저장 처리
  - Redis 선택한 이유
    - Redis 는 싱글-스레드 동작 방식으로 동시성 이슈 최소화하여 데이터 집계 가능

---
