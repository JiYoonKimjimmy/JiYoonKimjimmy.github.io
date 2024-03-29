---
layout: post
title : AWS 다양한 Service 정리
date  : 2023-08-24
image : aws_basic.png
tags  : aws
---

## AWS Services

### EBS (Elastic Block Store)

- `EBS` 는 `EC2` 인스터스에서 사용할 수 있는 **영구적인 블록 스토리지 볼륨을 제공한다.**
- 각 `EBS` 볼륨은 가용 영역 내 자동으로 복제되어 구성요소 장애로부터 보호하고, 고가용성 및 내구성을 제공한다.
- `EC2` 는 메모리, 그래픽카드 등 하드디스크를 제외한 컴퓨터의 모든 부분이라면,
- `EBS` 는 하드디스크를 담당하기 때문에 영구적인 보관을 필요하다면 활용 가능하다.

---

### EFS (Elastic File system)

- `EFS` 는 AWS 클라우드 서비스와 온프레미스 리소스에서 사용할 수 있는 탄력적인 파일 스토리지 기능을 제공한다.
- Linux 인스턴스를 위한 확장성, 공유성 높은 파일 스토리지로 활용가능하며,
- `EC2` Linux 인스턴스에 마운트된 `NFS (Network File System)` 을 통해 `VPC` 에서 필요한 파일 접근 또는 `AWS Direct Connect` 로 온프레미스 서버의 파일에 접근 가능하다.
  - 온프레미스 환경의 `NFS`, `NAS` 폴더와 비슷한 서비스 역할

---

### DataSync

- `DataSync` 는 데이터 마이그레이션을 간소화하고, AWS 스토리지 서비스 간에 파일 또는 객체 데이터를 안전하게 전송할 수 있도록 지원한다.
- 사용 사례
  - 데이터 검색 : 온프레미스 스토리지 성능 및 활용도를 파악
  - 데이터 마이그레이션 : 네트워크를 통해 활성 데이터 세트를 AWS 스토리지 서비스로 빠르게 이동
  - 콜드 데이터 아카이브 : 온프레미스 스토리지에 저장된 콜드 데이터를 `S3 Glacier Flexier Flexier Retrieval` 또는 `S3 Glacier Deep Archive` 와 같은 안정적이고 안전한 장기 스토리지 클래스로 직접 이동
  - 데이터 복제 : 요구 사항에 가장 비용 효율적인 스토리지 클래스를 선택하여 데이터를 원하는 `Amazon S3` 스토리지 클래스에 복사
  - 시기 적절한 클라우드 내 처리를 위한 데이터 이동 처리를 AWS 위해 데이터를 내부 또는 외부로 이동

---

### DMS (Database Migration Service)

- `DMS` 는 관계형 DB, 데이터 웨어하우스, NoSQL DB 등 데이터 저장소를 마이그레이션 할 수 있도록 지원
- `DMS` 를 사용하면 원본 데이터 저장소를 검색하고, 원본 스키마를 변환하여 데이터를 마이그레이션 처리
- 초기에 백업 후 한번 밀어 넣고, CDC 기능을 통해서 실시간 데이터 공유
- `DMS` 는 이기종 DBMS 에서 각각 다른 복제/복구 기능을 지원

##### 예시
1. IDC 오라클 DB > 클라우드 오라클 DB > DMS > OSS DB
2. 라이브되는 서비스에서는 > CDC > DMS > OSS DB

##### DMS 마이그레이션 프로세스

![DMS 마이그레이션 프로세스](/images/aws_dms_01.png)

---
