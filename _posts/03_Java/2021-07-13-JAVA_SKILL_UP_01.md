---
layout: post
title : Java Skill UP 을 하자! step.1
date  : 2021-02-28
image : java_01.png
tags  : java oop
---

## Java Skill UP !! step.1
`Java` 를 개발하면서 최근에는 `Stream`, `Optional` 등 `Lambda` 표현식으로 개발하는 경우를 많이 볼 수 있다. `JavaScript` 를 개발하면서 어느정도 `Lambda` 를 사용한 개발 방식이 익숙하다고 생각 했지만, 다른 언어인 만큼 문법적인 정리가 필요하고, 과연 `Java` 에서는 어떻게 `Lambda` 표현식을 구현하였는지 알아볼 필요가 있다고 생각하였다.<br>
최종 목표는 `Reactive Programming` 이란 무엇이고, 왜 `Spring Webflux` 가 나왔는지 살펴볼 예정이다.<br>
먼저 아래와 같은 개념을 살펴보자.

- Functional Programming
- Lambda

---

### Functional Programming <small>함수형 프로그래밍</small>
함수형 프로그래밍은 명령형이 아닌 선언적 방식으로 구현하여 흐름 제어를 명시적으로 기술하지 않고 프로그램 로직을 표현을 의미한다.

- 선언적 프로그램 : 흐름 제어를 추상화하고 데이터 흐름을 설명하는 코드 사용하는 것으로, 데이터의 입력이 주어지고 데이터를 다루는 과정(흐름)을 정의하는 방식

#### First Object, First Class Citizen <small>1급 객체</small>
##### 1급 객체 조건
- 변수나 데이터안으로 삽입 가능
- 파라미터 전달 가능
- 동적 프로퍼티 할당 가능
- 반환값으로 사용 가능

`Java` 에서는 함수는 *1급 객체* 에 포함될 수 없지만, 함수도 객체로 구분되는 `JavaScript` 에서는 **함수도 1급 객체가 될 수 있다.**<br>
`Java` 의 경우 ***함수형 인터페이스*** 를 통해 구현 가능하다.

##### 함수형 프로그래밍 조건 3가지
###### 순수 함수
- 같은 입력에 대해서는 같은 출력을 반환하는 함수
- 부작용(다른 요인에 대한 결과 변경)이 없는 함수

###### 고차함수
- 함수의 인자로 함수를 전달 가능
- 함수를 함수의 반환값으로 사용 가능

###### 익명 함수
- 람다식으로 표현되는 함수

---

### Lambda <samll>람다</small>
람다식(Lambda Expression) 이란 함수를 하나의 식(Expression) 으로 표현하는 것이다. 함수를 람다식으로 표현하면 함수명 없이 **익명의 함수** 로 사용 가능하다.<br>
`Java` 에서 *Stream* 함수들은 함수형 인터페이스를 받도록 되어있으며, 람다식은 반환값으로 함수형 인터페이스를 반환하고 있다.

{% highlight java %}
( parameters ) -> expression body       // 인자가 여러개 이고 하나의 문장으로 구성
( parameters ) -> { expression body }   // 인자가 여러개 이고 여러 문장으로 구성
() -> { expression body }               // 인자가 없고 여러 문장으로 구성
() -> expression body                   // 인자가 없고 하나의 문장으로 구성
{% endhighlight %}

#### Lambda + Functional Programming
##### 기존 익명 함수
{% highlight java %}
public class Lambda {
  public static void main(String[] args) {
    System.out.println(new MyLambdaFunction() {
      public int max(int a, int b) {
        retrun a > b ? a : b;
      }
    });
  }
}
{% endhighlight %}

##### Functional Interface 익명 함수
`Java` 에서는 `@FunctionalInterface` 라는 Annotation 을 통해 1개 뿐인 abstract 함수를 선언하고 함수형 인터페이스를 생성할 수 있다.

{% highlight java %}
@FunctionalInterface
interface MyLambdaFunction {
  int max(int a, int b);
}

public class Lambda {
  public static void main(String[] args) {
    MyLambdaFunction myLambdaFunction = (int a, int b) -> a > b ? a : b;
    System.out.println(myLambdaFunction.max(3, 5)); // out : 5
  }
}
{% endhighlight %}

###### 주의 사항
- Lambda 식으로 생성된 순수 함수는 함수형 인터페이스로만 사용 가능
- 함수형 인터페이스는 1개의 함수만을 갖도록 제한

---

### `Java`에서 제공하는 함수형 인터페이스
#### `Supplier<T>`
- 매개변수 없이 반환값만을 가지는 함수형 인터페이스

{% highlight java %}
Supplier<String> supplier = () -> "Hello world";
System.out.println(supplier.get()); // out : Hello world
{% endhighlight %}

#### `Cunsumer<T>`
- 매개변수 `T` 객체를 받아 소비하는 함수형 인터페이스
- `accept()` 추상메소드를 가지고 있지만, 추가적으로 `andThen()` 함수를 가지고 있어서, `accept()` 처리 후에 추가적인 처리 가능

{% highlight java %}
Consumer<String> consumer = s -> System.out.println(s + " World");
consumer.accept("Hello"); // out : Hello World

consumer = s -> System.out.println(s.split(" ")[0]);
consumer
  .accept("Hello World")          // out : Hello
  .andThen(System.out::println);  // out : Hello World
{% endhighlight %}

#### `Function<T, R>`
- `T` 객체로 매개변수받아 `R` 객체로 반환하는 함수형 인터페이스
- `apply()` 추상메소드에서 처리하고, `andThen()` 함수도 있지만, 첫번째 함수 실행 이전에 먼저 함수를 실행하여 처리해주는 `compose()` 함수를 포함

{% highlight java %}
Function<String, Integer> function = s -> s.length();
function.apply("Hello world");  // out : 11
{% endhighlight %}

#### `Predicate<T>`
- `T` 객체를 받아 처리후 `Boolean` 타입으로 반환 처리

{% highlight java %}
Predicate<String> predicate = (str) -> str.equals("Hello World");
System.out.println(predicate.test("Hello World"));  // out : true
{% endhighlight %}

#### `Operator<T>`
- `Function<T, R>` 인터페이스와 동일한 `apply()` 추상메소드를 처리하지만, 입력받은 인자와 동일한 타입을 반환
- `Function<T, R>` 인터페이스는 상속받고 있고, 다양한 형태의 `Operator<T>` 인터페이스를 제공

##### `UnaryOperator<T>`
{% highlight java %}
// ArrayList.replaceAll + UnaryOperator interface
UnaryOperator operator = i -> i * 2;
ArrayList list = Arrays.asList(1, 2);
list.replaceAll(operator);  // list : (1, 2) -> (2, 4)
{% endhighlight %}

##### `BinaryOperator<T, U, R>`
{% highlight java %}
BinaryOperator<Integer> operator = (first, second) -> first + second;
Stream<Integer> integerStream = Stream.of(1, 2, 3);
Optional<Integer> reduce = integerStream.reduce(operator);
System.out.println(reduce.get());   // out : 6
{% endhighlight %}

---

#### 출처
- [짧굵배 9. 함수형 프로그래밍과 람다](https://dinfree.com/lecture/language/112_java_9.html#m1)
- [망나니개발자 [Java] 람다식(Lambda Expression)과 함수형 인터페이스(Functional Interface) (2/5)](https://mangkyu.tistory.com/113)
