---
layout: post
title : AWS Webina - 금융사의 클라우드 도입
date  : 2023-07-26
image : aws-cloud-webina-2023-00.png
tags  : aws awscloudwebina
---

## 금융사의 클라우드 도입 첫걸음을 위한 웨비나

### 금융사의 클라우드 도입, 왜 해야 하나요? <small>실제 사례와 사용 규제 업데이트</small>

#### 왜 클라우드를 사용하는가?

- 탄력적, 고성능 리소스를 클라우드로 빠르게 구축하여 비용 절감
- 저축은행, 캐피탈의 신용평가시스템(CSS) 시스템 적용 사례
  - 노후화된 인프라 및 솔루션 교체
  - SAS Viya 시스템 활용을 위한 고성능 Core 배치
  - EKS 스케쥴링을 통한 비용 경제적 운영
  - AI, ML 고성능 최신 기술 적용을 위한 유연한 환경 마련
- 증권: MTS 시스템 적용 사례
  - 대형 IPO 열풀 및 일평균 이용 증가에 따른 유연한 리소스 관리
- 중요 정보를 클라우드 저장 가능?
  - 2019년 1월 이후로 금융정책법 상 중요 정보 저장 가능하도록 허용
- 2023년 전금법 변화에 따른 클라우드 대응 가능?
  - 금융보안원에게 안전성 결과 공유를 요청하여 클라우드 활용 가능
  - 사후 보고: 클라우드 활용 시기 3개월 내 사후 보고 필수
- 클라우드 이용 보고를 위한 절차가 필요하고, 이또한 AWS 컨설팅 가능
  - 너무 복잡쓰
- 클라우드에 대한 안정성 평가?
  - 금융보안원의 2023 CSP 안정성 평가를 완료
  - M 뭐시기 level 3 보안 수준 확보
- AWS 클라우드 적용에 대한 프로그램은?
  - 클라우드 적용 전약 : DIP - 새로운 비지니스 구축을 위한 디지털 솔루션 기획 워크샵
  - 마이그레이션 현황 분석 : MRA - 클라우드 도입 준비 상태 점검
  - 외 보안진단, 가드레일 구축, PoC 지원

---

### 금융사의 클라우드 도입, 얼마나 가치가 있나요? <small>무료 TCO 분석 프로그램 소개</small>

> TCO : Total Cost Ownership

#### Cloud Economics

- 회사의 정보에 근거한 클라우드 적용의 의사결정을 지원
- AWS 효율적으로 선택하고 사용할 수 있도록 가이드 지원
- 클라우드의 비즈니스 가치는 단순 사내 리소스 비용 절감 뿐만 아니라 다양한 측면에서 클라우드 이점 발생

#### Business Value Framework

- AWS 에서 회사의 정보/리소스를 분석하여 비용 측정
- AWS 자체 툴과 프로세스를 통해 분석
- First Call > 팀 빌딩 > 정보 수집 > 분석 및 보완 > 최종 보고
- 서비스가 운영되고 있는 서버 리소스에 대한 정보를 기반으로 분석

#### Business Case Report

- 리소스를 분석한 자료를 시각화하여 자세하게 보고서를 작성.. 대단쓰

---

### 금융사의 클라우드, 어떻게 할 수 있나요? <small>AWS 를 활용한 손쉬운 데이터 마이그레이션</small>

#### 클라우드 마이그레이션

- 클라우드 마이그레이션의 필요성은 "새 음식은 새 그릇에 담아야 한다."
- 클라우드 전환 이점 : 데이터레이크 / 앱현대화 / 미션크리티컬 / 파일공유 / 백업&복원 / 아카이빙
- 클라우드 도입에 대한 허들을 해결하기 위해 다양한 AWS 서비스를 활용
- 클라우드 마이그레이션 여정
  1. 조직 평가 : 조직의 준비 상태를 분석 및 평가
  2. 평가 활용 : 조직 평가를 기반으로 마이그레이션 준비
  3. 마이그레이션 및 현대화
- EFS?
- AWS 현대화 방식
  - 온프레미스 서버를 실시간 동기화를 통해 AWS 클라우드와 동기화 가능
- Amazon EBS : EC2 와 함께 사용하도록 설계하면 고성능 스토리지 시스템으로 활용 가능 (서버 백업용)
- Amazon EFS : 운영체제별 파일 스토리지 서비스 지원
- Amazon S3 : key-value 형태의 데이터 저장 구조로 무제한에 가까운 스토리지 용량과 오브젝트 제공
- AWS 는 100개 가까운 리전을 관리하고 있으며, 각 리전 간의 이중화, 전용 네트워크를 통해서 백업 프로세스 구축
- 데이터 마이그레이션을 위한 프로세스를 제공
- 데이터 마이그레이션을 위한 5 가지 주요 질문
  1. 어떤 데이터를 어디로 이전?
  2. 일회성 또는 지속적인 복제?
  3. 단방향 또는 양방향?
  4. 데이터 양과 예상 소요 시간?
  5. 네트워크 대역폭?
- 온프레미스에 AWS 의 서비스를 설치하여 AWS 클라우드 서버로 백업/동기화 처리

---