---
layout: post
title : ApprovalTests.Java
date  : 2025-12-13
image : approvaltests_logo.png
tags  : java approvaltestsjava approvaltests
---

## ApprovalTests.Java란?

ApprovalTests.Java는 복잡한 객체나 출력 결과를 검증하는 데 특화된 Java 테스트 라이브러리입니다. 전통적인 단위 테스트가 어려운 상황에서, 특히 레거시 코드나 복잡한 데이터 구조를 테스트할 때 매우 유용한 도구입니다.

### Approval Testing 개념

Approval Testing은 **"예상되는 결과를 미리 승인(approve)하고, 이후 실행 결과와 비교하여 검증하는"** 테스트 방식입니다. 

기존의 단위 테스트 방식과 비교하면:
- **기존 방식**: Assert 문을 사용하여 특정 값이나 조건을 명시적으로 검증
- **Approval 방식**: 전체 출력 결과를 파일로 저장하고, 이를 승인된 결과와 비교

이 방식의 핵심은 `.received.txt`와 `.approved.txt` 파일을 통해 검증하는 것입니다.

---

### 기존 테스트 방식과의 차이점

#### 전통적인 Assert 기반 테스트

```java
@Test
public void testSorting() {
    String[] names = {"Llewellyn", "James", "Dan", "Jason", "Katrina"};
    Arrays.sort(names);
    
    assertEquals("Dan", names[0]);
    assertEquals("James", names[1]);
    assertEquals("Jason", names[2]);
    // ... 모든 요소를 개별적으로 검증
}
```

**문제점:**
- 복잡한 객체나 긴 문자열을 검증하기 어려움
- 출력 결과의 일부만 검증하여 전체 맥락을 놓칠 수 있음
- 레거시 코드의 복잡한 출력을 테스트하기 어려움

#### Approval Test 방식

```java
@Test
public void testSorting() {
    String[] names = {"Llewellyn", "James", "Dan", "Jason", "Katrina"};
    Arrays.sort(names);
    
    Approvals.verifyAll("Sorted Names", names);
}
```

**장점:**
- 전체 출력 결과를 한 번에 검증
- 복잡한 객체나 긴 문자열도 쉽게 검증 가능
- 레거시 코드의 출력을 그대로 캡처하여 테스트 가능
- Diff 도구를 통한 시각적 비교 가능

---

### 언제 사용하면 좋은가?

ApprovalTests.Java는 다음 상황에서 특히 유용합니다:

1. **레거시 코드 테스트**: 기존 코드의 동작을 검증하고 싶을 때
2. **복잡한 데이터 구조**: HashMap, 컬렉션, 중첩 객체 등 복잡한 구조 검증
3. **긴 문자열 검증**: 로그, 리포트, 템플릿 렌더링 결과 등
4. **UI 컴포넌트**: JPanel, Swing 컴포넌트 등의 시각적 검증
5. **API 응답 검증**: JSON, XML, HTML 등의 구조화된 응답
6. **리팩토링 안전망**: 리팩토링 전후의 출력이 동일한지 검증

---

## 설치 및 설정

### Maven 의존성 추가

`pom.xml`에 다음 의존성을 추가합니다:

```xml
<dependency>
    <groupId>com.approvaltests</groupId>
    <artifactId>approvaltests</artifactId>
    <version>25.7.0</version>
    <scope>test</scope>
</dependency>
```

### Gradle 의존성 추가

`build.gradle`에 다음 의존성을 추가합니다:

```groovy
dependencies {
    testImplementation 'com.approvaltests:approvaltests:25.7.0'
}
```

### 호환성

- **테스트 프레임워크**: JUnit 3, 4, 5 및 TestNG와 호환
- **JDK 요구사항**: JDK 1.8 이상
- **최신 버전**: 25.7.0 (2024년 기준)

---

## 기본 사용법

### 첫 번째 Approval Test 작성

가장 간단한 예제부터 시작해보겠습니다:

```java
import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;
import java.util.Arrays;

public class SampleArrayTest {
    @Test
    public void testList() {
        String[] names = {"Llewellyn", "James", "Dan", "Jason", "Katrina"};
        Arrays.sort(names);
        Approvals.verifyAll("Sorted Names", names);
    }
}
```

### `.received.txt`와 `.approved.txt` 파일 이해

테스트를 실행하면 다음과 같은 과정이 진행됩니다:

1. **첫 실행**: `SampleArrayTest.testList.received.txt` 파일이 생성됩니다.
   ```
   Sorted Names
   Dan
   James
   Jason
   Katrina
   Llewellyn
   ```

2. **파일 승인**: 생성된 `.received.txt` 파일을 검토한 후, 파일명을 `.approved.txt`로 변경합니다.

3. **이후 실행**: 테스트가 실행될 때마다 `.received.txt`와 `.approved.txt`를 비교합니다.
   - 두 파일이 동일하면 테스트 통과
   - 다르면 테스트 실패 (Diff 도구로 차이점 확인 가능)

### 기본 검증 메서드

#### `Approvals.verify()` - 단일 객체 검증

```java
@Test
public void testSingleObject() {
    String result = "Hello, ApprovalTests!";
    Approvals.verify(result);
}
```

#### `Approvals.verifyAll()` - 컬렉션 검증

```java
@Test
public void testCollection() {
    List<String> items = Arrays.asList("Apple", "Banana", "Cherry");
    Approvals.verifyAll("Fruits", items);
}
```

첫 번째 파라미터는 라벨(label)로, 출력 결과의 제목으로 사용됩니다.

---

## 주요 기능

### 다양한 객체 타입 검증

ApprovalTests.Java는 다양한 타입의 객체를 자동으로 포맷팅하여 검증할 수 있습니다.

#### HashMap 검증

```java
@Test
public void testHashMap() {
    Map<String, Integer> scores = new HashMap<>();
    scores.put("Alice", 95);
    scores.put("Bob", 87);
    scores.put("Charlie", 92);
    
    Approvals.verify(scores);
}
```

출력 결과:
```
{Alice=95, Bob=87, Charlie=92}
```

#### 컬렉션 검증

```java
@Test
public void testList() {
    List<Person> people = Arrays.asList(
        new Person("Alice", 30),
        new Person("Bob", 25),
        new Person("Charlie", 35)
    );
    
    Approvals.verifyAll("People", people);
}
```

#### 긴 문자열 검증

```java
@Test
public void testLongString() {
    String report = generateMonthlyReport();
    Approvals.verify(report);
}
```

#### 로그 파일 검증

```java
@Test
public void testLogOutput() {
    Logger logger = Logger.getLogger(MyClass.class.getName());
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    
    // 로그 출력을 캡처
    logger.info("Application started");
    logger.warning("Low memory detected");
    
    Approvals.verify(sw.toString());
}
```

#### XML, HTML, JSON 검증

```java
@Test
public void testXML() {
    String xml = "<root><item>value</item></root>";
    Approvals.verify(xml);
}

@Test
public void testJSON() {
    String json = "{\"name\":\"Alice\",\"age\":30}";
    Approvals.verify(json);
}
```

#### JPanel (UI 컴포넌트) 검증

```java
@Test
public void testJPanel() {
    JPanel panel = new JPanel();
    panel.add(new JLabel("Hello"));
    panel.add(new JButton("Click"));
    
    Approvals.verify(panel);
}
```

### 커스텀 Formatter 작성

기본 포맷터가 만족스럽지 않을 때, 커스텀 Formatter를 작성할 수 있습니다:

```java
import org.approvaltests.core.ApprovalWriter;
import org.approvaltests.namer.ApprovalNamer;
import org.approvaltests.reporters.DiffReporter;

public class CustomFormatter implements ApprovalWriter {
    @Override
    public String writeReceivedFile(String received) throws Exception {
        // 커스텀 포맷팅 로직
        String formatted = formatCustom(received);
        return formatted;
    }
    
    @Override
    public String getReceivedFilename(ApprovalNamer namer) {
        return namer.getReceivedFile(".custom");
    }
    
    @Override
    public String getApprovalFilename(ApprovalNamer namer) {
        return namer.getApprovedFile(".custom");
    }
    
    private String formatCustom(String input) {
        // 원하는 포맷으로 변환
        return input.toUpperCase();
    }
}
```

### Diff 도구 통합

ApprovalTests.Java는 다양한 Diff 도구와 통합할 수 있습니다:

- **기본**: Java Swing 기반 Diff 뷰어
- **외부 도구**: Beyond Compare, WinMerge, KDiff3 등

설정 방법:

```java
import org.approvaltests.reporters.UseReporter;
import org.approvaltests.reporters.DiffReporter;

@UseReporter(DiffReporter.class)
public class MyTest {
    // ...
}
```

---

## 고급 기능

### Named Parameter 사용

Named Parameter를 사용하면 테스트의 가독성을 높이고, 여러 시나리오를 쉽게 테스트할 수 있습니다:

```java
import org.approvaltests.namer.NamedEnvironment;
import org.approvaltests.Approvals;

@Test
public void testWithNamedParameter() {
    String[] scenarios = {"scenario1", "scenario2", "scenario3"};
    
    for (String scenario : scenarios) {
        try (NamedEnvironment env = NamedEnvironment.create(scenario)) {
            String result = processScenario(scenario);
            Approvals.verify(result);
        }
    }
}
```

이렇게 하면 각 시나리오별로 별도의 승인 파일이 생성됩니다:
- `MyTest.testWithNamedParameter.scenario1.approved.txt`
- `MyTest.testWithNamedParameter.scenario2.approved.txt`
- `MyTest.testWithNamedParameter.scenario3.approved.txt`

### Reporter 설정

Reporter는 테스트 실패 시 차이점을 어떻게 표시할지 결정합니다:

```java
import org.approvaltests.reporters.UseReporter;
import org.approvaltests.reporters.QuietReporter;
import org.approvaltests.reporters.DiffReporter;

// 클래스 레벨에서 Reporter 설정
@UseReporter(DiffReporter.class)
public class MyTestClass {
    
    // 특정 테스트에서만 다른 Reporter 사용
    @UseReporter(QuietReporter.class)
    @Test
    public void quietTest() {
        // ...
    }
}
```

주요 Reporter 종류:
- `DiffReporter`: 기본 Diff 도구 사용
- `QuietReporter`: 출력 없이 조용히 실패
- `ClipboardReporter`: 차이점을 클립보드에 복사
- `FileLauncherReporter`: 파일 탐색기에서 파일 열기

### File Approver 사용

기본적으로 ApprovalTests는 텍스트 파일을 사용하지만, 다른 형식의 파일도 승인할 수 있습니다:

```java
import org.approvaltests.core.ApprovalFailureReporter;
import org.approvaltests.reporters.FileLauncherReporter;

@Test
public void testBinaryFile() {
    byte[] imageData = generateImage();
    Approvals.verifyBinaryFile(imageData, ".png");
}
```

### 레거시 코드 테스트 전략

레거시 코드를 테스트할 때는 다음과 같은 전략을 사용할 수 있습니다:

#### 1. 출력 캡처 방식

```java
@Test
public void testLegacyMethod() {
    // 레거시 메서드의 출력을 캡처
    String output = captureLegacyOutput();
    Approvals.verify(output);
}

private String captureLegacyOutput() {
    // System.out 리다이렉트
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    PrintStream old = System.out;
    System.setOut(ps);
    
    try {
        legacyMethod(); // 레거시 메서드 실행
    } finally {
        System.setOut(old);
    }
    
    return baos.toString();
}
```

#### 2. 상태 스냅샷 방식

```java
@Test
public void testLegacyState() {
    LegacyObject obj = new LegacyObject();
    obj.process();
    
    // 객체의 전체 상태를 문자열로 변환
    String state = captureObjectState(obj);
    Approvals.verify(state);
}
```

### 복잡한 객체 검증 패턴

#### 중첩 객체 검증

```java
@Test
public void testNestedObject() {
    ComplexObject obj = new ComplexObject();
    obj.setNested(new NestedObject());
    
    // 자동으로 toString() 또는 커스텀 포맷터 사용
    Approvals.verify(obj);
}
```

#### 여러 객체 동시 검증

```java
@Test
public void testMultipleObjects() {
    Map<String, Object> context = new HashMap<>();
    context.put("input", inputData);
    context.put("output", outputData);
    context.put("metadata", metadata);
    
    Approvals.verify(context);
}
```

---

## 실제 적용 사례

### 레거시 코드 리팩토링 시나리오

레거시 코드를 리팩토링할 때, Approval Test를 사용하면 안전하게 리팩토링할 수 있습니다:

```java
public class LegacyCodeRefactoringTest {
    
    @Test
    public void testBeforeRefactoring() {
        // 리팩토링 전 코드 실행
        String result = LegacyClass.oldMethod(input);
        Approvals.verify(result);
    }
    
    @Test
    public void testAfterRefactoring() {
        // 리팩토링 후 코드 실행
        String result = RefactoredClass.newMethod(input);
        Approvals.verify(result);
    }
    
    // 두 테스트의 approved 파일을 비교하여 동일한지 확인
}
```

### 복잡한 데이터 구조 검증 예제

```java
@Test
public void testComplexDataStructure() {
    Map<String, List<Map<String, Object>>> complexData = new HashMap<>();
    
    List<Map<String, Object>> users = new ArrayList<>();
    Map<String, Object> user1 = new HashMap<>();
    user1.put("name", "Alice");
    user1.put("age", 30);
    user1.put("roles", Arrays.asList("admin", "user"));
    users.add(user1);
    
    complexData.put("users", users);
    
    Approvals.verify(complexData);
}
```

### UI 컴포넌트 테스트 예제

```java
@Test
public void testJPanelLayout() {
    JPanel panel = createUserPanel();
    
    // 패널의 시각적 표현을 검증
    Approvals.verify(panel);
}
```

### API 응답 검증 예제

```java
@Test
public void testAPIResponse() {
    String apiResponse = callExternalAPI();
    
    // JSON 응답을 검증
    Approvals.verify(apiResponse);
}

@Test
public void testAPIResponseFormatted() {
    String apiResponse = callExternalAPI();
    
    // JSON을 포맷팅하여 검증
    JSONObject json = new JSONObject(apiResponse);
    String formatted = json.toString(2); // 들여쓰기 2칸
    Approvals.verify(formatted);
}
```

---

## Best Practices

### Approval Test 작성 가이드라인

1. **명확한 테스트 이름 사용**
   ```java
   // 좋은 예
   @Test
   public void testUserRegistrationWithValidData() { }
   
   // 나쁜 예
   @Test
   public void test1() { }
   ```

2. **의미 있는 라벨 사용**
   ```java
   Approvals.verifyAll("User List After Filtering", users);
   ```

3. **결정론적 출력 보장**
   - 랜덤 값, 타임스탬프 등은 고정된 값으로 대체
   ```java
   @Test
   public void testWithFixedTimestamp() {
       // 타임스탬프를 고정 값으로 대체
       String result = generateReport().replaceAll(
           "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}",
           "2025-01-01 00:00:00"
       );
       Approvals.verify(result);
   }
   ```

### 파일 관리 전략

1. **`.approved.txt` 파일을 버전 관리에 포함**
   - `.approved.txt` 파일은 Git에 커밋
   - `.received.txt` 파일은 `.gitignore`에 추가

2. **파일 위치 설정**
   ```java
   // PackageSettings 클래스를 상위 패키지에 생성
   public class PackageSettings {
       public static String ApprovalBaseDirectory = "../resources";
   }
   ```

3. **파일명 규칙 준수**
   - 클래스명.메서드명.approved.txt 형식 유지
   - Named Parameter 사용 시 시나리오명 포함

### 팀 협업 시 주의사항

1. **승인 프로세스 정의**
   - 누가 `.approved.txt` 파일을 승인할지 명확히 정의
   - 코드 리뷰 시 승인 파일도 함께 리뷰

2. **변경 사항 문서화**
   - 승인 파일이 변경된 이유를 주석이나 커밋 메시지에 명시

3. **충돌 해결**
   - 승인 파일도 코드처럼 병합 충돌이 발생할 수 있음
   - Diff 도구를 사용하여 신중하게 병합

### CI/CD 통합 방법

1. **CI에서 `.received.txt` 파일 생성 방지**
   ```bash
   # CI 환경에서는 승인 파일이 없으면 테스트 실패
   # 로컬에서는 수동으로 승인 필요
   ```

2. **자동 승인 스크립트 (주의해서 사용)**
   ```bash
   # 특정 조건에서만 자동 승인
   if [ "$AUTO_APPROVE" = "true" ]; then
       find . -name "*.received.txt" -exec sh -c 'mv "$1" "${1%.received.txt}.approved.txt"' _ {} \;
   fi
   ```

3. **테스트 실패 시 아티팩트 저장**
   - CI에서 테스트 실패 시 `.received.txt` 파일을 아티팩트로 저장
   - 개발자가 다운로드하여 검토 후 승인

---

## 결론 및 참고 자료

### 장단점 정리

#### 장점

1. **복잡한 출력 검증 용이**: 긴 문자열, 복잡한 객체 등을 쉽게 검증
2. **레거시 코드 테스트**: 기존 코드의 동작을 빠르게 검증 가능
3. **시각적 비교**: Diff 도구를 통한 직관적인 비교
4. **유연성**: 다양한 타입의 객체를 자동으로 포맷팅
5. **리팩토링 안전망**: 리팩토링 전후의 출력을 비교하여 안전성 보장

#### 단점

1. **수동 승인 필요**: 첫 실행 후 수동으로 파일 승인 필요
2. **파일 관리**: 많은 승인 파일 관리 필요
3. **결정론적 출력 필요**: 랜덤 값, 타임스탬프 등 처리 필요
4. **학습 곡선**: 팀원들의 학습 필요

### 언제 사용하면 좋은지

**적합한 경우:**
- 레거시 코드 테스트
- 복잡한 데이터 구조 검증
- 리포트, 로그, 템플릿 렌더링 결과 검증
- UI 컴포넌트 시각적 검증
- API 응답 검증
- 리팩토링 안전망 구축

**부적합한 경우:**
- 단순한 값 검증 (기존 Assert로 충분)
- 성능 테스트
- 결정론적 출력을 보장하기 어려운 경우

### 공식 문서 및 추가 학습 자료

- **GitHub 저장소**: [https://github.com/approvals/ApprovalTests.Java](https://github.com/approvals/ApprovalTests.Java)
- **공식 웹사이트**: [https://approvaltests.com/](https://approvaltests.com/)
- **ApprovalTests Koans**: [https://github.com/approvals/ApprovalTests.Java/tree/master/approvaltests-tests/src/test/java/approvaltests/koans](https://github.com/approvals/ApprovalTests.Java/tree/master/approvaltests-tests/src/test/java/approvaltests/koans)
- **권남님의 ApprovalTests 소개**: [https://kwonnam.pe.kr/wiki/java/approvaltests](https://kwonnam.pe.kr/wiki/java/approvaltests)

---

#### Conclusion

ApprovalTests.Java는 복잡한 출력을 검증하거나 레거시 코드를 테스트할 때 매우 유용한 도구입니다. 전통적인 Assert 기반 테스트로는 어려운 상황에서, 전체 출력을 한 번에 검증할 수 있어 테스트 작성이 훨씬 간편해집니다.

특히 레거시 코드를 리팩토링하거나, 복잡한 데이터 구조를 다루는 프로젝트에서 ApprovalTests.Java를 활용하면 테스트 커버리지를 빠르게 높일 수 있습니다. 다만, 수동 승인 프로세스와 파일 관리에 대한 팀의 합의가 필요하며, 결정론적 출력을 보장하는 것이 중요합니다.

적절한 상황에서 ApprovalTests.Java를 활용하면, 더 견고하고 유지보수하기 쉬운 테스트 코드를 작성할 수 있을 것입니다.

---
