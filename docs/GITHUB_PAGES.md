# GitHub Pages로 마케팅·지원 URL 게시하기

이 폴더(`docs/`)는 Google Play Console의 **스토어 등록정보 URL**과 **개인정보 처리방침 URL**에 넣을 수 있는 정적 웹 페이지입니다.

## 게시 후 사용할 URL 예시

저장소 이름이 `ListenToGospel-Android`이고 GitHub 사용자명이 `mrnoh99`일 때:

| 용도 | URL |
|------|-----|
| Marketing URL | `https://mrnoh99.github.io/ListenToGospel-Android/` |
| Support URL | `https://mrnoh99.github.io/ListenToGospel-Android/support.html` |
| 개인정보 처리방침 | `https://mrnoh99.github.io/ListenToGospel-Android/privacy.html` |

(조직 페이지나 커스텀 도메인을 쓰면 위 주소를 그에 맞게 바꿉니다.)

## `404`가 나올 때 (마케팅 URL이 안 열릴 때)

저장소에 `docs/`가 있어도 **GitHub Pages 게시를 켜지 않으면** `https://mrnoh99.github.io/ListenToGospel-Android/` 는 404입니다.

1. [저장소 Settings → Pages](https://github.com/mrnoh99/ListenToGospel-Android/settings/pages) 로 이동합니다.
2. **Build and deployment** 의 **Source**에서 **Deploy from a branch** 를 선택합니다.
3. **Branch**: `gh-pages` / **Folder**: `/ (root)` → **Save**
4. **Actions** 탭에서 **Deploy GitHub Pages** 워크플로가 성공했는지 확인합니다. 성공 후 1~2분 뒤에 열릴 수 있습니다.

## GitHub에서 설정 (방법 1: `gh-pages` 브랜치 — 권장)

`.github/workflows/deploy-github-pages.yml` 이 **main**에 푸시될 때마다 `docs/` 가 `gh-pages` 브랜치로 올라갑니다.

1. **Settings** → **Pages**
2. **Source**: *Deploy from a branch*
3. **Branch**: `gh-pages` / **Folder**: `/ (root)` → **Save**
4. **Actions** 탭에서 **Deploy GitHub Pages** 가 초록색으로 완료되는지 확인합니다.

## GitHub에서 설정 (방법 2: main 브랜치의 `/docs` 직접 게시)

워크플로 없이 브랜치에서 바로 게시할 수도 있습니다.

1. **Settings** → **Pages**
2. **Source**: *Deploy from a branch*
3. **Branch**: `main` / **Folder**: `/docs` → **Save**

방법 1과 2는 **동시에 쓰지 말고** 하나만 선택합니다.

## 페이지 구성

| 파일 | Play Console 용도 |
|------|-------------------|
| `index.html` | 마케팅 URL (앱 소개) |
| `support.html` | 지원 URL |
| `privacy.html` | 개인정보 처리방침 URL |
| `accessibility.html` | TalkBack 안내 (선택, 스토어 설명에 링크 가능) |

## 수정할 항목

- `index.html`: Play Store 출시 후 `id=njs.listentogospel` 링크가 열리는지 확인
- 지원 이메일: `jsnoh2010@gmail.com`
- 앱 표기명: **복음서듣기**

## 참고

- `.nojekyll` 파일은 Jekyll 없이 정적 HTML만 서빙할 때 사용합니다.
- 저장소가 **비공개**여도 GitHub Pages 무료 플랜에서는 공개 사이트 정책을 확인하세요.
