---
layout: post
title : Java 21 JVM Option
date  : 2025-04-28
image : java_log.png
tags  : java jdk21 jvm jvamoption
---

## Java 21 JVM Option 설정 튜닝

### Problem

#### JVM 옵션 튜닝 포인트

- **메모리 설정**: 
  - 초기 힙 크기가 작아 동적 증가 시 오버헤드 발생
  - NewRatio 설정으로 인한 불필요한 메모리 조정
- **GC 설정**: 
  - ZGC는 2GB 정도의 힙 크기에서는 오버스펙
  - STW(Stop-The-World) 시간 제한이 없어 예측 불가능한 지연 발생
- **로깅 설정**: 
  - 구버전/신버전 설정 혼용으로 인한 로깅 무효화
  - 불필요한 로그 정보 수집

#### 개선 목표

- **안정성**: 
  - 예측 가능한 GC 동작
  - 안정적인 메모리 사용
- **성능**: 
  - 최소한의 GC 오버헤드
  - 효율적인 메모리 활용
- **모니터링**: 
  - 명확한 GC 로그 수집
  - 문제 발생 시 빠른 원인 파악

---

### Solution

#### 기존 설정

```shell
## Configuration ###############################################################
JAVA_OPTS="$JAVA_OPTS -Xms512m -Xmx2048m -XX:NewRatio=2"
JAVA_OPTS="$JAVA_OPTS -Xss32m"
JAVA_OPTS="$JAVA_OPTS -XX:SurvivorRatio=4"
JAVA_OPTS="$JAVA_OPTS -XX:+UseZGC"
JAVA_OPTS="$JAVA_OPTS -Xlog:safepoint=debug"
JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails"
JAVA_OPTS="$JAVA_OPTS -verbose:gc -Xloggc:$GCLOG"
JAVA_OPTS="$JAVA_OPTS -Xlog:gc*"
JAVA_OPTS="$JAVA_OPTS -Xlog:gc+heap=debug"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=$GCDIR/$SERVICE_NAME-java_pid.hprof"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF8"
```

#### 신규 설정

```shell
## Configuration ###############################################################
# 1. 힙 메모리 설정 변경
#JAVA_OPTS="$JAVA_OPTS -Xms512m -Xmx2048m -XX:NewRatio=2"
JAVA_OPTS="$JAVA_OPTS -Xmx2048m -Xmx2048m"

# 2. 스레드 스택 크기 설정 제거
# JAVA_OPTS="$JAVA_OPTS -Xss32m"

# 3. GC 설정 변경
#JAVA_OPTS="$JAVA_OPTS -XX:+UseZGC"
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 4. GC 로깅 설정 통합
JAVA_OPTS="$JAVA_OPTS -Xlog:gc*=info,gc+heap=debug,safepoint=debug:file=$GCLOG:time,level,tags"
#JAVA_OPTS="$JAVA_OPTS -Xlog:safepoint=debug"
#JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDetails"
#JAVA_OPTS="$JAVA_OPTS -Xlog:gc*"
#JAVA_OPTS="$JAVA_OPTS -Xlog:gc+heap=debug"
#JAVA_OPTS="$JAVA_OPTS -verbose:gc -Xloggc:$GCLOG"

JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=$GCDIR/$SERVICE_NAME-java_pid.hprof"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF8"
```

---

### 변경 사항 정리

#### 1. 힙 메모리 설정 변경
- **ASIS**: `-Xms512m -Xmx2048m -XX:NewRatio=2`
- **TOBE**: `-Xmx2048m -Xmx2048m`
- **변경 이유**: 
  - 하나의 VM에 하나의 Java 인스턴스만 실행되므로 초기 힙 크기를 낮게 설정할 필요가 없음
  - 동적 힙 증가 시 발생하는 오버헤드 방지
  - G1GC는 New/Old 비율을 자동으로 조절하므로 NewRatio 설정 불필요

#### 2. 스레드 스택 크기 설정 제거
- **ASIS**: `-Xss32m`
- **TOBE**: 제거 (기본값 사용)
- **변경 이유**:
  - 스레드당 32MB의 큰 스택 메모리는 리소스 낭비
  - Virtual Thread 사용 시 더욱 위험한 설정
  - 기본값 사용이 더 안전하고 효율적

#### 3. GC 설정 변경
- **ASIS**: `-XX:+UseZGC`
- **TOBE**: `-XX:+UseG1GC -XX:MaxGCPauseMillis=200`
- **변경 이유**:
  - ZGC는 수십~수백GB의 대용량 힙에서 적합
  - 2GB 정도의 힙 크기에서는 G1GC가 더 효율적
  - MaxGCPauseMillis로 STW 시간 제한 설정

#### 4. GC 로깅 설정 통합
- **ASIS**: 여러 개의 구버전 GC 로깅 설정 혼용
- **TOBE**: `-Xlog:gc*=info,gc+heap=debug,safepoint=debug:file=$GCLOG:time,level,tags`
- **변경 이유**:
  - 구버전/신버전 설정 혼용 시 신버전 설정이 무효화될 수 있음
  - 통합된 로깅 설정으로 관리 용이성 향상
  - 필요한 로그 레벨과 정보만 선택적으로 수집

#### 5. 유지된 설정
- `-XX:+HeapDumpOnOutOfMemoryError`
- `-XX:HeapDumpPath=$GCDIR/$SERVICE_NAME-java_pid.hprof`
- `-Dfile.encoding=UTF8`

---

### 번외: logback.xml > logback-spring.xml

- Spring Boot 애플리케이션에서 `logback.xml > logback-spring.xml` 변경하는 이유

#### 1. Spring Boot의 고급 기능 지원

- `logback-spring.xml` 사용하면 Spring Boot 특별한 기능 활용 가능
- `<springProperty>`, `<springProfile>` Spring Boot 전용 태그 사용 가능
- `application.properties`나 `application.yml` 정의된 프로퍼티 로깅 설정 직접 참조 가능

#### 2. 프로파일 기반 설정

- `logback-spring.xml` 은 Spring 프로파일 기능을 활용 가능하여 환경별로 다른 로깅 설정 적용 가능
- 개발, 테스트, 운영 환경에 따라 다른 로그 레벨이나 출력 방식 설정

#### 3. Spring Boot의 자동 설정과의 통합

- `logback-spring.xml` 은 Spring Boot 자동 설정 메커니즘과 높은 상호 호환성 지원
- Spring Boot 기본 로깅 설정을 오버라이드 & Spring Boot 다른 기능들과 충돌 없이 동작 가능

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="60 seconds">

    <statusListener class="ch.qos.logback.core.status.OnConsoleStatusListener"/>

    <springProperty scope="context" name="logPath" source="logging.file.path" defaultValue="/home/ktc/log"/>
    <springProperty scope="context" name="logFileName" source="logging.file.name" defaultValue="${USER}-${HOSTNAME}-${INSTANCE_NAME}"/>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${logPath}/${logFileName}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${logPath}/${logFileName}-%d{yyyyMMddHH}-%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>

        <encoder>
            <pattern>
                %d{yyyy-MM-dd HH:mm:ss.SSS}.%thread> %-5level T[%X{correlationId}] U[%X{userId}] M[%X{mpaId}] - %msg%n
            </pattern>
        </encoder>
    </appender>

    <springProfile name="dev">
        <logger name="com.kona.ktc" level="DEBUG"/>
        <logger name="org.springframework" level="INFO"/>
        <logger name="org.springframework.data.redis" level="DEBUG"/>
        <logger name="org.springframework.web" level="DEBUG"/>
        <logger name="io.netty" level="INFO"/>
    </springProfile>

    <springProfile name="qa">
        <logger name="com.kona.ktc" level="DEBUG"/>
        <logger name="org.springframework" level="INFO"/>
        <logger name="org.springframework.data.redis" level="DEBUG"/>
        <logger name="org.springframework.web" level="DEBUG"/>
        <logger name="io.netty" level="INFO"/>
    </springProfile>

    <springProfile name="prod">
        <logger name="com.kona.ktc" level="INFO"/>
        <logger name="org.springframework" level="WARN"/>
        <logger name="org.springframework.data.redis" level="WARN"/>
        <logger name="org.springframework.web" level="WARN"/>
        <logger name="io.netty" level="WARN"/>
    </springProfile>

    <root level="INFO">
        <appender-ref ref="FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>
</configuration>
```

---

