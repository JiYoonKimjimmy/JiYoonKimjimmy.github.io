---
layout: post
title : RxJava Observable
date  : 2021-08-22
image : rxjava.png
tags  : java oop reactive rxjava
---

## RxJava Observable
`Observable` class 는 `Observer` 디자인 패턴을 구현한다. 옵저버 패턴은 객체의 상태 변화를 관찰자 목록에 객체를 등록하고, **상태 변화 발생할때마다 함수를 호출하여 옵저버의 상태 변경에 대한 알림을 전달한다.**<br>

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

#### 출처
- [길은 가면, 뒤에 있다. - [RxJava] RxJava 프로그래밍(1) - 리액티브 프로그래밍](https://12bme.tistory.com/570)
