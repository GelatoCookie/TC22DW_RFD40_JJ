#!/bin/bash
# Automated clean, build, deploy, and run script for Zebra RFID Sample App

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.zebra.rfid.demo.sdksample"
MAIN_ACTIVITY=".MainActivity"

echo "========================================="
echo " Zebra RFID Sample App - Build & Deploy"
echo "========================================="

# --- Step 1: Check for connected device ---
echo ""
echo "[1/5] Checking for connected device..."
if ! command -v adb &> /dev/null; then
    echo "ERROR: adb not found. Please add Android SDK platform-tools to your PATH."
    exit 1
fi

DEVICE_COUNT=$(adb devices | grep -cw "device")
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "ERROR: No device connected. Connect a device via USB or start an emulator."
    exit 1
fi
echo "  Device found."

# --- Step 2: Clean ---
echo ""
echo "[2/5] Cleaning project..."
chmod +x "$PROJECT_DIR/gradlew"
"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" clean
echo "  Clean complete."

# --- Step 3: Build ---
echo ""
echo "[3/5] Building debug APK..."
"$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" assembleDebug
if [ ! -f "$APK_PATH" ]; then
    echo "ERROR: APK not found at $APK_PATH"
    exit 1
fi
echo "  Build complete."

# --- Step 4: Deploy ---
echo ""
echo "[4/5] Installing APK to device..."
adb install -r "$APK_PATH"
echo "  Install complete."

# --- Step 5: Run ---
echo ""
echo "[5/5] Launching app..."
adb shell am start -n "$PACKAGE_NAME/$PACKAGE_NAME$MAIN_ACTIVITY"
echo "  App launched."

echo ""
echo "========================================="
echo " Done! App is running on device."
echo "========================================="
