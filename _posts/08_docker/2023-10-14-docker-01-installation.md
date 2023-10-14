---
layout: post
title : Docker.01 설치
date  : 2023-10-14
image : docker.png
tags  : docker container
---

## Docker

`Docker` 는 Application 애플리케이션을 가상 Container 컨테이너를 통해 환경 구축, 개발 및 배포를 효율적으로 지원하는 도구이다.

`Container` 컨테이너는 애플리케이션과 그 의존성을 하나로 묶는 패키지라는 개념이다.
가상머신과는 다른 개념이며, 컨테이너는 호스트 운영체제의 리소스를 공유하며, 가상머신보다 가볍고 빠른 장점이 있다.

컨테이너는 `Image` 이미지라는 객체 단위로 구축한 컨테이너 환경을 구성할 수 있고, 이미지를 통해 동일한 시스템 환경을 확장할 수 있다. 이러한 점에서 컨테이너 환경의 시스템 배포 환경은 유기적인 운영을 할 수 있는 장점을 가지게 된다.

---

### Docker 설치

{% highlight bash %}
# 1. `Docker` 설치
$ sudo yum install docker -y
# 2. `Docker` version 확인
$ docker -v
# 3. `Docker` service 실행
$ sudo service docker start
# 4. `Docker` 실행 확인
$ sudo systemctl status docker.service
{% endhighlight %}

> `yum -y` : 설치 과정에서 묻는 모든 과정을 `yes` 입력하여 설치되도록 하는 옵션

#### Docker 설정

##### EC2 인스턴스 접속 후 Docker 제어할 수 있도록 사용자 그룹 설정

{% highlight bash %}
$ sudo usermod -aG docker ec2-user
{% endhighlight %}

> `usermod -G` : 추가로 다른 그룹에 추가할 때 사용
> `usermod -a` : `-G` 옵션과 함께 사용하여 기존의 2차 그룹 외 추가로 2차 그룹을 지정할 때 사용

##### Docker 명령어 확인

{% highlight bash %}
$ docker run hello-world

# 출력 log 확인
Hello from Docker!
This message shows that your installation appears to be working correctly.

To generate this message, Docker took the following steps:
 1. The Docker client contacted the Docker daemon.
 2. The Docker daemon pulled the "hello-world" image from the Docker Hub.
    (amd64)
 3. The Docker daemon created a new container from that image which runs the
    executable that produces the output you are currently reading.
 4. The Docker daemon streamed that output to the Docker client, which sent it
    to your terminal.

To try something more ambitious, you can run an Ubuntu container with:
 $ docker run -it ubuntu bash

Share images, automate workflows, and more with a free Docker ID:
 https://hub.docker.com/

For more examples and ideas, visit:
 https://docs.docker.com/get-started/

{% endhighlight %}

> `docker run` : `DockerHub` 에 존재하는 **이미지**를 `pull` 하여 실행

---

#### 출처
- [Install Docker with Ubuntu](https://haengsin.tistory.com/128)
