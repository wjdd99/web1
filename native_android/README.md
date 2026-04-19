# Perfect Circle – Native Android APK

A minimal, native Android implementation of the Perfect Circle game. This
project exists because the default Flutter build path requires Google's Maven
repo and SDK download server, which are not reachable from this sandbox. The
native variant here uses only tools obtainable from Maven Central, npm, and
GitHub Releases, so the APK can be built offline-relative-to-Google.

The built APK lives at `../dist/perfect-circle.apk` (checked in).

## What's in it

* `src/com/perfectcircle/app/` – three small Java classes:
  * `Scorer` – pure-Java port of the circle-fit scorer (centroid, mean radius,
    σ, closure error → 0..100 score).
  * `GameView` – custom `View` that captures touch, draws the stroke, and
    renders the result card.
  * `MainActivity` – fullscreen activity that hosts the view.
* `AndroidManifest.xml` – minSdk 21, targetSdk 30, portrait lock.
* `res/` – launcher icons (five mipmap buckets) and strings.
* `build.sh` – builds and signs the APK without needing the Google Android SDK.

## Build

```bash
apt-get install -y openjdk-8-jdk-headless  # dx 1.7 needs Java ≤6 bytecode
./build.sh
# produces ../dist/perfect-circle.apk
```

### Tooling provenance

| Tool | Source | Purpose |
|------|--------|---------|
| `android.jar` (API 30) | `raw.githubusercontent.com/Sable/android-platforms` | Compile-time classpath |
| `dx-1.7.jar` | Maven Central (`com.google.android.tools:dx:1.7`) | `.class` → `classes.dex` |
| `aapt` (linux) | `aaptjs` npm package | Resource compile + APK packaging |
| `uber-apk-signer-1.3.0.jar` | GitHub release (patrickfav) | zipalign + v1/v2/v3 signing |
| `javac`, `keytool` | JDK 8 (`openjdk-8-jdk-headless`) | Compile + generate debug keystore |

None of these hosts are `dl.google.com` or `maven.google.com`.

## Install

Transfer `dist/perfect-circle.apk` to an Android device (USB, adb push, email,
Google Drive, etc.) and tap to install. You may need to allow installation
from unknown sources the first time.

Signed with a generated debug key; swap in a release keystore in `build.sh`
for production use.

## Caveats

* Uses legacy `aapt` (v1) and `dx` instead of `aapt2` + `d8`. APK is smaller
  and fine for API 21–30 but not optimized with R8.
* No AndroidX / Material Components – the UI is hand-drawn on a Canvas.
* The built APK is ~21 KB.
