---
layout: post
title : Git Merge vs Rebase
date  : 2024-01-06
image : git-merge-vs-rebase.png
tags  : git merge rebase squash
---

## Git Branch `Merge vs Rebase`

많은 회사에서 프로젝트 형상 관리를 위해 `Git` 을 많이 사용하고 있을 것이다.

그리고 하나의 프로젝트를 여러 명의 개발자들이 공동 작업하는 경우도 많이 하다보면, `Branch 브랜치` 관리에 대한 전략을 수립하여 진행하게 된다.

기본적인 `Git Flow` 전략을 통해서 브랜치를 나누고, 병합하는 방식이지만, 
`Git` 에서 제공하는 `Merge` 외 브랜치를 `Rebase Merge` 방식과 `Squash Merge` 방식에 대해서 정리해보았다.

---

기능 개발을 위해 다음과 같이 `master` 브랜치에서 `my-branch` 란 작업 브랜치를 분리하였다.

![Git Branch Checkout](/images/git-branch-checkout.jpg)

`my-branch` 에서 개발 진행 중 다음과 같이 `master` 브랜치에 새로운 다른 작업이 이미 `Merge` 된 상태인 경우가 있을 것이다.

![Git Another Branch Checkout](/images/git-another-branch-checkout.jpg)

이런 경우, `my-branch` 를 `master` 에 병합하기 위해서 단순히 `Merge` 를 사용할 수도 있지만, 조금 더 깔끔한 브랜치 관리를 위해 
`Squash Merge` 또는 `Rebase Merge` 에 대해서 살펴보자.

---

### Merge

`Merge` 만 하여 병합하는 경우를 먼저 살펴보면, 아래와 같이 이미 작업되어 병합된 `master` 브랜치가 유지되면서,
`my-branch` 는 다음 `commit(m)` 으로 병합되게 된다.

![Git Branch Merge](/images/git-branch-merge.jpg)

1. `my-branch` 브랜치에서 작업한 `commit(a, b, c)` 에 대한 `commit(m)` 을 생성한다.
2. `my-brnach` 브랜치의 `commit(m)` 이 `master` 브랜치로 추가된다.

위와 같은 방식은 `Recursive Merge` 라고도 한다.

`Merge` 방식은 아래와 같은 경우 적합하다.

- 두 브랜치 간의 변경 사항이 서로 **독립적**인 경우
- 병합 후 브랜치 `commit` **기록 유지**가 필요한 경우

{% highlight bash %}
 $ git checkout master
 $ git merge my-branch
{% endhighlight %}

---

### Squash Merge

`Squash Merge` 방식은 두 브랜치의 변경 사항을 하나의 `commit` 으로 병합하는 방식이며, 
아래와 같이 `my-branch` 의 `commit(a, b, c)` 는 하나의 `commit(abc)` 로 합쳐져 병합된다.

![Git Squash Branch Merge](/images/git-branch-squash-merge.jpg)

1. `my-branch` 브랜치에서 작업한 `commit(a, b, c)` 은 `commit(abc)` 으로 합쳐지고 버려진다.
2. `my-brnach` 브랜치의 `commit(abc)` 이 `master` 브랜치로 추가된다.

`Squash Merge` 방식은 아래와 같은 경우 적합하다.

- 두 브랜치의 변경 사항이 서로 **의존적**인 경우
- 병합 후 브랜치 `commit` **기록 단순화**가 필요한 경우

{% highlight bash %}
 $ git checkout master
 $ git merge --squash my-branch
 $ git commit -m "commit message"
{% endhighlight %}

---

### Rebase Merge

`Rebase Merge` 방식은 한 브랜치의 변경 사항을 다른 브랜치의 변경 사항 위에 `overwrite` 덧씌우는 방식이며,
아래와 같이 `my-branch` 의 `commit(a, b, c)` 는 `master` 브랜치의 현재 기준 다음으로 추가되어 병합된다.

![Git Rebase Branch Merge](/images/git-branch-rebase-merge.jpg)

1. `my-branch` 브랜치에서 작업한 `commit(a, b, c)` 은 그대로 `master` 브랜치의 `commit(a, b, c)` 으로 추가된다.

`Rebase Merge` 방식은 아래와 같은 경우 적합하다.

- 두 브랜치의 변경 사항이 서로 **의존적**인 경우
- 병합 후 브랜치 `commit` **기록 유지 & 단순화**가 필요한 경우

{% highlight bash %}
 $ git checkout my-branch
 $ git rebase master
 $ git checkout master
 $ git merge my-branch
{% endhighlight %}

---

### Merge vs. Squash Merge vs. Rebase Merge 비교

| 구분 | Merge | Squash Merge | Rebase Merge | 
| :---: | :---: | :---: | :---: |
| **변경 사항 연관성** | 독립적 | 의존적 | 의존적 |
| **커밋 기록 관리** | 유지 | 단순화 | 유지 & 단순화 |

#### Branch Merge 운용 방식

##### `feature > develop` 병합하는 경우, Squash Merge

기능 개발을 위한 `feature` 브랜치를 개발 완료 후 `develop` 브랜치로 병합할 때는,

 **`Squash Merge` 방식이 적합하다.**

동일한 기능에 대한 연관적인 작업 내용을 나눠서 `commit` 한 뒤, 해당 `commit` 은 하나의 기능을 위한 것이기 때문에
`Squash Merge` 를 통하여 하나의 `commit` 으로 `develop` 브랜치에 병합한다.

*단,* 기존 `feature` 브랜치에서 작업한 `commit` 기록이 사라질 수 있기 때문에 **마지막 커밋 메시지를 상세하게 잘 작성할 필요가 있다.**

##### `develop > main` 병합하는 경우, Rebase Merge

서로 다른 기능 개발을 한 브랜치가 쌓인 `develop` 브랜치를 `main` 브랜치로 병합할 때는,

 **`Rebase Merge` 방식이 적합하다.**

`develop` 브랜치의 작업된 `commit` 기록을 모두 남겨둔 채 `main` 브랜치로 병합해야 특정 기능에 대한 이슈 장애 발생에 대한 대응이 가능하기 때문이다.

---

#### 출처
- [[Git] Merge 이해하기 (Merge / Squash and Merge / Rebase and Merge)](https://im-developer.tistory.com/182)
- [Git의 다양한 브랜치 병합 방법 (Merge, Squash & Merge, Rebase & Merge)](https://hudi.blog/git-merge-squash-rebase/)
- [Git Rebase vs. Merge: A Complete Guide](https://www.simplilearn.com/git-rebase-vs-merge-article)
