#!/bin/bash
set -e
export JAVA_HOME=~/shadow_build/jdk17
export ANDROID_HOME=~/shadow_build/android-sdk
export ANDROID_SDK_ROOT=~/shadow_build/android-sdk
export PATH="$JAVA_HOME/bin:$PATH"
PROJ=/home/user/Doubao/chats/38445152083111682/ShadowBrowser
cd "$PROJ"
# 指向本机 SDK（不入库）
echo "sdk.dir=$ANDROID_HOME" > local.properties
GRADLE=~/shadow_build/gradle-8.7/bin/gradle
echo "=== gradle version ==="
"$GRADLE" --version | head -5
echo "=== building assembleDebug ==="
"$GRADLE" --no-daemon --stacktrace assembleDebug
echo "=== APK ==="
ls -la "$PROJ"/app/build/outputs/apk/debug/ 2>/dev/null
