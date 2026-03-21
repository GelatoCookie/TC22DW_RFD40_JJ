#!/bin/bash
# Automated build and deployment script for Zebra RFID Sample App

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.zebra.rfid.demo.sdksample"

# Build the app
chmod +x "$PROJECT_DIR/gradlew"
"$PROJECT_DIR/gradlew" assembleDebug

# Deploy APK to connected device
adb install -r "$APK_PATH"

# Launch the app
adb shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1

echo "Build, install, and launch complete."
