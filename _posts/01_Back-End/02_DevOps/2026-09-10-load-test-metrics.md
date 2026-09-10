---
layout: post
title : 부하테스트 수치 읽는 법 — 처리량·지연시간·에러·여유
date  : 2026-09-10
image : load-test-metrics.png
tags  : [부하테스트, 성능, k6, DevOps, 모니터링, Apdex]
---

# 부하테스트 수치 읽는 법

부하테스트를 한 번 돌리면 스무 개 남짓한 숫자가 쏟아집니다. `http_req_duration`의 avg·med·p95·max, 에러율, VU 수, CPU 사용률… 각각은 어렵지 않은데, **어떤 순서로 무엇을 근거로 읽어야 하는지**가 정리되지 않으면 엉뚱한 결론에 도달하기 쉽습니다.

이 글은 부하테스트 결과 보고서에 나오는 숫자들이 각각 어디서 어떻게 측정되고, 무엇을 판단하게 해주는지를 공식 문서 기준으로 정리한 것입니다. 도구는 [k6](https://grafana.com/docs/k6/latest/)를 예로 들지만, 개념 자체는 도구와 무관합니다.

---

## 1. 무엇을 알아내려고 하는가

부하테스트의 질문은 "우리 서버 빠른가요?"가 아닙니다. **"목표 트래픽에서, 이 자원으로, 약속한 응답 수준을 지킬 수 있는가?"** 입니다. 그래서 답도 "몇 ms"가 아니라 **"인스턴스 1대당 몇 TPS 까지, 그때 CPU 몇 %"** 같은 대응 관계로 나옵니다.

무엇을 볼지는 이미 표준 목록이 있습니다. Google SRE Book 6장의 **Four Golden Signals** 입니다.

> "If you can only measure four metrics of your user-facing system, focus on these four."

| 신호 | 부하테스트에서의 질문 | 대표 지표 |
|---|---|---|
| **Traffic** (처리량) | 목표만큼 실제로 흘려보냈나 | 초당 요청 수, 버려진 반복 수 |
| **Latency** (지연시간) | 얼마나 기다렸나 | 응답시간의 p95·p99 |
| **Errors** (정확성) | 제대로 처리됐나 | 상태코드 실패율, 업무 실패율 |
| **Saturation** (여유) | 얼마나 남았나 | CPU·메모리 사용률, 커넥션 풀, 큐 길이 |

k6 기준 지표명으로는 각각 `http_reqs`·`dropped_iterations` / `http_req_duration` / `http_req_failed`·`checks` 이고, Saturation 은 부하 생성기 밖에서 봐야 합니다.

**하나만 보면 반드시 틀립니다.** 응답이 빨라도 절반이 에러면 의미가 없고, 에러가 없어도 CPU 가 99%면 다음 피크에 무너집니다. SRE Book 이 포화에 목표치를 두라고 하는 이유가 이것입니다.

> "many systems degrade in performance before they achieve 100% utilization, so having a utilization target is essential."

지연시간에는 단서가 하나 붙습니다.

> "It's important to distinguish between the latency of successful requests and the latency of failed requests."

빠르게 실패한 요청이 p95 를 끌어내려 **에러가 성능을 좋아 보이게 만드는** 일이 실제로 일어납니다.

다만 처방은 "에러를 빼라"가 아닙니다. 같은 문서가 반대 방향의 지침도 함께 줍니다.

> "a slow error is even worse than a fast error! Therefore, it's important to track error latency, as opposed to just filtering out errors."

성공과 실패를 **나눠서 각각** 보는 것이지, 실패를 버리는 것이 아닙니다.

---

## 2. 부하를 만드는 두 방식은 서로 다른 실험이다

같은 도구, 같은 스크립트라도 **무엇이 반복 횟수를 결정하느냐**에 따라 다른 질문에 답하는 실험이 됩니다.

![닫힌 모델과 열린 모델 비교](/images/load-test-workload-model.svg)

k6 문서는 두 모델을 이렇게 정의합니다.

> "In a closed model, the execution time of each iteration dictates the number of iterations executed in your test." — "the next iteration doesn't start until the previous one finishes."

> "the open model decouples VU iterations from the iteration duration." — "the response times of the target system no longer influence the load on the target system."

|  | 닫힌 모델 (closed) | 열린 모델 (open) |
|---|---|---|
| 지정하는 값 | VU 수 | 초당 반복 수(도착률) |
| 재현하는 상황 | 동시 사용자 N명 | 초당 N건이 도착 |
| k6 executor | `constant-vus`, `ramping-vus` | `constant-arrival-rate`, `ramping-arrival-rate` |

### 닫힌 모델의 함정 — coordinated omission

닫힌 모델은 서버가 느려지면 **부하도 같이 줄어듭니다.** 응답을 기다리는 동안 다음 요청을 보내지 않기 때문입니다.

> "the target system is stressed and starts to respond more slowly, a closed model load test will wait, resulting in increased iteration durations and a tapering off of the arrival rate of new VU iterations."

이 현상에는 이름이 있습니다 — **coordinated omission**. 다만 k6 문서가 스스로 규정하는 게 아니라 외부 문헌의 명명을 소개하는 형태입니다: "In some testing literature, this problem is known as coordinated omission."

부하 생성기가 서버 사정에 "협조"해서 가장 나쁜 순간의 요청을 아예 만들지 않고, 그 결과 최악의 표본이 통계에서 빠져 시스템이 실제보다 건강해 보인다 — 이 설명은 성능공학 문헌의 통상적 해석이고, k6 문서가 직접 서술하는 것은 **도착률이 떨어진다**는 데까지입니다.

거꾸로도 성립합니다. 서버가 빨라지면 같은 VU 수로 더 많은 요청을 보내므로, **몇 TPS 를 쏜 실험이었는지가 사후에야 정해집니다.** "N TPS 를 견디는가"라는 질문에는 구조적으로 답할 수 없습니다.

닫힌 모델이 틀린 것은 아닙니다. **질문이 처리량이면 열린 모델, 질문이 동시성이면 닫힌 모델**입니다.

### 열린 모델의 대가 — VU 풀

응답과 무관하게 요청을 밀어넣으려면, 응답을 기다리는 동안 요청을 들고 있을 일꾼이 충분해야 합니다. k6 문서가 근사식을 제시합니다(문서는 이 식에 이름을 붙이지 않습니다 — 리틀의 법칙과 같은 형태라는 건 제 해석입니다).

> "preAllocatedVUs = [median_iteration_duration * rate] + constant_for_variance"

초당 500건 × 반복 0.1초 ≈ 50명이면 되지만, 응답이 2초로 늘면 같은 도착률에 1,000명이 필요합니다. **서버가 느려질수록 필요한 VU 가 늘어나는** 구조라, 정상 상태 기준으로 잡으면 정작 한계 구간에서 모자랍니다.

문서 스스로 이 공식의 한계를 인정합니다.

> "In the real world, if you know *exactly* how long an iteration takes, you likely don't need to run a test."

그렇다면 상한(`maxVUs`)을 크게 잡아 부족할 때 자동으로 늘리면 될 것 같은데, **문서는 그 반대를 권합니다.** 절 제목이 그대로 결론입니다 — "You probably don't need maxVUs".

> "Though it seems convenient, you should avoid using `maxVUs` in most cases. Allocating VUs has CPU and memory costs, and allocating VUs as the test runs can overload the load generator and skew results."

> "In almost all cases, the best thing to do is to pre-allocate the number of VUs you need beforehand."

인용문의 경고 대상이 **실행 중 동적 증원**이라는 점이 중요합니다. 사전 할당을 넉넉히 잡는 것에 대한 경고가 아닙니다. `maxVUs` 가 말이 되는 경우로 문서는 첫 테스트에서 필요량을 가늠할 때, 사전 할당에 약간의 여유를 둘 때, 부하 생성기를 세심하게 키워야 하는 대규모 분산 테스트를 한정 열거합니다.

즉 실무 결론은 **"상한으로 방어"가 아니라 "필요량을 미리 재서 사전 할당"** 입니다.

### dropped_iterations — 가장 먼저 봐야 할 숫자

VU 가 모자라 **시작조차 못 한 반복**은 별도 카운터로 집계됩니다.

> "k6 tracks the number of unsent iterations in a counter metric, `dropped_iterations`."

이 값이 크면 목표 부하를 만들지 못한 것이므로, **그 회차의 지연시간·에러율은 "그 부하에서의 값"이 아닙니다.** 결과를 읽기 전에 먼저 확인해야 하는 이유입니다.

문서가 원인을 두 갈래로 선언합니다 — "The executor configuration is insufficient." / "The SUT can't handle the configured VU arrival rate." 그리고 executor 계열마다 발생 조건이 다릅니다.

| executor 계열 | 문서 서술 | 뜻하는 것 |
|---|---|---|
| 반복 횟수 기반<br>(`shared-iterations`·`per-vu-iterations`) | "iterations drop if the scenario reaches its `maxDuration` before all iterations finish" | 시간 부족 → duration 을 늘림 |
| 도착률 기반<br>(`constant`·`ramping-arrival-rate`) | "iterations drop if there are no free VUs" | VU 소진 → 아래 판별 필요 |

**도착률 기반에서 두 원인을 가르는 기준은 "언제 발생했는가" 이고, 이것은 문서가 직접 제시합니다.**

> "If it happens at the beginning of the test, you likely just need to allocate more VUs. If this happens later in the test, the dropped iterations might happen because SUT performance is degrading and iterations are taking longer to finish."

즉 **초반 드롭이면 설정 부족, 후반 드롭이면 SUT 열화**입니다.

여기서 흔한 오해를 하나 정리해 둡니다. **"VU 에 여유가 있는데 드롭이 났으니 서버 탓"은 성립하지 않습니다.** 도착률 executor 의 드롭은 정의상 free VU 가 없을 때만 발생하고, SUT 열화로 인한 드롭이야말로 VU 가 소진된 상태에서 나타납니다 — "At a certain point of high latency or longer iteration durations, k6 will no longer have free VUs to start iterations with at the configured rate." `vus` 와 `vus_max` 의 관계는 원인 판별자가 아니라 **부하 생성기가 포화했는지 확인하는 용도**입니다.

양(量)의 해석도 문서가 줍니다.

> "A few dropped iterations might indicate a quick network error. Many dropped iterations might indicate that your SUT has completely stopped responding."

그리고 이 값에도 오차 예산을 두라고 권합니다 — "consider what an acceptable rate of dropped iterations is (the *error budget*)", "use the `dropped_iterations` metric in a Threshold".

---

## 3. 왜 계단으로 올리는가

k6 문서는 부하 모양을 세 국면으로 권합니다.

> "ramp-up, plateau, ramp-down"

복잡한 변주는 경계합니다 — 그런 패턴은 "will waste resources and make it hard to isolate issues." 부하 모양이 복잡하면 어떤 구간의 어떤 조건이 문제를 만들었는지 분리할 수 없습니다.

![계단식 부하 프로파일과 무릎](/images/load-test-step-profile.svg)

**유지 구간(plateau)이 필요한 이유는 두 가지입니다.**

**첫째, 정상 상태에 도달시키기 위해서입니다.** 램프업 중의 수치는 과도 상태입니다. 커넥션 풀·캐시·JIT·오토스케일링이 안정되기 전 값으로 판단하면 안 됩니다.

**둘째, 그 부하에서의 자원 사용량을 기록하기 위해서입니다.** "부하 X 일 때 사용률 Y%" 라는 대응 관계가 있어야 필요한 자원을 역산할 수 있습니다. 한 점만 찍으면 선을 그을 수 없습니다. 계단으로 나눠 올리는 이유가 이것입니다 — 계단마다 점이 하나씩 남습니다.

> **근거 구분:** k6 문서가 뒷받침하는 것은 테스트 유형 정의와 "ramp-up / plateau / ramp-down" 구조 권고, breakpoint 의 성격·경고까지입니다. 반면 **위 두 이유(정상 상태 도달·자원 사용량 기록), "계단마다 기록해 인스턴스당 처리량을 산출한다"는 절차, 그리고 아래 9절의 "무릎이 어디에 있느냐" 처방**은 1차 문서에 서술이 없는 용량 산정의 통상적 관행이고 제 정리입니다. 그리고 **plateau 를 몇 분 유지하라는 정량 권고는 문서에 없습니다** — 비교표의 Duration 열은 plateau 가 아니라 **테스트 전체 길이**이며 유형마다 다릅니다(Smoke "Short (seconds or minutes)", Load·Stress "Mid (5-60 minutes)", Soak "Long (hours)", Spike "Short (a few minutes)", Breakpoint "As long as necessary").

### 무릎(knee) 찾기

부하를 올려도 지연시간이 한동안 거의 평평하다가, 어느 지점부터 급격히 꺾입니다. **그 지점이 그 구성의 실질적 한계**입니다.

무릎을 찾는 것이 목적인 유형이 **breakpoint test** 이고, 성격이 다릅니다.

> "Breakpoint testing aims to find system limits." — "no plateau, ramp-down, or other steps" — "Load slowly ramps up to a considerably high level."

그리고 완만함을 강하게 권합니다.

> "Increase the load gradually. A sudden increase may make it difficult to pinpoint why and when the system starts to fail."

합격선은 팀이 정의해야 합니다 — "System failure could mean different things to different teams" 응답 지연, 타임아웃, HTTP 에러, 완전 붕괴 중 무엇을 "한계"로 볼지 먼저 합의하지 않으면 결과 해석이 갈립니다. 그리고 결과 해석의 전제가 하나 있습니다 — "A breakpoint test must cause system failure."

**executor 도 문서가 지정합니다.**

> "breakpoint load increases even as the system starts to degrade. That makes it recommendable to use `ramping-arrival-rate` for a breakpoint test."

그리고 **결론을 통째로 무효화할 수 있는 경고**가 하나 있습니다.

> "Avoid breakpoint tests in elastic cloud environments. The elastic environment may grow as the test moves further, finding only the limit of your cloud account bill. If this test runs on a cloud environment, turning off elasticity on all the affected components is strongly recommended."

오토스케일링이 켜진 채로 한계를 찾으면 **찾은 것은 시스템의 한계가 아니라 결제 한도**입니다. 순서에 대한 경고도 있습니다 — "Run breakpoints only when the system is known to perform under all other test types."

**목표 부하까지 꺾이지 않았다면 "한계는 그보다 위"라는 것만 알 수 있습니다.** 한계값 자체를 알려면 꺾일 때까지 올려야 합니다. 두 실험은 목적이 다릅니다.

### 여섯 가지 테스트 유형

| 유형 | 목적 | 답하는 질문 |
|---|---|---|
| **Smoke** | "validate that your script works and that the system performs adequately under minimal load" | 스크립트가 제대로 돌긴 하나 |
| **Average-load** | "assess how your system performs under expected normal conditions" | 평상시 트래픽에서 괜찮은가 |
| **Stress** | "assess how a system performs at its limits when load exceeds the expected average" | 피크에서 버티나 |
| **Soak** | "assess the reliability and performance of your system over extended periods" | 오래 돌리면 새는 데가 있나 |
| **Spike** | "validate the behavior and survival of your system in cases of sudden, short, and massive increases in activity." | 갑작스런 폭증에서 살아남나 |
| **Breakpoint** | "gradually increase load to identify the capacity limits of the system." | 한계가 어디인가 |

**smoke 를 건너뛰지 않는 것**이 실질적으로 가장 많은 시간을 아낍니다. 스크립트 오류로 날린 회차와 서버 한계로 실패한 회차는 결과가 비슷하게 생겼습니다.

다만 **이 분류를 절대적인 것으로 다루지 말라고 문서 스스로 경고합니다.**

> "no single test can uncover all issues. What's more, the categories themselves are relative to use cases. A stress test for one application is an average-load test for another. Indeed, no consensus even exists about the names of these test types"

> "Avoid thinking in absolutes."

---

## 4. 도구가 내놓는 숫자들

k6 내장 지표의 정의는 공식 문서 원문 그대로입니다.

| 지표 | 정의 | 읽을 때 |
|---|---|---|
| `http_reqs` | "How many total HTTP requests k6 generated." | 총량보다 **초당 값**이 목표와 맞는지가 핵심 |
| `http_req_duration` | "Total time for the request. It's equal to `http_req_sending + http_req_waiting + http_req_receiving` (i.e. … without the initial DNS lookup/connection times)." | 가장 중요한 지연 지표. **DNS·연결 수립 시간이 빠져 있다고 문서가 명시** |
| `http_req_waiting` | "Time spent waiting for response from remote host (a.k.a. 'time to first byte', or 'TTFB')." | 사실상 서버가 일한 시간. 크면 서버 내부를 의심 |
| `http_req_blocked` / `connecting` / `tls_handshaking` | 연결 슬롯 대기 / TCP 수립 / TLS 핸드셰이크 | `duration` 에 **안 들어감**. 커넥션 재사용이 안 되면 여기가 큼 |
| `http_req_failed` | "The rate of failed requests according to setResponseCallback." | 기본은 상태코드 200~399 기준 |
| `checks` | "The rate of successful checks." | 통과율만 기록. **판정에는 영향 없음.** 요약에 뜨는 `checks_total`·`checks_succeeded`·`checks_failed` 는 "cannot be used in thresholds" |
| `iterations` | "The aggregate number of times the VUs execute the JS script (the `default` function)." | 요청 1개짜리 스크립트면 `http_reqs` 와 거의 같음 |
| `dropped_iterations` | "The number of iterations that weren't started due to lack of VUs (for the arrival-rate executors) or lack of time (expired `maxDuration` in the iteration-based executors)." | **회차 유효성의 1차 관문.** 괄호 두 개가 곧 원인 두 갈래 |
| `vus` / `vus_max` | 현재 활성 VU / 상한 | 실제가 상한에 붙으면 부하를 못 만든 것 |

지표 타입은 4종(Counter / Gauge / Rate / Trend)이고, **타입이 무엇을 계산할 수 있는지를 정합니다.** 백분위는 Trend 에서만 나오고, 실패율은 Rate 입니다. Trend 의 기본 요약 출력은 `avg`·`min`·`med`·`max`·`p(90)`·`p(95)` 입니다.

참고로 1절의 신호↔지표 매핑은 제가 붙인 게 아닙니다. k6 문서가 직접 대응을 밝힙니다.

> "In other terminology, these metrics measure traffic (in requests), availability (in error rate), and latency (in request duration). SREs might recognize these metrics as three of the four Golden Signals."

### 에러의 두 층 — 상태코드와 업무 결과

가장 많이 놓치는 지점입니다. SRE Book 은 에러의 형태를 셋으로 나눕니다.

> "either explicitly (e.g., HTTP 500s), implicitly (for example, an HTTP 200 success response, but coupled with the wrong content), or by policy."

도구의 기본 실패 판정은 **첫 번째만** 잡습니다. k6 의 기본값은 "requests with status codes between 200 and 399 are considered 'expected'" 입니다. 즉 **업무 실패를 200 으로 내려주는 API 라면 `http_req_failed` 는 항상 0%** 입니다.

그래서 응답 본문의 결과 코드를 보는 **별도의 실패율 지표**가 필요합니다. `check` 로 본문을 검증해 Rate 지표를 만들거나 커스텀 Rate 를 직접 정의합니다. 다만 check 만으로는 판정이 되지 않는데, 그 이야기는 8절에서 이어집니다.

세 번째 형태인 **by policy** 는 부하테스트와 가장 직결됩니다 — "(for example, \"If you committed to one-second response times, any request over one second is an error\")". 응답이 성공했고 내용도 맞지만 **약속한 시간을 넘겼으면 에러**라는 정의이고, 이것을 코드로 옮긴 것이 곧 threshold 입니다.

`http_req_failed` 자체에도 함정이 둘 있습니다. 판정이 **리다이렉트에도 적용**되어 200 만 기대값으로 두면 앞선 301 이 실패로 집계되고("This does include all redirects…"), 콜백을 `null` 로 두면 **지표 자체가 방출되지 않습니다**("Setting the callback to null disables … the emitting of `http_req_failed`").

---

## 5. avg 말고 p95 를 보는 이유

응답시간 분포는 **좌우 대칭이 아닙니다.** 대부분 빠르고 일부가 아주 느린, 오른쪽으로 긴 꼬리를 가집니다. 이 비대칭 때문에 평균은 어느 사용자의 경험도 대표하지 못합니다.

![응답시간 분포와 백분위 위치](/images/load-test-latency-distribution.svg)

SRE Book 이 수치로 보여줍니다.

> "If you run a web service with an average latency of 100 ms at 1,000 requests per second, 1% of requests might easily take 5 seconds."

평균 100ms 라는 한 숫자 뒤에 **초당 10명이 5초를 기다리는** 상황이 숨을 수 있습니다. 그리고 그 꼬리는 위로 전파됩니다.

> "If your users depend on several such web services to render their page, the 99th percentile of one backend can easily become the median response of your frontend."

의존 서비스가 여러 개면 각 백엔드의 꼬리가 합쳐져 **프론트엔드의 중앙값**이 됩니다. 백엔드 하나의 p99 를 "1%의 이야기"로 넘길 수 없는 이유입니다.

| 통계량 | 뜻 | 쓰임 |
|---|---|---|
| `med` (p50) | 절반은 이보다 빨랐다 | 전형적인 처리 속도. 서버 본연의 속도에 가까움 |
| `avg` | 산술 평균 | **단독으로 쓰지 않음.** 느린 소수에 크게 흔들림 |
| `p95` | 100건 중 95건이 이 안에 끝남 | 판정 기준으로 흔히 쓰임. 꼬리를 포함하되 극단값은 배제 |
| `p99` | 100건 중 99건 | 더 엄격. **포화의 조기 경보**로도 씀 |
| `max` | 가장 느렸던 한 건 | **원인 추적용.** 판정 기준으로 쓰면 안 됨 |

`avg` 가 `med` 보다 눈에 띄게 크면 그 자체가 **꼬리가 있다는 신호**입니다. 둘이 거의 같으면 분포가 대칭에 가깝다는 뜻입니다.

### 백분위 하나로도 부족하다

SRE Book 의 권고는 "p95 를 봐라"가 아니라 **분포 자체를 저장하라**입니다.

> "The simplest way to differentiate between a slow average and a very slow 'tail' of requests is to collect request counts bucketed by latencies (suitable for rendering a histogram), rather than actual latencies"

그리고 높은 백분위를 사후 판정이 아니라 **선행 지표**로 씁니다.

> "Measuring your 99th percentile response time over some small window (e.g., one minute) can give a very early signal of saturation."

부하를 계단으로 올리며 구간별 p99 를 보는 것이 무릎을 찾는 방법인 이유가 여기 있습니다.

### 백분위는 합산·평균되지 않는다

부하 생성기를 여러 대(또는 여러 파드)로 분산했을 때, **파드별 p95 를 산술평균하면 안 됩니다.** 백분위는 분포의 순위 통계량이라 부분집합의 백분위로 전체 백분위를 복원할 수 없습니다. 전체 p95 를 얻으려면 원 표본이나 히스토그램을 합친 뒤 다시 계산해야 합니다 — 바로 위에서 인용한 "collect request counts bucketed by latencies" 가 그렇게 합칠 수 있는 형태로 저장하라는 권고입니다.

(비합산성 자체는 백분위의 정의에서 따라 나오는 것이지 인용 문서의 문장은 아닙니다. 한 가지 정밀화: 전체 p95 를 복원할 수는 없어도, 샤드별 p95 의 최솟값과 최댓값 사이에 있다는 경계는 성립합니다.)

### max 를 다루는 법

> 이 절은 1차 문서 근거가 아니라 실무 관행입니다.

`max` 는 판정 기준이 아니라 **조사의 시작점**입니다. 큰 표본에서 극단값 한 건은 거의 항상 존재하고, 그 원인이 부하와 무관한 주기적 이벤트(캐시·자격증명 갱신, JIT 예열, GC, 콜드스타트)인 경우가 많습니다.

확인할 것은 값의 크기가 아니라 **부하량에 따라 늘어나는가**입니다. 부하와 무관하게 일정 주기로 발생한다면 부하로 인한 열화가 아닙니다.

### Apdex — 백분위 대신 만족도 지수로 요약하기

같은 응답시간 표본을 **0~1 사이 지수 하나**로 환산하는 표준도 있습니다. Apdex Alliance 의 기술 스펙 V1.1(2007)이 원 규격입니다.

임계값 T 를 정하면 구역이 자동으로 정해집니다. Satisfied(0~T), Tolerating(T~F), Frustrated(F 초과) 이고, **F 는 자유 파라미터가 아니라 F = 4T** 입니다.

```
Apdex_T = ( Satisfied count + Tolerating count / 2 ) ÷ Total samples
```

읽을 때의 주의점 셋:

1. **T 없는 Apdex 는 비교 불가능합니다.** 스펙이 "The value of T must be clearly displayed in association with the Apdex score" 로 못박습니다. 표기는 `0.90 [4.0]` 형태입니다. 같은 0.90 이라도 T=0.5초와 T=8초는 전혀 다른 이야기입니다.
2. **표본 100건 미만이면 값에 `*` 를 붙여야 합니다.** "The minimum sample size for normal reporting of the index is 100 samples within a report group". 표본이 0이면 `NS` 이고, 최소 1건은 있어야 계산할 수 있습니다.
3. **서버 측 실패는 응답시간과 무관하게 frustrated 로 셉니다.** "Server aborts … are counted as a frustrated sample regardless of the Task time measurement", 404 같은 애플리케이션 에러도 마찬가지입니다. 반면 **사용자가 중단한 경우는 고정되지 않습니다** — "user aborts can fall into any of the satisfied, tolerating, frustrated zones".

등급은 0.94~1.00 Excellent / 0.85~0.93 Good / 0.70~0.84 Fair / 0.50~0.69 Poor / 0.00~0.49 Unacceptable 인데, 스펙 표 제목이 "Examples where T=4" 이므로 **T=4 기준의 예시**로 읽어야 합니다.

---

## 6. 같은 요청인데 숫자가 다른 이유

보고서마다 응답시간이 다르게 나오는 것은 대개 **틀린 게 아니라 재는 위치가 다른** 것입니다.

![계측 위치별 포함 구간](/images/load-test-measurement-points.svg)

| 계측 위치 | 포함 구간 | 무엇을 판단하나 |
|---|---|---|
| **부하 생성기** | 전 구간 왕복 | 사용자 체감에 가장 가까움. 단 생성기가 원격이면 그 네트워크가 섞임 |
| **게이트웨이·프록시** | 인프라 진입 이후 | 내부만의 성능. 요청률·상태코드의 **독립 검증** |
| **APM (서버 내부)** | 애플리케이션 안에서 보낸 시간 | 느린 쿼리·외부 호출 추적 |

바깥쪽에서 잴수록 값이 큽니다. 서비스 메시는 이를 아예 명시적으로 다룹니다. Istio 표준 메트릭 `istio_request_duration_milliseconds` 는 `reporter` 라벨로 관측 주체를 구분하는데, 값은 "`destination` if report is from a server Istio proxy and `source` if report is from a client Istio proxy or a gateway" 입니다. **양단에 프록시가 있는 구성이라면 같은 요청이 각각 기록**된다는 뜻입니다(destination 쪽만 있는 구성에서는 한 번만 기록됩니다 — 이 조건은 제 보충이고 인용문에 없습니다).

실제로 쓰는 방법은 **세 값이 서로 모순되지 않는지 확인**하는 것입니다.

- 부하 생성기가 잰 초당 요청 수와 게이트웨이의 요청률이 맞는가
- 부하 생성기의 에러율과 게이트웨이의 상태코드 분포가 맞는가

맞으면 계측을 신뢰할 수 있고, **한쪽만 어긋나면 계측 오류이거나 그 사이 구간에 문제가 있다는 신호**입니다.

---

## 7. 여유가 얼마나 남았는지 보는 값들

네 축 중 **Saturation** 에 해당하는, 부하 생성기 바깥에서 봐야 하는 지표들입니다. **운영 스펙은 여기서 산출됩니다.**

| 지표 | 무엇을 보나 |
|---|---|
| **CPU 사용률** | 가장 중요한 여유 지표. 부하 계단별로 기록해야 "부하 X 일 때 Y%" 라는 선이 그려집니다 |
| **힙 메모리** | 부하 중 증가는 정상입니다. 볼 것은 **GC 후에도 계속 우상향하는지**(누수 의심)와 **OOM 발생 여부** |
| **인스턴스 안정성** | 재시작·OOMKilled·CrashLoopBackOff 가 **모두 0이어야** 그 회차 결과가 유효합니다. 중간에 재시작됐다면 그 구간 수치는 신뢰할 수 없습니다 |
| **DB 커넥션 · 슬로우 쿼리** | 인스턴스를 늘리면 **인스턴스 수 × 커넥션 풀** 만큼 DB 연결이 늘어, DB 가 먼저 한계에 닿을 수 있습니다 |
| **큐 길이 · 스레드 풀** | 요청이 처리되지 못하고 쌓이기 시작하는 지점 |
| **Apdex** | 5절 참고. 여러 서비스를 한 눈금으로 비교할 때 |

여기서 **사용률 100%를 목표로 잡지 않는 것**이 핵심입니다. 앞서 인용한 SRE Book 의 문장이 그대로 적용됩니다 — "many systems degrade in performance before they achieve 100% utilization".

---

## 8. 합격선을 코드로 적어두기

k6 는 합격 기준을 스크립트에 적어두고 자동으로 판정합니다.

> "Thresholds are the pass/fail criteria that you define for your test metrics. If the performance of the system under test (SUT) does not meet the conditions of your threshold, the test finishes with a failed status."

실패하면 non-zero exit code 로 끝나므로 CI 게이트로 쓸 수 있습니다.

```js
// 일반 예시입니다
export const options = {
  thresholds: {
    'http_req_failed{phase:load}':   ['rate<0.01'],   // 실패 1% 미만
    'http_req_duration{phase:load}': ['p(95)<200'],   // 95%가 200ms 안에
    'business_fail_rate{phase:load}': ['rate<0.01'],  // 업무 실패율도 별도로
  },
};
```

### check 는 판정하지 않는다

가장 자주 오해되는 지점입니다.

> "Checks are similar to what many testing frameworks call an *assert*, but failed checks do not cause the test to abort or finish with a failed status."

check 는 통과율을 **기록만** 합니다. 판정에 반영하려면 threshold 와 엮어야 합니다.

> "Each check creates a rate metric. To make a check abort or fail a test, you can combine it with a Threshold."

역할이 나뉘어 있습니다 — **check 는 관측, threshold 는 판정.** 4절에서 만든 업무 실패율 check 도, 그 Rate 지표에 threshold 를 따로 걸어야 비로소 합격/불합격에 들어옵니다.

**주의:** 요약 화면에 뜨는 `checks_total`·`checks_succeeded`·`checks_failed` 는 표시용이라 "cannot be used in thresholds" 입니다. threshold 를 거는 대상은 **각 check 가 만드는 rate 지표**입니다.

### 태그로 판정 구간 좁히기

위 예시의 `{phase:load}` 가 태그 서브메트릭 문법입니다. 이게 실무에서 결정적인 이유는 **판정에서 빼야 할 구간이 거의 항상 있기 때문**입니다.

- **예열(warmup) 구간** — 콜드스타트·JIT·커넥션 풀 초기화·캐시 적재로 초반 몇 건이 극단적으로 느립니다. **표본이 적은 초기에 이 몇 건이 p95 를 지배합니다.**
- **본 측정 대상이 아닌 요청** — 토큰 발급, 사전 데이터 준비 같은 보조 호출.

이들을 같은 태그 밖으로 빼면 판정이 실제 측정 구간만 보게 됩니다.

제약이 하나 있습니다 — "Since thresholds are defined as the properties of a JavaScript object, you can't specify multiple ones with the same property name." 같은 지표·같은 태그 조합에 기준을 두 개 걸려면 배열 안에 나열해야 합니다.

### abortOnFail — 끄고 켜는 기준

- `abortOnFail` — "Whether to abort the test if the threshold is evaluated to false before the test has completed."
- `delayAbortEval` — "If you want to delay the evaluation of the threshold to let some metric samples to be collected, you can specify the amount of time to delay using relative time strings like `10s`, `1m` and so on."

문서가 `delayAbortEval` 의 존재 이유를 명시합니다.

> "When you set `abortOnFail`, the test run stops as soon as the threshold fails. Sometimes, though, a test might fail a threshold early and abort before the test generates significant data. To prevent these cases, you can delay `abortOnFail` with `delayAbortEval`."

클라우드 실행에서는 중단이 늦어질 수 있습니다.

> "When k6 runs in the cloud, thresholds are evaluated every 60 seconds. Therefore, the `abortOnFail` feature may be delayed by up to 60 seconds."

**어느 쪽이 맞는지는 테스트의 목적이 정합니다.**

| 목적 | 권장 | 이유 |
|---|---|---|
| 한계점(breakpoint) 찾기 | `abortOnFail: true` | breakpoint 는 "must be stopped before it completes the scheduled execution" 이고, threshold 로 멈추려면 "you must define `abortOnFail` as true"(수동 `Ctrl+C` 도 문서가 제시하는 대안) |
| 계단별 자원 사용량 기록 | 중단하지 않음 | 중간에 멈추면 **다음 계단의 대응 관계를 얻지 못함** (문서 권고가 아니라 제 판단) |

여기서 흔한 사고가 하나 있습니다. **램프업 초기에 표본이 몇 건뿐일 때 콜드스타트 한 건이 p95 를 넘겨 테스트가 시작 직후 중단되는 것**입니다 — 문서가 정확히 이 상황을 "a test might fail a threshold early and abort before the test generates significant data" 로 적고 있습니다. 이때 반사적으로 `abortOnFail` 을 꺼버리기 쉬운데, 해법은 두 가지입니다.

1. `delayAbortEval` 로 **표본이 쌓일 때까지 평가를 미루기** — 이 옵션이 존재하는 이유 자체입니다.
2. 예열 구간을 태그로 분리해 **판정 대상에서 빼기**.

`abortOnFail` 을 끄는 것은 "끝까지 데이터를 남겨야 한다"는 **별개의 목적**이 있을 때의 선택이지, 조기 중단 문제의 해법이 아닙니다.

---

## 9. 결과가 나오면 이 순서로 본다

지연시간부터 보면 안 됩니다. 각 단계가 **뒤 단계의 전제**이기 때문입니다.

```
회차 유효  →  부하 달성  →  에러 없음  →  지연 기준 내  →  여유 확인  →  이상값 규명
   │            │             │              │
   │            │             │              └ 에러가 있으면 지연 해석이 왜곡됨
   │            │             └ 부하가 목표에 못 미치면 에러율의 의미가 다름
   │            └ 회차가 무효면 부하 수치 자체가 무의미
   └ 여기서 걸리면 나머지는 볼 필요 없음
```

**1. 이 회차가 유효한가**
인스턴스 재시작·OOM·크래시 루프가 0인가. `dropped_iterations` 가 무시할 수준인가("A few dropped iterations might indicate a quick network error. Many dropped iterations might indicate that your SUT has completely stopped responding."). `vus` 가 `vus_max` 에 붙지 않았는가. 여기서 걸리면 나머지 수치는 볼 필요가 없습니다.

**2. 목표 부하를 실제로 만들었나**
`http_reqs` 의 **초당 값**이 목표와 맞는가. 게이트웨이의 요청률과도 일치하는가.

**3. 에러가 없는가**
상태코드 기반 실패율과 업무 결과 기반 실패율을 **둘 다**. 게이트웨이의 상태코드 분포로 교차 확인.

**4. 지연시간이 기준 안인가**
구간별 p95·p99. `avg` 와 `med` 의 격차로 꼬리 확인. 계단을 올려도 평평한지, 어디서 꺾이는지. `http_req_waiting`(TTFB)이 `duration` 의 대부분이면 서버 내부가 원인.

**5. 여유가 얼마인가**
여기서 운영 스펙이 산출됩니다. 계단별 CPU·메모리·커넥션 풀·큐 길이. 메모리는 부하 중 증가하는 것이 정상이고, 봐야 할 것은 **GC 후에도 우상향하는지**(누수 의심)와 **OOM 여부**입니다. 그리고 **애플리케이션 자원과 하류 자원(DB·외부 API)을 나눠 봐야** 합니다.

**6. 이상값의 정체**
`max` 가 튄 건들이 무엇이었는지 로그·APM 으로 확인. 부하 때문인지, 주기적 이벤트인지.

### 무릎이 어디에 있느냐가 결론을 바꾼다

5번이 중요한 이유가 있습니다. 한계에 먼저 닿는 자원이 무엇이냐에 따라 **처방이 정반대**가 됩니다.

- **애플리케이션 CPU** 가 먼저 포화 → 인스턴스를 늘리면 개선됩니다.
- **공유 자원(DB·외부 API·잠금)** 이 먼저 포화 → 인스턴스를 늘려도 개선되지 않고, 커넥션 수가 늘어 **오히려 악화**될 수 있습니다.

후자의 경우 결론은 "스펙을 올리자"가 아니라 "**이 공유 자원이 병목이다**" 가 됩니다. 부하 구간마다 하류 자원도 함께 기록해야 이 구분이 가능합니다. (이 처방은 1차 문서 근거가 아니라 용량 산정 관행입니다.)

---

## 정리

- 부하테스트의 답은 단일 수치가 아니라 **"이 자원으로 N TPS, 그때 사용률 M%"** 라는 대응 관계다.
- **네 축을 동시에** 본다. Traffic / Latency / Errors / Saturation.
- 처리량이 질문이면 **열린 모델**(도착률 기반). 닫힌 모델은 coordinated omission 으로 최악의 표본을 숨긴다.
- `dropped_iterations` 와 `vus_max` 도달 여부가 **회차 유효성의 관문**이다.
- 지연시간은 **p95·p99** 로 읽고, `avg`-`med` 격차로 꼬리를 감지하고, `max` 는 조사 시작점으로만 쓴다. **백분위는 합산·평균되지 않는다.**
- 상태코드 실패율만 보면 **업무 실패가 통째로 누락**될 수 있다.
- 값이 다르면 대개 **재는 위치가 다른 것**이다. 세 계측 지점이 서로 모순되지 않는지 본다.
- **check 는 관측, threshold 는 판정.** 예열 구간은 태그로 분리하고, 조기 중단은 `delayAbortEval` 로 푼다.

---

## 참고 자료

- [Grafana k6 Docs — Open vs closed models](https://grafana.com/docs/k6/latest/using-k6/scenarios/concepts/open-vs-closed/)
- [Grafana k6 Docs — Metrics reference](https://grafana.com/docs/k6/latest/using-k6/metrics/reference/)
- [Grafana k6 Docs — Arrival-rate VU allocation](https://grafana.com/docs/k6/latest/using-k6/scenarios/concepts/arrival-rate-vu-allocation/)
- [Grafana k6 Docs — Dropped iterations](https://grafana.com/docs/k6/latest/using-k6/scenarios/concepts/dropped-iterations/)
- [Grafana k6 Docs — Thresholds](https://grafana.com/docs/k6/latest/using-k6/thresholds/) · [Checks](https://grafana.com/docs/k6/latest/using-k6/checks/)
- [Grafana k6 Docs — Test types](https://grafana.com/docs/k6/latest/testing-guides/test-types/) · [Breakpoint testing](https://grafana.com/docs/k6/latest/testing-guides/test-types/breakpoint-testing/)
- [Google SRE Book — Monitoring Distributed Systems](https://sre.google/sre-book/monitoring-distributed-systems/)
- [Apdex Technical Specification V1.1](https://www.apdex.org/wp-content/uploads/2020/09/ApdexTechnicalSpecificationV11_000.pdf)
- [Istio — Standard Metrics](https://istio.io/latest/docs/reference/config/metrics/)

> 문서 인용은 모두 2026-09-10 기준입니다. k6 문서 URL 은 `latest` 셀렉터라 특정 버전에 고정되어 있지 않습니다.
