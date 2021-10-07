---
layout: post
title : RxJava Observable
date  : 2021-08-22
image : rxjava.png
tags  : java reactive rxjava
---

## RxJava Observable
`Observable` class 는 `Observer` 디자인 패턴을 구현한다. 옵저버 패턴은 객체의 상태 변화를 관찰자 목록에 객체를 등록하고, **상태 변화 발생할때마다 함수를 호출하여 옵저버의 상태 변경에 대한 알림을 전달한다.**

### Observable 종류
- `Observable` : 가장 기본적인 형태로서, 0 ~ N 개의 데이터 발행 (BackPressure 없음)
- `Single` : 단 1개의 데이터 발행 및 구독 처리
- `Maybe` : 0개 or 1개 데이터 발행 및 구독, `success` or `fail` or `complete` 결과 처리
- `Completable` : `success` or `fail` 결과만 발행
- `Flowable` : 0 ~ N 개 데이터 발행 (BackPressure 있음)

---

### Hot Observable vs Cold Observable
- `Hot Observable` : 구독자의 존재 여부와 상관없이 데이터를 발행하는 `Observable`
  - 다수의 구독자를 고려 가능하나, 구독자가 `Observable` 의 데이터를 처음부터 구독했는지 보장 불가
  - 시스템 이벤트, 센서 데이터, 주식 가격 등
- `Cold Observable` : 구독자가 구독을 하는 순간 데이터를 발행하는 `Observable`
  - 구독자가 구독을 하면, 준비된 데이터를 처음부터 발 처리
  - 웹 요청, 데이터베이스 조회, 파일 읽기 등

#### Hot Observable 를 Cold Observable 로 변환해주는 `Subject` class
`Subject` Class 는 `Hot Observable` 에서 `Cold Observable` 로 변환해줄 수 있으며, 발행자와 구독자의 속성을 모두 가지고 있다.<br>

### `RxJava` 의 주요 `Subject` Class
#### `AsyncSubject`
- `Observable` 에서 발행한 마지막 데이터 구독
- `onComplete()` 함수가 호출되는 순간 마지막 데이터 발행 및 구독 처리
![AsyncSubject](/images/asyncsubject.png)

{% highlight java %}
AsyncSubject<String> subject = AsyncSubject.create();
subject.subscribe(data -> System.out.println("subscribe #1 : " + data));
subject.onNext("one");
subject.onNext("two");
subject.subscribe(data -> System.out.println("subscribe #2 : " + data));
subject.onNext("three");
subject.onComplete();
// subscribe #1 : three
// subscribe #2 : three
{% endhighlight %}

#### `BehaviorSubject`
- 구독하는 순간, 가장 최근 데이터 혹은 기본값 데이터를 구독
![BehaviorSubject](/images/behaviorsubject.png)

{% highlight java %}
BehaviorSubject<String> subject = BehaviorSubject.createDefault("BLUE");
subject.subscribe(data -> System.out.println("subscribe #1 : " + data));
subject.onNext("RED");
subject.onNext("GREEN");
subject.subscribe(data -> System.out.println("subscribe #2 : " + data));
subject.onNext("WHITE");
subject.onComplete();
// subscribe #1 : BLUE
// subscribe #1 : RED
// subscribe #1 : GREEN
// subscribe #2 : GREEN
// subscribe #1 : WHITE
// subscribe #2 : WHITE
{% endhighlight %}

#### `PublishSubject`
- 가장 평범한 `Subject` Class 로서, 오직 구독자만 구독할 경우에만 데이터를 발행
![PublishSubject](/images/publishsubject.png)

{% highlight java %}
PublishSubject<String> subject = PublishSubject.create();
subject.subscribe(data -> System.out.println("subscribe #1 : " + data));
subject.onNext("one");
subject.onNext("two");
subject.subscribe(data -> System.out.println("subscribe #2 : " + data));
subject.onNext("three");
subject.onComplete();
// subscribe #1 : one
// subscribe #1 : two
// subscribe #1 : three
// subscribe #2 : three
{% endhighlight %}

#### `ReplaySubject`
- 새로운 구독자가 생기면 데이터의 처음부터 끝까지 다시 발행 (메모리 누수 위험)
![ReplySubject](/images/replaysubject.png)

{% highlight java %}
ReplaySubject<String> subject = ReplaySubject.create();
subject.subscribe(data -> System.out.println("subscribe #1 : " + data));
subject.onNext("one");
subject.onNext("two");
subject.subscribe(data -> System.out.println("subscribe #2 : " + data));
subject.onNext("three");
subject.subscribe(data -> System.out.println("subscribe #3 : " + data));
subject.onComplete();
// subscribe #1 : one
// subscribe #1 : two
// subscribe #2 : one
// subscribe #2 : two
// subscribe #1 : three
// subscribe #2 : three
// subscribe #3 : one
// subscribe #3 : two
// subscribe #3 : three
{% endhighlight %}

#### `ConnectableObservable`
- `Observable` 에서 데이터를 발행하더라도 데이터 발행을 유예하고 특정 시점(`connect() 함수 호출 시점`)에 발행 가능
![ConnectableObservable](/images/connectableobservable.png)

{% highlight java %}
String[] arr = {"one", "two", "three"};
Observable<String> observable = Observable.interval(100L, TimeUnit.MILLISECONDS)
        .map(Long::intValue)
        .map(i -> arr[i])
        .take(arr.length);
ConnectableObservable<String> source = observable.publish();
source.subscribe(data -> System.out.println("subscribe #1 : " + data));
source.subscribe(data -> System.out.println("subscribe #2 : " + data));
source.connect();

Thread.sleep(200);
source.subscribe(data -> System.out.println("subscribe #3 : " + data));
Thread.sleep(100);
// subscribe #1 : one
// subscribe #2 : one
// subscribe #1 : two
// subscribe #2 : two
// subscribe #1 : three
// subscribe #2 : three
// subscribe #3 : three
{% endhighlight %}

---

### `subscribeOn()` vs `observeOn()`
- `subscribeOn()` : `Observable` 객체 실행 `Thread` 지정
- `observeOn()` : 연쇄적인 연산 실행 `Thread` 지정

{% highlight java %}
List<Figure> figures = Arrays.asList(
        new Figure("Red", "Ball"),
        new Figure("Green", "Ball"),
        new Figure("Blue", "Ball")
);

Observable.fromIterable(figures)
        .subscribeOn(Schedulers.computation())                  // 내부적으로 Thread pool 생성
        .subscribeOn(Schedulers.io())                           // 필요할때마다 Thread 생성
        .doOnSubscribe(data -> PrintUtil.printData("doOnSubscribe"))
        .doOnNext(data -> PrintUtil.printData("doOnNext [init] : ", data))
        .observeOn(Schedulers.newThread())                      // 매번 새로운 Thread 생성
        .map(data -> {data.shape = "Square"; return data;})
        .doOnNext(data -> PrintUtil.printData("doOnNext [Ball -> Square] : ", data))
        .observeOn(Schedulers.newThread())
        .map(data -> {data.shape = "Triangle"; return data;})
        .doOnNext(data -> PrintUtil.printData("doOnNext [Square -> Triangle] : ", data))
        .observeOn(Schedulers.newThread())
        .subscribe(data -> System.out.println("subscribe : " + data));
{% endhighlight %}

{% highlight java %}
class Figure {
    String color;
    String shape;

    public Figure(String color, String shape) {
        this.color = color;
        this.shape = shape;
    }
}

class PrintUtil {
    static void printData(String message) {
        System.out.println("" + Thread.currentThread().getName() + " | " + message);
    }

    static void printData(String message, Object obj) {
        System.out.println("" + Thread.currentThread().getName() + " | " + message + " | " + obj.toString());
    }
}
{% endhighlight %}

---

### Flowable
- `BackPressure` 를 제어할 수 있는 `Observable`

#### BackPressure
- `Observable` 에서 데이터를 발행하는 속도를 `Observer` 의 구독하는 속도가 따라가지 못하는 현상 (`BackPressure` 또는 `배압`)
- 메모리가 overflow 가 되면서 `OutOfMemoryError` 발생 가능

#### BackPressure 방지를 위한 Flowable
##### Observable 의 BackPressure
{% highlight java %}
private static void backPressure() throws Exception {
    PrintUtil.printData("backPressure()");
    Observable.range(1, 10000)
            .doOnNext(i -> PrintUtil.printData("doOnNext", i))
            .observeOn(Schedulers.io())
            .subscribe(i -> {
                PrintUtil.printData("subscribe", i);
                Thread.sleep(100);
            });
    Thread.sleep(10000);
}
// `Observable` 는 배압 제어를 못하기 때문에 10000 건 모두 발행한다.
{% endhighlight %}

##### Flowable 의 BackPressure 제어
{% highlight java %}
private static void noBackPressure() throws Exception {
    PrintUtil.printData("noBackPressure()");
    Flowable.range(1, 10000)
            .doOnNext(i -> PrintUtil.printData("doOnNext", i))
            .observeOn(Schedulers.io())
            .subscribe(i -> {
                PrintUtil.printData("subscribe", i);
                Thread.sleep(100);
            });
    Thread.sleep(10000);
}
// `Flowable` 는 일정량 발행이 되면, 발행을 제어하고 구독 처리된다.
{% endhighlight %}

#### Observable vs Flowable
- When to use `Observable` ? 1,000개 미만의 이벤트 데이터 흐름이 발생하는 경우
- When to use `Flowable` ? 10,000개 이상의 데이터 흐름이 발생하는 경우
  - 디스트에서 파일 읽는 경우
  - DB 에서 데이터 읽는 경우
  - 네트워크 IO 실행하는 경우

#### BackPressure Strategy
- `Flowable` 구현일지라도 배압 제어를 통해 `MissingBackpressureException` 을 방지 필수

| Field | 내용 |
| :---: | --- |
| MISSION |  배압 전략 없음 |
| ERROR | 배압 현상 발생시 `MissingBackpressureException` 발생 |
| BUFFER | 데이터를 소비할 때까지 데이터를 Buffer 에 저장. 하지만 `OutOfMemoryError` 발생 가능 |
| DROP | 배압 현상이 발생한 경우 발행되는 데이터 모두 버림 |
| LATEST | 구독자가 새로운 데이터 구독 준비될 때까지 최신 데이터만 유지하고 나머지는 버림 |

#### `Flowable` BackPressure Strategy 3가지 연산자
  - `onBackPressureBuffer()`
  - `onBackPressureDrop()`
  - `onBackPressure=Latest()`

---

#### 출처
- [길은 가면, 뒤에 있다. - [RxJava] RxJava 프로그래밍(1) - 리액티브 프로그래밍](https://12bme.tistory.com/570)
- [HERSTORY [RxJava] RxJava 이해하기 - 7. Backpressure와 Flowable](https://4z7l.github.io/2020/12/23/rxjava-7.html)

#### 관련 예제
- [https://github.com/JiYoonKimjimmy/rxjava-starter.git](https://github.com/JiYoonKimjimmy/rxjava-starter.git)
