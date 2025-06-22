---
layout: post
title : Spring REST Docs 적용(feat. Kotest)
date  : 2025-06-22
image : spring-rest-docs.png
tags  : springrestdocs spring-rest-docs
---

# Spring REST Docs 적용 가이드 (Kotest 환경)

## 1. 도입 배경

### API 문서 자동화의 필요성

- 프로젝트를 진행하면서 API 명세는 프론트엔드와 백엔드 개발자 간의 중요한 소통 수단 
- 하지만 기능이 계속 추가되고, 변경되면서 API 문서를 최신 상태로 유지하는 것은 번거로운 작업으로 전락하는 현실
- 이 문제를 해결하기 위해 API 문서 자동화 도구 도입 필요

### Swagger vs. Spring REST Docs

#### Swagger

- 코드에 직접 다양한 애노테이션을 추가하여 문서를 생성하는 방식으
- 적용이 간편하고, 직관적인 UI 제공
- 하지만, 프로덕션 코드에 문서화를 위한 애노테이션이 추가되어 코드의 가독성 저하\
- 실제 API 동작과 문서가 일치하지 않을 위험 존재

#### Spring REST Docs

- **테스트 코드** 기반으로 문서 조각(snippet)을 생성하고, 이를 조합하여 최종 문서 생성
- 테스트가 성공해야만 문서가 생성되므로, **항상 정확하고 신뢰도 높은 문서를 보장**

**`Kotlin + Spring Boot + Kotest` 기반의 프로젝트에서 `Spring REST Docs` 적용으로 정확하고 신뢰성 높은 API 문서 제공 목표!!**

---

## 2. Gradle 설정 (`build.gradle.kts`)

- 먼저 `build.gradle.kts` 파일에 Spring REST Docs와 Asciidoctor 관련 설정 추가 필요

### `asciidoctor` 플러그인 추가

- `asciidoctor` 플러그인: `Asciidoc` 문서를 `HTML` 변환 역할

```kotlin
plugins {
    // ...
    id("org.asciidoctor.jvm.convert") version "3.3.2"
}
```

### 라이브러리 의존성 추가
 
- `spring-restdocs-mockmvc`: 테스트 코드에서 `MockMvc` 사용해 API를 호출하고, 문서 조각 생성 라이브러리
- `spring-restdocs-asciidoctor`: 생성된 문서 조각을 `Asciidoctor` 가 사용할 수 있도록 제공 지원 라이브러리

```kotlin
dependencies {
    // ...
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    asciidoctorExt("org.springframework.restdocs:spring-restdocs-asciidoctor")
}
```

### Gradle Task 설정

- 문서 생성과 관련된 Gradle Task 설정 필요

```kotlin
// 생성된 스니펫이 저장될 디렉토리 변수 선언
val snippetsDir by extra { file("build/generated-snippets") }
// asciidoctor 확장 설정을 위한 configuration 생성
val asciidoctorExt by configurations.creating

// ...

// 1. Test Task 설정
tasks.withType<Test> {
    useJUnitPlatform()
    // 테스트 실행 결과물로 snippetsDir를 지정
    outputs.dir(snippetsDir)
    // (*핵심*) 테스트가 끝난 뒤, 항상 asciidoctor 태스크를 실행하도록 설정
    // 해당 부분을 `dependsOn` 으로 설정하면, 애플리케이션 기동 후에만 문서 확인 가능
    finalizedBy(tasks.named("asciidoctor"))
}

// 2. Asciidoctor Task 설정
tasks.withType<AsciidoctorTask> {
    // 입력 소스로 snippetsDir를 지정
    inputs.dir(snippetsDir)
    configurations(asciidoctorExt.name)
    // Asciidoctor 실행 전, 반드시 test 태스크가 먼저 실행되도록 설정
    dependsOn(tasks.withType<Test>())
}

// 3. BootJar Task 설정
val asciidoctorTask = tasks.named<AsciidoctorTask>("asciidoctor")
tasks.withType<BootJar> {
    // bootJar 실행 전, 반드시 asciidoctor 태스크가 먼저 실행되도록 설정
    dependsOn(asciidoctorTask)
    // asciidoctor가 생성한 문서를 jar 파일 내부의 /static/docs/ 경로에 포함
    from(asciidoctorTask.get().outputDir) {
        into("static/docs")
    }
}
```
- **`dependsOn`**: 각 태스크가 올바른 순서로 실행되도록 보장
- **`finalizedBy`**: `test` 단독 실행해도 API 문서 생성되도록 설정(개발 편의성 향상)

---

## 3. API 문서 기본 구조 작성 (`index.adoc`)

- `index.adoc`: API 문서의 구조 정의하는 설정 파일
  - 해당 파일 저장 경로: `{project_dir}/src/docs/asciidoc/` 

```
= Demo AMS API Docs
:doctype: book
:icons: font
:source-highlighter: highlightjs
:toc: left
:toclevels: 2
:sectlinks:

[[overview]]
== 개요

AMS(Academy Management System) API 문서

[[student-api]]
== 학생 관리 API

학생 등록/조회/수정/삭제 정보 관리 API

[[student-create]]
=== 학생 정보 생성

// 테스트 코드에서 생성한 스니펫을 이곳에 포함시킴
operation::student/create[snippets='http-request,request-fields,http-response,response-fields']
```

- `Asciidoc` 의 다양한 속성을 이용해 문서의 제목, 목차, 스타일 등을 설정 가능
- `operation::student/create[...]`: 테스트 실행 시 `student/create` 라는 이름으로 생성된 문서 조각들(HTTP 요청/응답, 필드 설명 등)을 현재 위치에 자동으로 포함시켜주는 매크로

---

## 4. Kotest 테스트 코드 작성 (`*Test.kt`)

### (핵심) Kotest와 Spring REST Docs 생명주기 연동

- `Kotest` 중첩 구조(`given-when-then`) 와 `Spring REST Docs` 의 생명주기는 충돌하여 여러 런타임 오류 발생 가능
- 이 문제를 해결하기 위해, `@AutoConfigureRestDocs` 의존하는 대신 `ManualRestDocumentation` 활용하여 수동으로 생명주기를 제어하도록 설정
- `aroundTest` 함수를 사용하여 **실제 테스트 케이스(`TestType.Test`)가 실행될 때만** `REST Docs 컨텍스트` 생성하고, 정리하도록 제어

```kotlin
@CustomSpringBootTest
class StudentManagementControllerTest(
    private val webApplicationContext: WebApplicationContext
) : CustomBehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    private lateinit var mockMvc: MockMvc
    private val restDocumentation = ManualRestDocumentation()

    init {
        aroundTest { (testCase, execute) ->
            // 1. 'then' 블록과 같은 실제 테스트 케이스만 필터링
            if (testCase.type == TestType.Test) {
                // 2. 테스트 전에 REST Docs 컨텍스트 준비
                restDocumentation.beforeTest(javaClass, testCase.name.testName)
                
                // 3. 준비된 컨텍스트로 MockMvc 객체 생성
                mockMvc = MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .apply<DefaultMockMvcBuilder>(
                        MockMvcRestDocumentation.documentationConfiguration(this.restDocumentation)
                    )
                    .build()
                
                // 4. 실제 테스트 코드 실행
                val result = execute(testCase)
                
                // 5. 테스트가 끝나면 컨텍스트 정리
                restDocumentation.afterTest()
                
                // 6. 테스트 결과 반환
                result
            } else {
                // 'given', 'when' 블록은 그냥 통과
                execute(testCase)
            }
        }

        given("...") {
            `when`("...") {
                // (!!주의!!) MockMvc를 사용하는 코드는 반드시 then 블록 안에 위치해야 함
                then("...") {
                    val result = mockMvc.post(...)
                    
                    result.andExpect { ... }
                        .andDo {
                            handle(
                                MockMvcRestDocumentation.document(
                                    "student/create", // 스니펫이 저장될 경로
                                    PayloadDocumentation.requestFields(...),
                                    PayloadDocumentation.responseFields(...)
                                )
                            )
                        }
                }
            }
        }
    }
}
```

### MockMvc 호출과 문서화

- **`andDo { handle(...) }`**
  - Spring MockMvc의 Kotlin DSL을 사용할 때, `document()` 핸들러를 이와 같이 `handle()` 함수를 통하여 문서 정의 작성 코드 구성 & 문서 생성 동작 가능
- **`PayloadDocumentation.requestFields`, `PayloadDocumentation.responseFields`**
  - 실제 API 문서로 만들어질 API 내역 정보가 되는 부분
  - API 요청/응답 본문에 포함된 각 필드의 타입, 설명 등을 상세하게 작성

---

## 5. Troubleshooting

#### **`Context already exists`, `NullPointerException` 이슈**

- **문제 상황** 
  - Kotest 생명주기 콜백(`beforeTest`, `afterTest`)과 REST Docs의 컨텍스트 관리 타이밍 차이 발생하였지만, 
- **해결 방안**
  - `aroundTest`를 사용하여 테스트 실행 전 과정을 완벽하게 통제함으로써 해결

#### **`UninitializedPropertyAccessException` 이슈**

- **문제 상황**
  - Kotest 테스트 구조 생성 단계(`when` 블록)에서 아직 초기화되지 않은 `mockMvc`를 사용하려 했기 때문에 에러 발생하였지만,
- **해결 방안**
  - `mockMvc` 사용하는 모든 코드를 실제 테스트 실행 단계인 `then` 블록으로 이동시켜 해결

#### **`SnippetException` 이슈**

- **문제 상황**
  - API의 실제 응답과 `responseFields`에 기술한 내용이 일치하지 않을 때 발생
- **해결 방안**
  - 응답 Payload 맞게 누락된 필드를 추가하거나 오타를 수정하여 해결

#### **IDE 실행 시 `FileNotFoundException` 이슈**

- **문제 상황**
  - 인텔리제이의 'Run'은 Gradle의 `bootJar` 태스크를 실행하지 않기 때문에 에러 발생
- **해결 방안**
  - 개발 중에는 `./gradlew test` 또는 `asciidoctor` 태스크를 실행하여 생성된 `build/docs/asciidoc/index.html` 파일을 직접 브라우저로 열어 확인하는 방식으로 워크플로우를 정립
  - 개발/운영 서버 애플리케이션 배포 후 `http://{domain}:{port}/{context-path}/docs/index.html` 에서 문서 확인

#### **Gradle 순환 참조 이슈**

- **문제 상황**
  - `processResources` task가 `asciidoctor` task에 의존하도록 설정하여 발생
- **해결 방안**
  - `bootJar` task가 `asciidoctor` task에 의존하도록 하는 표준적인 방식으로 적용하여 해결

---

#### Reference

- [Spring REST Docs Reference Documentation](https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/)
- [Kotest Framework - Spring Extension](https://kotest.io/docs/extensions/spring.html)

---
