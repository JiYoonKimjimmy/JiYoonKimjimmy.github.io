---
layout: post
title : Strategy Pattern
date  : 2022-07-02
image : strategy-pattern.png
tags  : design-pattern strategy-pattern 디자인패턴 전략패턴
---

## Strategy Pattern
### 전략 패턴
**전략 패턴**은 객체들의 각각 기능에 대해서 유사한 기능을 캡슐화하여 별도의 전략 클래스로 생성하고, 객체의 행위를 변경하는 경우, 객체의 기능을 직접 수정하지 않고, 전략 클래스의 기능을 변경해주기만 하면 된다는 목적이 있는 패턴이다.

더 쉽게 정리하면,
1. 유사한 기능을 가진 객체들에 대해서 전략 클래스를 생성하고, 객체들은 그 전략 클래스의 기능을 참조한다.
2. 기능 수정이 필요하면 전략을 바꿔주면 된다.

---

## Why?
먼저 전략 패턴이 필요한 예제를 살펴보면, `Bus` 와 `Train` 이란 교통수단의 객체의 `move` 라는 기능을 정의할 때 각각의 상세 기능에 대한 차이가 있다.

{% highlight java %}
public interface Movable {
    void move();
}

class Bus implements Movable {
    @Override
    public void move() {
        System.out.println("Move by road.");
    }
}

class Train implements Movable {
    @Override
    public void move() {
        System.out.println("Move by rail.");
    }
}
{% endhighlight %}

`Bus` 와 `Train` 의 `move` 함수의 기능을 수정하고자 하면, 각각 클래스를 직접 수정하는 방법밖에 없다. 이는 `Java SOLID 원칙` 중에 **Open-Closed Principle(`OCP`)** 원칙을 위반하게 된다.

> [Java SOLID 원칙 관련 글](/2021/02/28/JAVA_SOLID_PRINCIPLE/)

---

## 구현
**전략 패턴**은 각각의 세부 클래스 기능 수정을 최소화하기 위해,

##### 1. **전략 클래스**라는 새로운 인터페이스를 생성하고, 그 인터페이스를 구현하는 **상세 전략 클래스**를 생성한다.

{% highlight java %}
public interface Movable {
    void move();
}

class MoveByRoad implements Movable {
    @Override
    public void move() {
        System.out.println("Move by road.");
    }
}

class MoveByRail implements Movable {
    @Override
    public void move() {
        System.out.println("Move by rail.");
    }
}
{% endhighlight %}

##### 2. 위와 같이 유사한 기능들에 대한 전략을 정의한 전략 클래스를 참조 구현할 수 있는 클래스를 구성하고, 각 `Bus` 와 `Train` 과 같은 구현 객체가 상속받도록 한다.

{% highlight java %}
class Movable {
    private MoveStrategy moveStrategy;

    public void move() {
        moveStrategy.move();
    }

    public void setMoveStrategy(MoveStrategy moveStrategy) {
        this.moveStrategy = moveStrategy;
    }
}

class Bus extends Movable { }
class Train extends Movable { }
{% endhighlight %}

##### 3. `Client` 는 `Bus` 와 `Train` 클래스를 참조하면 `move()` 함수를 호출하기 전에 각 클래스에 전략을 부여 및 변경해주면서 기능 정의를 할 수 이다.

{% highlight java %}
class Client {
    public static void main(String[] args) {
        Bus bus = new Bus();
        Train train = new Train();

        bus.setMoveStrategy(new MoveByRoad());
        train.setMoveStrategy(new MoveByRail());
        bus.move();
        train.move();

        // change strategy of bus
        bus.setMoveStrategy(new MoveByRail());
        bus.move();
    }
}
{% endhighlight %}

---

## 정리
- **전략 패턴**은 각 기능 구현하는 클래스의 수정을 최소화하기 위한 패턴이다.(`OCP` 원칙)
- **상위 전략 클래스**를 생성하고, 각 **세부 전략 클래스**에서 기능에 대한 전략을 구현한다.
- 전략을 참조 및 설정하는 클래스를 생성하고, 해당 클래스를 상속해주는 세부 클래스에서는 간단한 `setter` 만으로 전략을 변경이 가능하게끔 구성한다.

---

#### 출처
- [Wikipedia - 전략 패턴](https://ko.wikipedia.org/wiki/%EC%A0%84%EB%9E%B5_%ED%8C%A8%ED%84%B4)
- [victolee 님의 tistory - 전략 패턴 (Strategy Pattern)](https://victorydntmd.tistory.com/292)
