# OSX command setting
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
1. [brew 설치](https://brew.sh/index_ko)
```bash
$ /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

2. `zsh` & `oh-my-zsh` 설치
* oh-my-zsh : zsh 설정 관리 Framework. git, homebrew, python 등 많은 Plugin 및 Theme 지
```bash
# zsh 설치
$ brew install zsh
# oh-my-zsh 설욕
$ sh -c "$(curl -fsSL https://raw.github.com/robbyrussell/oh-my-zsh/master/tools/install.sh)"
```

3. `zsh` Theme 변경
* [dracula theme 설치](https://draculatheme.com/zsh)

#### `zsh` Plugins
1. `~/.oh-my-zsh/plugins` directory 하위에 원하는 plugin git repository 를 clone
2. `~/.zshrc` 의 `plugins=()` 에 Plugin 추가

##### zsh-syntax-highlighting
* Command highlighting 처리 Plugin
```bash
$ git clone https://github.com/zsh-users/zsh-syntax-highlighting.git
```

##### zsh-autosuggestions
* Command 자동 완성/제안 처리 Plugin
```bash
$ git clone https://github.com/zsh-users/zsh-autosuggestions.git
```

---

### `vim` editor 설정
#### `vim` color scheme 적용
1. `awesome-vim-colorschemes` git clone 하기
```bash
$ cd ~/Download/awesome-vim-colorschemes
$ git clone https://github.com/rafi/awesome-vim-colorschemes
```

2. `~/.vim` directory 생성, clone repository 의 `colors` directory 이동
```bash
$ mkdir ~/.vim
$ cp ~/Download/awesome-vim-colorschemes/colors ~/.vim/
```

3. `~/.vimrc` file 생성
```bash
$ vi ~/.vimrc

# .vimrc
" Syntax Highlighting
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
```
