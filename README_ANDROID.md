# 복음서듣기 Android

가톨릭 한국어 성경 오디오 앱 (마태오·마르코·루카·요한복음서) Android 버전

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **오디오**: MediaPlayer + Foreground Service
- **상태 관리**: ViewModel + StateFlow
- **최소 Android**: API 26 (Android 8.0) · **타겟**: API 35 (Android 15)

## 기능

- 4복음서 선택 그리드 → 장 목록 → 재생
- 백그라운드 오디오 재생 (잠금화면 알림)
- 장 완료 시 다음 장 자동 재생
- 수면 타이머 (30/60/90/120분)
- 마지막 위치 자동 저장 및 이어듣기 제안
- 오디오 포커스 처리 (전화/알림 인터럽트)

## 프로젝트 설정

### 1. 저장소 클론
```bash
git clone -b android-studio https://github.com/mrnoh99/ListenToGospel.git ListenToGospel-Android
cd ListenToGospel-Android
```

### 2. 오디오 파일 복사
오디오 파일(89개 M4A, 약 339MB)은 크기로 인해 git에 포함되지 않습니다.  
iOS 프로젝트와 같은 경로에 클론했다면:
```bash
chmod +x copy_audio_assets.sh
./copy_audio_assets.sh
# 다른 경로라면:
./copy_audio_assets.sh /path/to/ListenToGospel/ListenToGospel/AudioFiles
```

### 3. Android Studio에서 열기
이 폴더(`settings.gradle.kts`가 있는 루트)를 Android Studio에서 Open합니다.

### 4. 빌드 및 실행
Gradle Sync 후 실행 버튼을 누르면 됩니다.

### 5. Signed AAB (Google Play 업로드)

**1) iCloud 업로드 키 사용 (권장)**

Keystore: `C:\Users\jsnoh\iCloudDrive\AppDevelop\KeyStoreFile\listentogospel-release.jks`

```powershell
cd C:\Users\jsnoh\StudioProjects\ListenToGospel-Android
.\use_listentogospel_keystore.cmd
```

(PowerShell에서는 현재 폴더의 명령에 `.\` 접두사가 필요합니다.)

비밀번호 입력 → `keystore.properties` 생성 → signed AAB 빌드까지 진행합니다.

**또는** 키를 새로 만들 때: `scripts\create_release_keystore.ps1`

**2) Signed AAB만 다시 빌드**

```powershell
.\build_release_aab.cmd
```

또는 Android Studio: **Build → Generate Signed App Bundle / APK…** → Android App Bundle.

**결과 파일**

`app\build\outputs\bundle\release\app-release.aab`

Play Console → **테스트 및 출시** → 트랙 선택 → **새 버전** → App bundle 업로드.

> `keystore.properties`가 없으면 release 서명이 적용되지 않을 수 있습니다. Play 업로드 전에 반드시 업로드 키로 빌드하세요.

### 장 넘김(자동 재생) 빠른 테스트
전체 장 오디오는 길어서, **약 10초짜리 테스트 클립**으로 바꿔 연속 재생을 검증할 수 있습니다. 원본은 프로젝트 루트 `.audio-backup/`에 보관됩니다 (git 제외).

**Windows (PowerShell, ffmpeg 필요):**

먼저 프로젝트 루트로 이동합니다 (`app/`, `scripts/` 폴더가 보이는 위치).

```cmd
cd C:\Users\jsnoh\StudioProjects\ListenToGospel-Android
winget install Gyan.FFmpeg
.\audio_test_mode.cmd -Enable
# 앱 실행 → 한 장이 끝나면 다음 장으로 넘어가는지 확인
.\audio_test_mode.cmd -Restore
```

PowerShell에서 `실행 정책` 오류가 나면 **`.cmd`를 사용**하세요 (정책 변경 불필요).

PowerShell로 직접 실행할 때:
```powershell
powershell -ExecutionPolicy Bypass -File ".\scripts\audio_test_mode.ps1" -Enable
```

**macOS / Linux:**
```bash
./scripts/audio_test_mode.sh enable
./scripts/audio_test_mode.sh restore
```

옵션: `-Seconds 8` (PowerShell) 또는 `./scripts/audio_test_mode.sh enable 8`  
상태 확인: `-Status` / `./scripts/audio_test_mode.sh status`

## 프로젝트 구조

```
app/src/main/
├── java/njs/listentogospel/
│   ├── ListenToGospelApp.kt       # Application class
│   ├── MainActivity.kt            # 단일 Activity
│   ├── model/Bible.kt             # Gospel enum, BibleChapter
│   ├── data/PlaybackPersistence.kt # SharedPreferences 저장
│   ├── audio/AudioPlayer.kt       # MediaPlayer 관리
│   ├── service/PlaybackService.kt # Foreground Service (알림)
│   ├── viewmodel/BiblePlayerViewModel.kt
│   └── ui/
│       ├── MainScreen.kt
│       ├── theme/
│       └── components/
└── assets/AudioFiles/             # M4A 오디오 파일 (별도 복사 필요)
```
