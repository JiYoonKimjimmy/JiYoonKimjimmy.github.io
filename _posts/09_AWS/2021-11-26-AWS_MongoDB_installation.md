---
layout: post
title : MongoDB 설치하기
date  : 2021-11-26
image : mongodb-01.jpg
tags  : aws mongodb
---

## MongoDB 설치

`EC2` 설치 또는 Linux OS 환경 구성이 선행되었다는 가정하에 `MongoDB` 를 설치해보겠다.
그리고 해당 방법의 기준 `OS` 는 `Amazon Linux 2` 로 결정하였다. 다양한 `OS` 의 설치 방법은 [`MongoDB` 공식 홈페이지](https://docs.mongodb.com/manual/installation/)에서도 충분히 잘 설명해주고 있는 것 같다.

`MongoDB` 설치 방법은 2가지 방법으로 정리하였다.

1. `yum` 활용한 설치 방법
2. `Tarball` 활용한 설치 방법

인터넷망이 제한되는 환경인 경우, 공식 홈페이지에서 `tar` 파일을 받아서 설치하는 방법도 함께 정리하였다.

----

### `yum` 활용한 설치 방법
#### 1. `yum` 으로 설치하기 전에, `yum` 설정에 `MongoDB` 관련 설정 파일이 추가한다.

{% highlight bash %}
$ vi /etc/yum.repos.d/mongodb-org-5.0.repo

# /etc/yum.repos.d/mongodb-org-5.0.repo
[mongodb-org-5.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/amazon/2/mongodb-org/5.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://www.mongodb.org/static/pgp/server-5.0.asc
{% endhighlight %}

#### 2. `yum install` 를 통해서 `MongoDB` 설치

{% highlight bash %}
# mongodb 기본 설치
$ sudo yum install -y mongodb-org

# 특정 release 의 패키지를 설치 싶을 때
$ sudo yum install -y mongodb-org-5.0.2 mongodb-org-database-5.0.2 mongodb-org-server-5.0.2 mongodb-org-shell-5.0.2 mongodb-org-mongos-5.0.2 mongodb-org-tools-5.0.2

# 특정 패키지를 제외하고 설치하고 싶을 때 `/etc/yum.conf` 수정
exclude=mongodb-org,mongodb-org-database,mongodb-org-server,mongodb-org-shell,mongodb-org-mongos,mongodb-org-tools
{% endhighlight %}

#### 3. `MongoDB` 프로세스 실행

{% highlight bash %}
# 프로세스 실행
$ sudo systemctl start mongod
# `Failed to start mongod.service: Unit mongod.service not found.` 에러 발생한 경우, 아래 명령어 실행후 재실행
$ sudo systemctl daemon-reload
# 프로세스 상태 확인
$ sudo systemctl status mongod
# 재부팅후 상태 확인하기
$ sudo systemctl enable mongod
# 프로세스 중지
$ sudo systemctl stop mongod
# 프로세스 재시작
$ sudo systemctl restart mongod
{% endhighlight %}

> ##### Init System
> 
> 리눅스 환경에서 `MongoDB` 의 프로세스 실행하고 관리하기 위해서 [`init system`](https://docs.mongodb.com/manual/reference/glossary/#std-term-init-system) 을 사용하게 된다.
> 
> 최신 리눅스 환경에서는 `systemd` 를 사용을 주로하지만, 이전 버전에서는 `System V init` 를 사용하는 경우가 있고, 그 둘의 실행 방법은 다르기 때문에 아래와 같은 명령어로 확인할 수 있다.
> 
> ```shell
> $ ps --no-headers -o comm 1
> ```


---

## `Tarball` 활용한 수동 설치 방법

### 1. MongoDB Community Edition `tar.gz`  다운로드

> [https://www.mongodb.com/try/download/community](https://www.mongodb.com/try/download/community)

##### 설치 대상 서버 OS 확인 방법

```shell
# 커널 이름, 버전, 호스트명 등 커널 관련 정보 확인
$ uname -a 
# OS 이름, 버전, ID 등 상세한 정보 확인
$ cat /etc/os-release
```

> ##### 설치 서버 정보
> 
> - **OS** : Rocky Linux 9.5 (RHEL 9 기반)
> - **아키텍처** : x86_64 (64bit)
> - **OpenSSL 버전**: 3.x (Rocky 9의 기본)
> 
> 설치 파일 : `RedHat / CentOS 9.3 x64` 플랫폼 선택 > `mongodb-linux-x86_64-rhel93-8.0.10.tgz` 파일 다운로드

### 2. `tar.gz` 파일 압축 해체 & 실행 파일 환경 구축

- **작업 위치 : /home/mock_server/mongodb**

```shell
$ tar -zxvf mongodb-linux-x86_64-rhel93-8.0.10.tgz
$ cp -R mongodb-linux-x86_64-rhel93-8.0.10/* /home/mock_server/mongodb/bin
$ export PATH=/home/mock_server/mongodb/bin:$PATH
$ source ~/.bashrc
```

### 3. MongoDB 실행 환경 구축

#### `db` & `log` 디렉토리 생성

```shell
$ mkdir -p /home/mock_server/mongodb/db
$ mkdir -p /home/mock_server/mongodb/log
```

#### `mongod.conf` 파일 생성

- **파일 위치 : /home/mock_server/mongodb/cfg**

```properties
systemLog:
  destination: file
  path: /home/mock_server/mongodb/log/mongod.log
  logAppend: true

storage:
  dbPath: /home/mock_server/mongodb/db

net:
  bindIp: 0.0.0.0   # 외부 접속 허용
  port: 27017

processManagement:
  fork: true        # 백그라운드 실행

```

### 4. MongoDB 실행

```shell
$ mongod --config /data/home/mock_server/mongodb/cfg/mongod.conf
```

### 5. MongoDB 접속

#### Mongosh 설치

> [https://www.mongodb.com/try/download/shell](https://www.mongodb.com/try/download/shell)
> 
> - 설치 파일 : `Linux x64` 플랫폼 선택 > `mongosh-2.5.2-linux-x64.tgz` 파일 다운로드

```shell
$ tar -zxvf mongosh-2.5.2-linux-x64.tgz
$ cp -R mongosh-2.5.2-linux-x64/bin/* /home/mock_server/mongodb/bin
$ source ~/.bashrc
```

#### Mongosh 접속

```shell
$ mongosh
```

### 6. MongoDB 종료

```shell
$ mongod --config /data/home/mock_server/mongodb/cfg/mongod.conf --shutdown
```

---

## MongoDB 계정 생성

### Admin 계정 생성

```json
use admin
db.createUser({ 
	user: "admin", 
	pwd: "<password>", 
	roles: [ "userAdminAnyDatabase", "dbAdminAnyDatabase", "readWriteAnyDatabase" ] 
})
```

### Custom 계정 생성

```json
use ms
db.createUser({ 
	user: "mock", 
	pwd: "Kona!234", 
	roles: [ "dbAdmin", "readWrite" ] 
})
```

---

## MongoDB 백업

### MongoDB Database Tools `tar.gz` 다운로드

> [https://www.mongodb.com/try/download/database-tools](https://www.mongodb.com/try/download/database-tools)
> 
> - 설치 파일 : `RedHat / CentOS 9.3 x86_64` 플랫폼 선택 > `mongodb-database-tools-rhel93-x86_64-100.12.2.tgz` 파일 다운로드

```shell
$ tar -zxvf mongodb-database-tools-rhel93-x86_64-100.12.2.tgz
$ cp -R mongosh-2.5.2-linux-x64/bin/* /home/mock_server/mongodb/bin
$ source ~/.bashrc
```

### MongoDB 백업하기: `mongodump`

```shell
$ mongodump --db <DB명> --out <백업 디렉토리>
```

### MongoDB 백업 파일 복원: `mongorestore`

```shell
$ mongorestore --db <DB명> --drop <백업 디렉토리>
```

- `--db` : 복원할 대상 DB 이름
- `--drop` : 기존 동일한 컬렉션이 있다면 삭제 후 overwrite
- `<백업 디렉토리>` : 기존 `mongodump` 를 통한 백업 파일 디렉토리 경로

---

##### etc. Linux 계정/그룹 관리

```shell
# 1. 공용 그룹 생성
$ sudo groupadd mongogrp

# 2. root & 현재 계정 그룹 추가
$ sudo usermod -aG mongogrp root
$ sudo usermod -aG mongogrp mongod
$ sudo usermod -aG mongogrp $(whoami)

# 3. 특정 디렉토리 그룹 소유자 변경
$ sudo chown -R mock_server:mongogrp /data/mongodb

# 4. 그룹 쓰기 권한 + 디렉토리 상속 설정
$ sudo chmod -R 2775 /data/mongodb
```

---

##### etc. Linux `systemd` 서비스 등록

```shell
# 1. `systemd` 서비스 파일 생성
$ sudo cp -R /home/mock_server/mongodb/01_setup/mongodb-linux-x86_64-rhel93-8.0.10/* ./
$ sudo vi /etc/systemd/system/mongod.service

# 2. /etc/systemd/system/mongod.service 내용 작성
[Unit]
Description=MongoDB Database Server (Custom Install)
After=network.target

[Service]
User=mock_server
Group=mongogrp
ExecStart=/opt/mongodb/bin/mongod --config /home/mock_server/mongodb/cfg/mongod.conf
PIDFile=/home/mock_server/mongodb/mongod.pid
RuntimeDirectory=mongodb
Restart=on-failure
LimitNOFILE=64000

[Install]
WantedBy=multi-user.target

# 3. 서비스 등록 및 활성화
$ sudo systemctl daemon-reexec
$ sudo systemctl daemon-reload
$ sudo systemctl enable mongod
$ sudo systemctl start mongod

# 4. 상태 확인
$ sudo systemctl status mongod

# etc. 커널 로그 확인
journalctl -xe -u mongod
```

---

#### 출처
- [MongoDB 공식 홈페이지](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-amazon/)
