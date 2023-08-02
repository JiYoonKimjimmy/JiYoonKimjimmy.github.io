# Jimmyberg's Blog
## Purpose
* Github.io 를 이용하여 Markdown Blog 구축
* MD 파일 Commit 완료하면 바로 Blog 게시글 Posting

## Blog Dev Environment
* Ruby
* Jekyll

#### Jekyll Blog 선택한 이유?
* Local 에서의 Test 환경 제공
* Github Commit 으로 Blog posting 가능
* **Markdown** 을 적극 활용할 수 있다는 장점

#### 설치 과정
##### MAC OSX
1. `Homebrew` 설치
```bash
$ /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```
2. `rbenv` 설치 및 설정
*  rbenv? ruby 관리 package
```bash
# Install rbenv
$ brew install rbenv

# Set up rbenv integration with shell
$ rbenv init

# check rbenv installation
$ curl -fsSL https://github.com/rbenv/rbenv-installer/raw/master/bin/rbenv-doctor | bash
```
3. ruby 설치 from `rbenv`
* jekyll 에서는 시스템 ruby 보다는 `rbenv` 의 ruby 설치 권장
```bash
# .bash_profile 환경 설정
$ echo 'if which rbenv > /dev/null; then eval "$(rbenv init -)"; fi' >> ~/.bash_profile
$ source ~/.bash_profile

# 21.02.08 기준 jekyll 공식 사이트 최신 버전
$ rbenv install 2.7.2
$ rbenv global 2.7.2
$ ruby -v
# ruby 2.7.2p137 (2020-10-01 revision 5445e04352)
```
4. `Jekyll` 설치 후 실행
```bash
# github-pages : Github 에서 사용하는 Jekyll 관련 의존성 package 지원
$ gem install jekyll bundler github-pages

# Jekyll Project 생성
$ jekyll new [project directory]

# Run jekyll
$ cd [project directory]
$ bundle exec jekyll serve
```
5. `Jekyll` 정적 사이트 이동
```http
http://localhost:4000
```

##### Windows
> [Jekyll 공식 홈페이지 참고](https://jekyllrb-ko.github.io/docs/installation/windows/)
1. `RubyInstaller for Windows` 설치
* [rubyinstaller.org](https://rubyinstaller.org/)
2. `Jekyll bundler` 설치
```bash
$ gem install jekyll bundler
```

#### Jekyll Theme
**[Zolan Theme](http://jekyllthemes.org/themes/zolan/)**
![Zolan Theme home](/images/zolan-jekyll-theme.png)

```bash
# 1. Zolan theme github clone
$ git clone https://github.com/artemsheludko/zolan.git
# 2. Bundle update
$ bundle update
# 3. jekyll bundle execute
$ bundle exec jekyll serve
```

---

## etc.
### ~/.bash_profile 자동 실행
* `~/.zshrc` file 추가
```bash
#!/bin/#!/usr/bin/env bash

if [ -f ~/.bash_profile ]; then
        . ~/.bash_profile
fi
```

### ATOM Editor `Snippets` 설정
- `ATOM` 의 자동 완성 추가하는 설정

#### `snippets.cson` 수정
```cson
'.text.md':
  'Jekyll Code Block':
    'prefix': 'codeblock'
    'body': '{% highlight $1 %}$2\n{% endhighlight %}'
  'Start Jekyll Code Block':
    'prefix': 'startcodeblock'
    'body': '{% highlight $1 %}'
  'End Jekyll Code Block':
    'prefix': 'endcodeblock'
    'body': '{% endhighlight %}'
  'br Tag Element':
    'prefix': 'br'
    'body': '<br>\n'
```

---

## Trouble Shooting 😈

### 🔥어느순간 `jekyll` 프로젝트 빌드가 안될 때🔥
- `Ruby` 나 `Jekyll` 시스템 자체를 오랜 시간 업데이트를 하지 않는다면, 프로젝트가 정상적으로 빌드되지 않을 수도 있다.
- 그럴 땐, 아래와 같은 방법을 수행하여 해결할 수 있었다.

#### `Ruby` 업데이트

```bash
$ brew update
$ brew upgrade ruby
```

#### `Jekyll` 업데이트

```bash
$ gem update jekyllrb
$ bundle update
$ bundle install
```

#### Version 업데이트로 인한 새로운 문제
- 위와 같이 모두 업데이트를 했더니 프로젝트는 정상적으로 빌드되어 구동이 되었지만, 아래와 같은 에러를 남긴다.
- `Sass` 컴파일러 자체적으로 `Deprecated` 함수가 있어서 발생하는 것 같다.
- 이는 프로젝트 내의 `Sass` 코드를 수정하던지, 아니면.. 에러 로그를 무시하는 방법이 있다.

```bash
...
Deprecation Warning: Using / for division outside of calc() is deprecated and will be removed in Dart Sass 2.0.0.

Recommendation: math.div($i, $columns) or calc($i / $columns)

More info and automated migrator: https://sass-lang.com/d/slash-div

   ╷
77 │         margin-left: percentage( $i / $columns );
   │                                  ^^^^^^^^^^^^^
   ╵
    1-tools/_grid.scss 77:34  @import
    - 61:9                    root stylesheet
Warning: 7 repetitive deprecation warnings omitted.
...
```

---

## Ref.
* [Jekyllrb.com - Jekyll Official site installation document](https://jekyllrb.com/docs/installation/macos/)
* [ogaeng.com - Jekyll 블로그 만들기(1) - 설치하기](https://ogaeng.com/jekyll-blog-install/)
* [lamarr.dev - Jekyll이란? 맥북에 설치하기](https://lamarr.dev/jekyll/2020/03/03/01.html)
