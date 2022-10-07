---
layout: post
title : Sieve of Eratosthenes 에라토스테네스의 체
date  : 2022-10-07
image : sieve-of-eratosthenes.png
tags  : algorithm sieve-of-eratosthenes
---

## Sieve of Eratosthenes Algorithm
### 에라토스테네스의 체 알고리즘
**에라토스테네스의 체** 알고리즘은 **소수 찾기**에 많이 활용되는 알고리즘이다.

#### 에라토스테네스의 체 수행 과정
1. 소수 찾고자 하는 수를 1차원 배열에 나열한다.
2. 숫자 `1` 는 제외하고, `2` 부터 자기 자신을 배수로 하고 있는 숫자를 삭제한다.
3. 2번 과정을 반복하고, 남겨진 숫자 모음이 소수 모음이 된다.

#### 시간 복잡도
- 에라토스테네스의 체 알고리즘의 기본적인 시간 복잡도는 `O(N^2)` 라고 할 수 있다.
- 하지만, 배수가 되는 숫자를 삭제함으로서 시간 복잡도도 `O(Nlog(logN))` 으로 변하게 된다.

---

### 구현
#### 문제 설명
1 ~ 30 까지의 숫자들 중에서 소수인 숫자만 출력

{% highlight java %}
public static void solution() {
    int N = 30;
    int[] A = new int[30];

    for (int i = 1; i <= 30; i++) {
        A[i - 1] = i;
    }

    for (int i = 0; i < A.length; i++) {
        int t = A[i];
        if (t != 1) {
            for (int j = i + 1; j < A.length; j++) {
                if (A[j] % t == 0) {
                    A[j] = 1;
                }
            }
        }
    }

    List<Integer> list = new ArrayList<>();
    for (int j : A) {
        if (j != 1) {
            list.add(j);
        }
    }
    System.out.println("list = " + list);
}
{% endhighlight %}

---

#### 출처
- [Do it! 알고리즘 코딩 테스트: 자바 편](http://www.kyobobook.co.kr/product)
- [https://prezi.com/ctoncynjjmfm/presentation/](https://prezi.com/ctoncynjjmfm/presentation/)
