# ⏰ 벌떡 알람 (Android · Kotlin)

알람을 끄려면 **깨우기 미션**을 완료해야만 꺼지는 네이티브 안드로이드 알람 앱입니다.
잠금화면 위로 풀스크린 알람이 뜨고, Doze 모드에서도 정시에 울립니다.

## 새로운 컨셉의 미션 3종

| 미션 | 동작 | 사용 기술 |
|------|------|-----------|
| 🗣️ **AI 모닝 대화** | Claude가 음성으로 말을 걸고, 또렷하게 대화해야 꺼짐. 잠꼬대 수준이면 계속 캐물음 | `SpeechRecognizer`(STT) + `TextToSpeech`(TTS) + **Claude API** |
| 📷 **침대 탈출 사진 인증** | 첫 알람에 집안 장소(세면대·냉장고 등)를 기준으로 등록 → 다음부터 그 장소로 가서 같은 사진을 찍어야 꺼짐 | `CameraX` + average-hash(aHash) 이미지 매칭 |
| 🏃 **신체 각성** | 폰을 격하게 흔들어야 꺼짐(강도 조절 가능) | `SensorManager` 가속도 센서 |

## 알람 동작 구조

```
AlarmManager.setAlarmClock()   ← 정확/Doze 관통 예약
        │ (시각 도달)
        ▼
AlarmReceiver  ── 반복이면 다음 회차 재예약
        │
        ▼
AlarmService (포그라운드, mediaPlayback)
   ├─ 알람음 + 진동 + WakeLock
   └─ AlarmActivity (showWhenLocked / turnScreenOn) 풀스크린 미션
              │ (미션 완료)
              ▼
        AlarmService.stop()
```

- 재부팅 후 `BootReceiver`가 알람을 자동 복원합니다.
- 뒤로가기로 알람 화면을 빠져나갈 수 없어, **미션을 풀어야만** 꺼집니다.

## 빌드 방법

1. Android Studio(Koala 이상)에서 `android/` 폴더를 엽니다.
2. **Claude API 키 설정** (AI 모닝 대화 미션용):
   - `android/gradle.properties`의 `ANTHROPIC_API_KEY=` 에 키를 넣거나
   - 빌드 시 환경변수 `ANTHROPIC_API_KEY`를 주입합니다.
   - 키가 없으면 AI 대화는 **오프라인 모드(문장 따라 입력)** 로 자동 대체됩니다.
3. 실기기(또는 에뮬레이터)로 Run. 최소 SDK 26(Android 8.0).

> ⚠️ **보안 주의:** 앱에 API 키를 직접 내장하는 것은 안전하지 않습니다(디컴파일로 노출 가능).
> 개인용 전제로 단순화한 것이며, 배포한다면 **본인 백엔드에 프록시**를 두고 그
> 엔드포인트를 호출하도록 `ClaudeClient`를 바꾸세요.

## 권한

| 권한 | 용도 |
|------|------|
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | 정확한 시각 알람 |
| `POST_NOTIFICATIONS` | 알람 알림(Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | 재부팅 후 알람 복원 |
| `CAMERA` | 사진 인증 미션 |
| `RECORD_AUDIO` | AI 대화 음성 인식 |
| `ACTIVITY_RECOGNITION` | 신체 각성(센서) |
| `INTERNET` | Claude API 호출 |

## 사용 기술

Kotlin · Jetpack Compose(Material 3) · AlarmManager · Foreground Service ·
CameraX · SensorManager · SpeechRecognizer/TextToSpeech · OkHttp ·
kotlinx.serialization · Anthropic Messages API(`claude-opus-4-8`)

## 주요 파일

```
app/src/main/java/com/beolddeok/alarm/
├─ data/            Alarm, AlarmStore, MissionType
├─ alarm/           AlarmScheduler, AlarmReceiver, BootReceiver, AlarmService
├─ ai/              ClaudeClient (Anthropic Messages API)
├─ util/            ImageHash (aHash)
└─ ui/
   ├─ MainActivity, EditAlarmDialog        알람 목록·편집
   └─ ring/
      ├─ AlarmActivity                     풀스크린 알람 화면
      └─ missions/                         AI대화 · 사진 · 신체 각성
```
