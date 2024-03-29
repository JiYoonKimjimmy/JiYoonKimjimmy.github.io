---
layout: post
title : OSX command setting
date  : 2021-02-14
image : 2021-02-14-osx-command-setting.jpg
tags  : OSX
---

## OSX command 설정 관련 정리
### iTerm2 설치
[iTerm2 Download](https://iterm2.com/index.html)
* Custom 단축키 및 Script, 화면 분할 가능한 Mac terminal Tool

#### iTerm2 Theme 설정
* [iTerm2 Color Theme Github](https://github.com/mbadolato/iTerm2-Color-Schemes) 에서 다양한 Theme 선택 가능
* 원하는 Theme github 에서 Download!
* iTerm2 설정 > Profiles > Colors > Color Presets... 에서 Download 한 `.itermcolors` 파일 Import!

---

### zsh 설치 및 설정
* zsh ? Mac 의 기본 Shell Script
  * 다양한 Plugin 제공

#### 설치
###### 1. brew 설치 ([https://brew.sh/index_ko](https://brew.sh/index_ko))

{% highlight bash %}
$ /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
{% endhighlight %}

###### 2. `zsh` & `oh-my-zsh` 설치
* oh-my-zsh : zsh 설정 관리 Framework. git, homebrew, python 등 많은 Plugin 및 Theme 지원

{% highlight bash %}
# zsh 설치
$ brew install zsh
# oh-my-zsh 설치
$ sh -c "$(curl -fsSL https://raw.github.com/robbyrussell/oh-my-zsh/master/tools/install.sh)"
{% endhighlight %}

###### 3. `zsh` Theme 변경
* dracula theme 설치 ([https://draculatheme.com/zsh](https://draculatheme.com/zsh))

#### `zsh` Plugins 설치
1. `~/.oh-my-zsh/plugins` directory 하위에 원하는 plugin git repository 를 clone
2. `~/.zshrc` 의 `plugins=()` 에 Plugin 추가

##### zsh-syntax-highlighting
* Command highlighting 처리 Plugin

{% highlight bash %}
$ git clone https://github.com/zsh-users/zsh-syntax-highlighting.git
{% endhighlight %}

##### zsh-autosuggestions
* Command 자동 완성/제안 처리 Plugin

{% highlight bash %}
$ git clone https://github.com/zsh-users/zsh-autosuggestions.git
{% endhighlight %}

#### zsh Custom command 만들기
* 원하는 directory 이동 후, `ls -al` 실행

{% highlight bash %}
# ~/.zshrc 수정
function ex() {
cd ~/go_to_directory
ls -al
}
{% endhighlight %}

* alias `ll` 실행 변경

{% highlight bash %}
# ~/.zshrc 수정
alias ll="la -al"
{% endhighlight %}

---

### `vim` editor 설정
#### `vim` color scheme 적용
###### 1. `awesome-vim-colorschemes` git clone 하기

{% highlight bash %}
$ cd ~/Download/awesome-vim-colorschemes
$ git clone https://github.com/rafi/awesome-vim-colorschemes
{% endhighlight %}

###### 2. `~/.vim` directory 생성, clone repository 의 `colors` directory 이동

{% highlight bash %}
$ mkdir ~/.vim
$ cp ~/Download/awesome-vim-colorschemes/colors ~/.vim/
{% endhighlight %}

###### 3. `~/.vimrc` file 추가 생성

{% highlight bash %}
\" Syntax Highlighting
if has("syntax")
    syntax on
endif
set autoindent
set cindent
set nu
# 원하는 color 지정
colo jellybeans
set laststatus=2
set statusline=\ %<%l:%v\ [%P]%=%a\ %h%m%r\ %F\
{% endhighlight %}

---

#### ETC

##### Mac `dock` 사용중인 앱만 표시하는 방법
- `terminal` 에서 아래 명령어 입력

{% highlight bash %}
defaults write com.apple.dock static-only -bool true; killall Dock
# 원상복구
defaults write com.apple.dock static-only -bool false; killall Dock
{% endhighlight %}
