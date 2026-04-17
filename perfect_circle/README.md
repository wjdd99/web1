# Perfect Circle

A minimalist Android game: draw a circle with one finger and get scored on how
round it is. Built with Flutter per `PRD_PerfectCircle.md`.

## Getting the APK

The repository includes a GitHub Actions workflow at
`.github/workflows/build-apk.yml` that builds a release APK on every push.

1. Push the `claude/android-game-apk-fpYC0` branch (already done).
2. Open the repository's **Actions** tab on GitHub.
3. Select the latest **Build Android APK** run.
4. Download the `perfect-circle-release-apk` artifact.
5. Transfer the `.apk` to your Android device and install (allow installation
   from unknown sources).

The APK is signed with Android's debug key, so it is installable without
configuring a release keystore. Replace the `signingConfig` in
`android/app/build.gradle` with your own keystore when you're ready to ship.

## Local build

```bash
cd perfect_circle
flutter pub get
flutter build apk --release
# APK at build/app/outputs/flutter-apk/app-release.apk
```

Requires Flutter 3.19+ and the Android SDK (compile SDK 34, min SDK 24).

## Gameplay

* Drag one finger to draw a closed circle.
* On release, the app computes:
  * centroid of your stroke,
  * mean radius,
  * roundness (standard deviation of radii / mean radius),
  * closure error (gap between start and end / mean radius).
* Score is `100 * exp(-9 * roundness) - closure_penalty`, clamped to 0..100.
* Best score is persisted with `shared_preferences`.

## Project layout

```
lib/
  main.dart
  core/
    constants/app_constants.dart
    theme/app_theme.dart
    utils/grade.dart
    services/
      storage_service.dart
      feedback_service.dart
  features/game/
    logic/circle_scorer.dart
    models/
      stroke_point.dart
      game_result.dart
    state/game_state.dart
    presentation/
      screens/
        home_screen.dart
        settings_screen.dart
      widgets/
        circle_painter.dart
        score_card.dart
test/
  circle_scorer_test.dart
android/
  app/...  (Android host)
```
