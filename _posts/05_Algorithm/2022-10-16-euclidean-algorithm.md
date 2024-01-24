---
layout: post
title : Euclidean Algorithm 유클리드 호제법 알고리즘
date  : 2022-10-15
image : euclidean-algorithm.png
tags  : algorithm euclidean
---

## Euclidean Algorithm
### 유클리드 호제법 알고리즘
**유클리드 호제법** 알고리즘은 두 수의 최대 공약수를 구하는 알고리즘이다. 최대 공약수를 구하는 방법은 소인수분해를 이용하여 공통된 소수들의 곱으로 표현할 수 있지만, **유클리드 호제법** 알고리즘을 활용하면 간단하게 구현 가능하다.

#### 유클리드 호제법 수행 과정
1. 큰 수를 작은 수로 나누는 **MOD** 연산을 한다.
2. **1번** 단계에서의 작은 수와 **MOD** 연산 결과값을 다시 **MOD** 연산한다.
3. 나머지가 `0` 이 될 때까지 **2번** 단계를 반복하고, `0` 되는 순간의 작은 수가 최대 공약수가 된다.

> MOD 연산? A / B 나눈 나머지 값을 구하는 연산

---

### 유클리드 호제법 관련 문제
#### 최소 공배수 구하기
`A` 와 `B` 라는 자연수의 최소 공배수 구한다고 할때, 공식은 `최소 공배수 = (A * B) / 최대 공약수` 을 활용한다.

> 최소 공배수? `A의 배수`이면서 `B의 배수`이기도 한 제일 작은 수

> 유클리드 호제법 구현 Tip
> 반복문을 활용하는 것보단 재귀 방식을 활용하는 것이 효율적!

{% highlight java %}
public static void solution() {
    int a = 6, b = 10;
    int result = (a * b) / gcd(a, b);
    System.out.println("result = " + result);   // 30
}

private static int gcd(int a, int b) {
    if (b == 0) {
        return a;
    } else {
        return gcd(b, a % b);
    }
}
{% endhighlight %}


---

- [Do it! 알고리즘 코딩 테스트: 자바 편](http://www.kyobobook.co.kr/product)
- [HyunWoo_deV.log - 유클리드 호제법](https://velog.io/@l2hyunwoo/Algorithm-EuclideanAlgorithm)
