# Perfect Circle – Android Game PRD

> Reference PRD transcribed from the task specification image.

## 1. Project Overview (L1)
* **Product Name:** Perfect Circle
* **Perfect Code:** PRD-PC-001 (internal)
* **Target:** A mobile casual game where the player draws a circle and gets
  scored on how circular the stroke is.
* **Dev Timeline:** MVP in 7 days.

## 2. Target Users
* Casual mobile gamers who enjoy 1-tap / skill-testing loops.
* All ages – minimal UI, no text-heavy flows.

## 3. Core Value Proposition
* Frictionless: open → draw → score → share.
* Numerical feedback rewards mastery.

## 4. Success Metrics (KPI)
| KPI            | Target (Day 30) |
|----------------|-----------------|
| D1 Retention   | ≥ 35%           |
| D7 Retention   | ≥ 12%           |
| Avg Session    | ≥ 90s           |
| Play/user/day  | ≥ 6             |
| Play-Now rate  | ≥ 0.4           |

## 5. Tech Stack (L2)
* **Framework:** Flutter 3.19+ (Dart 3).
* **State:** Riverpod.
* **Persistence:** `shared_preferences` for best score & settings.
* **Feedback:** `vibration`, `audioplayers`.
* **Platforms:** Android (min SDK 24, target SDK 34). iOS support optional.

## 6. Core Features
| ID | Feature          | Priority | MVP |
|----|------------------|----------|-----|
| F1 | Draw & score     | P0       | ✅  |
| F2 | Best score save  | P0       | ✅  |
| F3 | Retry loop       | P0       | ✅  |
| F4 | Settings         | P1       | ✅  |
| F5 | Haptic/sound fx  | P1       | ✅  |

## 7. Feature Details (F1)
1. User places finger on canvas → `GamePhase.drawing`.
2. Points appended on move.
3. On lift: scorer computes centroid, mean radius, σ, closure error.
4. Score 0..100 displayed with grade (S/A/B/C/D/F).
5. Best score persisted if beaten; play count incremented.

## 8. Edge Cases
* Too few samples (<30) → invalid, prompt "draw longer circle".
* Tiny radius (<30 px) → invalid.
* Partial arcs penalised via closure error.

## 9. Data Flow
```
UI gesture → GameController → CircleScorer → GameResult
                ↓
          StorageService ← best score
                ↓
          FeedbackService → haptics
```

## 10. Acceptance Criteria
* [x] APK builds and installs on Android 7.0+.
* [x] Draw gesture produces a score in <100ms.
* [x] Best score persists across launches.
* [x] Settings persist across launches.
* [x] No crashes on tiny/empty strokes.
