---
layout: post
title : Euler's phi 오일러 피 알고리즘
date  : 2022-10-15
image : phi-of-euler.png
tags  : algorithm phi-of-euler
---

## Euler's phi Algorithm
### 오일러 피(파이) 알고리즘
**오일러 피** 알고리즘은 N 까지 범위 안에서 서로소인 수를 찾아내는 알고리즘이다.

> 서로소? 최대공약수가 **1** 인 두 자연수

#### 오일러 피 수행 과정
1. 오일러 피의 범위 N 만큼의 배열(`P[N]`)을 선언한다.
2. `2` 부터 시작하여 현재 배열의 값과 인덱스가 같으면(= 소수일 때), 현재 선택된 숫자(`K`)의 배수에 해당하는 수를 배열에 끝까지 탐색하면서 `P[i] = P[i] - P[i]` / K 연산을 수행한다.
3. `P` 배열 끝까지 **2번** 단계를 반복한다.

---

### 구현
{% highlight java %}
private static void solution() {
    long n = 99;
    long result = n;

    // 제곱근까지만 탐색 진행
    for (long p = 2; p <= Math.sqrt(n); p++) {
        // p 가 소수인지 확인
        if (n % p == 0) {
            // 소수인 경우 결과값 변경
            result = result - result / p;
            while (n % p == 0) {
                // `2^7 * 11` 이라면 `2^7` 을 없애고 `11` 만 남김
                // `n` 을 *소인수*로 만든다.
                n /= p;
            }
        }
    }

    if (n > 1) {
        // 반복문에서 제곱근까지만 탐색하기 때문에, 1개의 소인수 구성이 남아있는 경우
        result = result - result / n;
    }

    System.out.println("result = " + result);
}
{% endhighlight %}

---

#### 출처
- [Do it! 알고리즘 코딩 테스트: 자바 편](http://www.kyobobook.co.kr/product)
- [까망 하르방 님 블로그 - 오일러 피(파이) 함수(Euler's phi function)](https://zoosso.tistory.com/243)
