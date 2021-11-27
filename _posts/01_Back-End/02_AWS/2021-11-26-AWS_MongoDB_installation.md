---
layout: post
title : EC2 + MongoDB 설치하기
date  : 2021-11-26
image : mongodb-01.png
tags  : aws mongodb
---

## MongoDB 설치 (+ EC2)
`EC2` 설치는 선행되었다는 가정하에 `MongoDB` 를 설치해보겠다.
그리고 해당 방법의 기준 `OS` 는 `Amazon Linux 2` 로 결정하였다. 다양한 `OS` 의 설치 방법은 [`MongoDB` 공식 홈페이지](https://docs.mongodb.com/manual/installation/)에서도 충분히 잘 설명해주고 있는 것 같다.

### 설치 방법
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

##### 기본적인 `MongoDB` 인스턴스
- 데이터 파일 : `/var/lib/mongo`
- 로그 파일 : `/var/log/mongodb`

---

### `MongoDB` 프로세스 실행

#### Init System
리눅스 환경에서 `MongoDB` 의 프로세스 실행하고 관리하기 위해서 [`init system`](https://docs.mongodb.com/manual/reference/glossary/#std-term-init-system) 을 사용하게 된다.
최신 리눅스 환경에서는 `systemd` 를 사용을 주로하지만, 이전 버전에서는 `System V init` 를 사용하는 경우가 있고, 그 둘의 실행 방법은 다르기 때문에 아래와 같은 명령어로 확인할 수 있다.

{% highlight bash %}
# os 환경의 `init system` 확인 방법
$ ps --no-headers -o comm 1
# systemd
{% endhighlight %}

#### 1. 실행시키기

{% highlight bash %}
$ sudo systemctl start mongod

# `Failed to start mongod.service: Unit mongod.service not found.` 에러 발생한 경우, 아래 명령어 실행후 재실행
$ sudo systemctl daemon-reload
{% endhighlight %}

#### 2. 실행 상태 확인하기

{% highlight bash %}
$ sudo systemctl status mongod

# 재부팅후 상태 확인하기
$ sudo systemctl enable mongod
{% endhighlight %}

#### 3. 중지시키기

{% highlight bash %}
$ sudo systemctl stop mongod
{% endhighlight %}

#### 4. 재시작

{% highlight bash %}
$ sudo systemctl restart mongod
{% endhighlight %}

---

### `MongoDB` 설정 및 외부 접속

#### 1. `MongoDB` 접속 가능 IP 정보 변경
`/etc/mongod.conf` 파일에서 `bindIp` 의 `127.0.0.1` 인 부분을 `0.0.0.0` 으로 수정

{% highlight bash %}
$ vi /etc/mongod.conf

26 # network interfaces
27 net:
28   port: 27017
29   bindIp: 0.0.0.0  # Enter 0.0.0.0,:: to bind to all IPv4 and IPv6 addresses or, alternatively, use the net.bindIpAll setting.
{% endhighlight %}

#### 2. 계정 생성

{% highlight bash %}
# `MongoDB` 접속
$ mongo

# `admin` 계정 생성
use admin
db.createUser({
    user: "<username>",
    pwd: "<password>",
    roles: [
        "userAdminAnyDatabase",
        "dbAdminAnyDatabase",
        "readWriteAnyDatabase"
    ]
})

# `customDB` 계정 생성
use <customDB>
db.createUser({
    user: "<username>",
    pwd: "<password>",
    roles: [
        "dbAdmin",
        "readWrite"
    ]
})
{% endhighlight %}

> `MongoDB` 계정 삭제하는 방법<br>
> use \<customDB><br>
> db.dropUser("\<username>")

#### 3, `EC2` 보안 그룹 설정 변경
- `MongoDB` 접속 `port(default: 27017)` 맞게 인바운드 규칙 추가

#### 4. `MongoDB` 외부 접속
- `EC2` host url 과 `MongoDB` 의 `port` 사용하여 정상 접속 확인!

---

#### 출처
- [MongoDB 공식 홈페이지](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-amazon/)
