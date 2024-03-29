---
layout: post
title : Java SOLID 원칙
date  : 2021-02-28
image : java_solid.jpg
tags  : java oop
---

## 객체 지향 5대 원칙

### 객체 지향 프로그래밍의 5대 원칙?
* SRP(단일 책임 원칙)
* OCP(개방-폐쇄 원칙)
* LSP(리스코프 치환 원칙)
* ISP(인터페이스 분리 원칙)
* DIP(의존성 역전 원칙)

---

### 1. Single Responsibility Principle(SRP, 단일 책임 원칙)
**새로운 요구사항 반영과 프로그램 수정의 영향이 적으려면? 프로그램의 책임이 단 하나인 경우이다.**
***(프로그램의 책임은 '기능'으로 정의 가능하다.)***

프로그램이 가지고 있는 책임이 많다면 함수의 결합도가 높을 가능성이 있기 때문이다. 강한 경합도는 수정에 대한 `side-effect` 발생도가 높아지게 된다.

---

### 2. Open-Closed Principle(OCP, 개방-폐쇄 원칙)
***기존 코드 변경 없이(Closed) 신규 기능 추가 및 수정할 수 있는(Open) 프로그램 설계가 필요하다***

변경 없이 새로운 기능을 추가하거나 수정하는 작업은 자주 변경되는 기능은 수정하기 쉽게 설계하고, 변경되지 않는 기능은 영향을 받지 않게 하는 것이다.
`Java` 에서는 `Interface` 를 통해서 `OCP` 원칙을 적용할 수 있을 것이다.

{% highlight java %}
// OCP example-1
class Order {
  void pay() {
    System.out.print("pay by card");
  }
}

public class Client {
  public static void main(String[] args) {
      Order order = new Order();
      order.pay();
  }
}
{% endhighlight %}

`OCP example-1` 는 주문`Order`에 대한 결제 방식`pay()`을 정의하고 있다.
하지만, 주문의 결제 방식이 달라진다고 하면, `Order`클래스의 `pay`함수를 수정해야할 것이다. **이는 `OCP` 원칙에 위배된다.**

`OCP` 원칙을 만족하기 위해선, `pay`함수를 쉽게 변경할 수 있게 `Interface`로 분리시킬 수 있다.

{% highlight java %}
// OCP example-2
interface PayOrder {
  public void pay();
}

class Card implements PayOrder {
  @Override
  public void pay() {
    System.out.print("pay by card");
  }
}

class Cash implements PayOrder {
  @Override
  public void pay() {
    System.out.print("pay by cash");
  }
}
{% endhighlight %}

{% highlight java %}
// OCP example-3
class Order {
  private PayOrder type;

  public void setType(PayOrder type) {
    this.type = type;
  }

  public void pay() {
    type.pay();
  }
}

public class Client {
  public static void main(String[] args) {
      Order order = new Order();
      order.setType(new Card());
      order.pay();    // log : "pay by card"

      order.setType(new Cash());
      order.pay();    // log : "pay by cash"
  }
}
{% endhighlight %}

`OCP example-2`에서 `PayOrder` 인터페이스를 통해서 각 결제 수단 클래스의 `pay`함수를 재정의하도록 설계하였고,
`OCP example-3`에서 `Order`클래스의 `type`을 통해 결제 수단 객체를 지정하여 각 `pay` 함수를 호출하였을 때, 영향이 받지 않도록 하였다.
이와 같은 설계는 **Strategy Pattern(전략 패턴)** 이라는 디자인 패턴으로도 정의할 수 있다.

---

### 3. Liskov Substitution Principle(LSP, 리스코프 치환 원칙)
**자식 클래스는 부모 클래스가 가진 기능을 수행할 수 있어야 한다.**

부모 클래스와 자식 클래스 사이의 기능은 일관성이 있어야 하며, 부모 클래스의 객체 대신 자식 클래스의 객체를 사용해도 문제가 없어야 한다. 상속 관계에서는 일반화 관계(IS-A)가 성립해야 하고, 일반화 관계는 일관성이 있다는 것이다.

---

### 4. Interface Segregation Principle(ISP, 인터페이스 분리 원칙)
**하나의 클래스 안에 자신이 사용하지 않는 인터페이스는 구현하지 않아야 한다.**

자신이 사용하지 않는 기능의 인터페이스에는 영향을 받지 않아야 한다는 의미로, 스마트폰의 전화 기능은 통화를 할 때 사진 촬영을 필수로 하지 않는 것처럼 전화, 사진 촬영 등 다양한 기능을 독립적인 인터페이스로 구현하여, 서로에게 영향을 받지 않도록 설계해야 한다.
`ISP` 원칙에 적합한 시스템은 내부 의존성을 약화시켜 리팩토링, 수정, 재배포를 보다 쉽게 수행할 수 있게 된다.

### 5. Dependency Inversion Principle(DIP, 의존 역전 원칙)
**변화하기 쉬운 것보다 변화하기 어려운 것에 의존해야 한다는 원칙이다.**

객체 지향 관점에서 구체화 된 클래스는 변화하기 쉽고, 인터페이스나 추상클래스가 변화하기 어려운 클래스가 된다. `DIP` 원칙을 만족하고자 한다면, 구체적인 클래스보다 인터페이스나 추상클래스와 관계를 맺어야 한다는 것을 의미한다.

---

> 출처 - [SOLID 원칙 - Programming Note](https://dev-momo.tistory.com/entry/SOLID-%EC%9B%90%EC%B9%99)
