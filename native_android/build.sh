#!/usr/bin/env bash
# Builds a release-signed Perfect Circle APK using only tools that can be
# fetched from Maven Central / GitHub / npm (no dl.google.com / maven.google.com).
#
# Usage: ./build.sh
# Output: dist/perfect-circle.apk

set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

TOOLS="build/tools"
CLASSES="build/classes"
APKDIR="build/apk"
DIST="$ROOT/../dist"

mkdir -p "$TOOLS" "$CLASSES" "$APKDIR" "$DIST"

# --- Find JDK 8 (required: dx 1.7 can't parse Java >1.6 bytecode) ---
if [[ -z "${JAVA8:-}" ]]; then
    for cand in /usr/lib/jvm/java-8-openjdk-amd64 /usr/lib/jvm/java-1.8.0-openjdk-amd64; do
        if [[ -x "$cand/bin/javac" ]]; then JAVA8="$cand"; break; fi
    done
fi
if [[ -z "${JAVA8:-}" || ! -x "$JAVA8/bin/javac" ]]; then
    echo "Need JDK 8. apt-get install openjdk-8-jdk-headless, or set JAVA8=/path/to/jdk8" >&2
    exit 1
fi
JAVAC="$JAVA8/bin/javac"
JAVA="$JAVA8/bin/java"
KEYTOOL="$JAVA8/bin/keytool"

# --- Fetch tools if absent ---
if [[ ! -f "$TOOLS/android.jar" ]]; then
    echo ">> downloading android.jar (API 30)"
    curl -sL --max-time 120 -o "$TOOLS/android.jar" \
        "https://raw.githubusercontent.com/Sable/android-platforms/master/android-30/android.jar"
fi
if [[ ! -f "$TOOLS/dx.jar" ]]; then
    echo ">> downloading dx.jar"
    curl -sL --max-time 120 -o "$TOOLS/dx.jar" \
        "https://repo1.maven.org/maven2/com/google/android/tools/dx/1.7/dx-1.7.jar"
fi
if [[ ! -f "$TOOLS/uber-apk-signer.jar" ]]; then
    echo ">> downloading uber-apk-signer.jar"
    curl -sL --max-time 120 -o "$TOOLS/uber-apk-signer.jar" \
        "https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar"
fi
if [[ ! -x "$TOOLS/aapt" ]]; then
    echo ">> installing aapt via npm (aaptjs)"
    TMP="$(mktemp -d)"
    (cd "$TMP" && npm init -y >/dev/null && npm install --silent aaptjs)
    cp "$TMP/node_modules/aaptjs/bin/linux/aapt" "$TOOLS/aapt"
    chmod +x "$TOOLS/aapt"
    rm -rf "$TMP"
fi

# --- Compile Java (target 1.6 so dx 1.7 can read the bytecode) ---
echo ">> compiling Java sources"
rm -rf "$CLASSES"
mkdir -p "$CLASSES"
"$JAVAC" -encoding UTF-8 -source 1.6 -target 1.6 \
    -bootclasspath "$TOOLS/android.jar" \
    -d "$CLASSES" \
    src/com/perfectcircle/app/*.java

# --- .class -> classes.dex ---
echo ">> dex-ing"
"$JAVA" -cp "$TOOLS/dx.jar" com.android.dx.command.Main --dex \
    --output="build/classes.dex" "$CLASSES"

# --- aapt package ---
echo ">> packaging APK"
rm -f "$APKDIR/app.unsigned.apk"
"$TOOLS/aapt" package -f -M AndroidManifest.xml -S res \
    -I "$TOOLS/android.jar" \
    -F "$APKDIR/app.unsigned.apk"

# aapt `add` needs classes.dex in the cwd under that name
pushd build >/dev/null
"../$TOOLS/aapt" add "apk/app.unsigned.apk" "classes.dex" >/dev/null
popd >/dev/null

# --- Keystore ---
if [[ ! -f "build/debug.keystore" ]]; then
    echo ">> creating debug keystore"
    "$KEYTOOL" -genkeypair -v \
        -keystore build/debug.keystore \
        -storepass android -keypass android \
        -alias androiddebugkey \
        -dname "CN=Android Debug,O=Android,C=US" \
        -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1
fi

# --- Sign ---
echo ">> signing"
cp "$APKDIR/app.unsigned.apk" "$APKDIR/perfect-circle.apk"
"$JAVA" -jar "$TOOLS/uber-apk-signer.jar" \
    --apks "$APKDIR/perfect-circle.apk" \
    --ks build/debug.keystore \
    --ksAlias androiddebugkey \
    --ksPass android \
    --ksKeyPass android \
    --allowResign --overwrite >/dev/null

cp "$APKDIR/perfect-circle.apk" "$DIST/perfect-circle.apk"
echo ">> done: $DIST/perfect-circle.apk ($(stat -c%s "$DIST/perfect-circle.apk") bytes)"
