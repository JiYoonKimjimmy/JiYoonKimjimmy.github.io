---
layout: post
title : Quick Sort Algorithm 퀵 정렬 알고리즘
date  : 2022-07-22
image : quick_sort.png
tags  : algorithm quick-sort 퀵정렬 알고리즘
---

## Quick Sort Algorithm
### 퀵 정렬 알고리즘
**퀵 정렬 알고리즘**은 분할 정복 알고리즘을 기반으로 비교적 많이 사용하는 정렬 알고리즘이다. 기본적인 시간 복잡도는 `O(NlogN)` 이지만, 최악의 경우에는 `O(N^2)` 이 될 수도 있다.

#### 특징
- 분할 정복 활용
- 시간 복잡도 : `O(NlogN)`

---

### 구현 순서
1. 특정한 값(Pivot)을 기준으로 잡아서,
2. 오른쪽으로 큰 수를 검색하고,
3. 왼쪽으로는 작은 수를 검색한 뒤,
4. 검색된 큰 수와 작은 수를 Swap해주고, 1 ~ 4번을 반복!
5. 오른쪽 검색과 왼쪽 검색이 교차가 되는 순간, Pivot과 작은 수를 Swap!
6. 교체된 Pivot을 기준으로 왼쪽과 오른쪽이 나눠지고,
7. 왼쪽 영역과 오른쪽 영역을 각각 1 ~ 4번을 반복!

#### Key Point
- `퀵 정렬` 은 `투 포인터` 와 pivot 을 설정하여 집합 분리하여 정렬한다.

1. `start < pivot` 인 경우, `start++` 이동
2. `end > pivot` 인 경우, `end--` 이동
3. `start > pivot & end < pivot` 인 경우, start 와 end 를 swap! 하고, `start++`, `end--` 이동
4. `start == end` 될 때까지 `1 ~ 3` 번 과정 반복
5. `start == end` 인 경우, `start < pivot` 이면 `start + 1` 위치에 pivot 를 swap! 하고, `start > pivot` 이면 `start - 1` 위치에 pivot 를 swap!

- **분리 그룹이 1개 이하가 될 때까지,** `1 ~ 5` 번 까지의 과정을 반복한다.

---

### 구현
{% highlight java %}
public static void main(String[] args) {
    int N = 5;
    int K = 3;
    int[] A = {4, 1, 2, 3, 5};

    quickSort(A, 0, N - 1, K - 1);

    CommonUtil.printIntArray(A);
    System.out.println(A[K - 1]);
}

public static void quickSort(int[] A, int S, int E, int K) {
    if (S < E) {
        int P = partition(A, S, E);
        if (K < P) {
            quickSort(A, S, P - 1, K);
        } else if (K != P) {
            quickSort(A, P + 1, E, K);
        }
    }
}

public static int partition(int[] A, int S, int E) {
    int P = (S + E) / 2;
    CommonUtil.swap(A, S, P);       // start <> pivot swap!
    int pivot = A[S];
    int i = S, j = E;
    while (i < j) {
        while (pivot < A[j]) {
            j--;
        }
        while (i < j && pivot >= A[i]) {
            i++;
        }
        CommonUtil.swap(A, i, j);   // i <> j swap!
    }
    A[S] = A[i];
    A[i] = pivot;
    return i;
}
{% endhighlight %}

#### Java 8 Collection 을 활요한 퀵 정렬 구현
- `List` 컬렉션을 활용하여, 아래 3가지 그룹으로 나누고, 각 그룹 별로 다시 그룹을 나누고 정렬하는 방식

  - `lesser` : `pivot` 값보다 작은 그룹
  - `equal` : `pivot` 값과 같은 그룹
  - `greater` : `pivot` 값보다 큰 그룹


{% highlight java %}
public static void main(String[] args) {
    int[] sample = {3, 7, 8, 1, 5, 9, 6, 10, 2, 4};

    String result = sort(Arrays.stream(sample).boxed().collect(Collectors.toList()))
            .stream()
            .map(String::valueOf)
            .collect(Collectors.joining(" "));

    System.out.println(result);

}

private static List<Integer> sort(List<Integer> list) {
    if (list.size() <= 1) return list;

    int pivot = list.get(list.size() / 2);
    List<Integer> lesser = new ArrayList<>(), equal = new ArrayList<>(), greater = new ArrayList<>();

    for (int n : list) {
        if (n < pivot) {
            lesser.add(n);
        } else if (n == pivot) {
            equal.add(n);
        } else {
            greater.add(n);
        }
    }

    return Stream.of(sort(lesser), equal, sort(greater))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
}
{% endhighlight %}

---

#### 출처
- [Do it! 알고리즘 코딩 테스트: 자바 편](http://www.kyobobook.co.kr/product/detailViewKor.laf?mallGb=KOR&ejkGb=KOR&barcode=9791163033448)
- [9kong.log - 알고리즘/정렬 2. 퀵 정렬(Quick Sort)](https://velog.io/@kongji47/%EC%95%8C%EA%B3%A0%EB%A6%AC%EC%A6%98%EC%A0%95%EB%A0%AC-2.-%ED%80%B5-%EC%A0%95%EB%A0%ACQuick-Sort)
