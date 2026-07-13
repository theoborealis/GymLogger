#!/usr/bin/env bash
# Headless emulator control for GymLogger testing/profiling.
# Run inside the emulator shell:  nix-shell emulator.nix --run './scripts/emulator.sh <cmd>'
#
#   boot      create the AVD if needed and run the emulator in the FOREGROUND
#             (intended to be launched as a long-lived background task)
#   wait      block until the device finishes booting, then unlock the keyguard
#   install   (re)install the debug APK
#   launch    start the main activity
#   shot NAME screencap to .emulator/shots/NAME.png
#   jank      reset gfxinfo, leave it to accumulate (use 'gfxinfo' to read)
#   gfxinfo   print the frame-timing / jank summary
#   down      kill the emulator
set -euo pipefail
cd "$(dirname "$0")/.."

AVD_NAME="${AVD_NAME:-gymlogger}"
SYS_IMAGE="system-images;android-34;google_apis;x86_64"
APK="app/build/outputs/apk/debug/app-debug.apk"
PKG="com.theob.gymlogger"
ACT="$PKG/.MainActivity"
EMU_PORT="${EMU_PORT:-5554}"
SERIAL="emulator-$EMU_PORT"
SHOT_DIR=".emulator/shots"
ADB="$ANDROID_HOME/platform-tools/adb"

ensure_avd() {
  mkdir -p "$ANDROID_AVD_HOME" "$ANDROID_USER_HOME" "$SHOT_DIR"
  if ! avdmanager list avd 2>/dev/null | grep -q "Name: $AVD_NAME"; then
    echo "Creating AVD '$AVD_NAME' ..."
    if ! (echo no | avdmanager create avd -n "$AVD_NAME" -k "$SYS_IMAGE" -d pixel_6 --force); then
      echo no | avdmanager create avd -n "$AVD_NAME" -k "$SYS_IMAGE" --force
    fi
  fi
}

boot() {
  "$ADB" kill-server >/dev/null 2>&1 || true
  ensure_avd
  echo "Starting emulator (headless, KVM)..."
  exec emulator -avd "$AVD_NAME" -port "$EMU_PORT" \
    -no-window -no-audio -no-boot-anim -no-snapshot \
    -gpu "${GPU_MODE:-swiftshader_indirect}" -memory 2048 \
    -camera-back none -camera-front none
}

wait_boot() {
  "$ADB" -s "$SERIAL" wait-for-device
  for _ in $(seq 1 150); do
    [ "$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && {
      "$ADB" -s "$SERIAL" shell input keyevent 82 >/dev/null 2>&1 || true
      "$ADB" -s "$SERIAL" shell wm dismiss-keyguard >/dev/null 2>&1 || true
      echo "Device booted."; return 0
    }
    sleep 2
  done
  echo "Boot timed out"; return 1
}

case "${1:-}" in
  boot)    boot ;;
  wait)    wait_boot ;;
  install) "$ADB" -s "$SERIAL" install -r "$APK" ;;
  launch)  "$ADB" -s "$SERIAL" shell am start -n "$ACT" >/dev/null && echo "launched" ;;
  shot)    mkdir -p "$SHOT_DIR"; "$ADB" -s "$SERIAL" exec-out screencap -p > "$SHOT_DIR/${2:-shot}.png"; echo "$SHOT_DIR/${2:-shot}.png" ;;
  jank)    "$ADB" -s "$SERIAL" shell dumpsys gfxinfo "$PKG" reset >/dev/null && echo "gfxinfo reset" ;;
  gfxinfo) "$ADB" -s "$SERIAL" shell dumpsys gfxinfo "$PKG" | grep -iE "Total frames|Janky frames|percentile|Number (Missed|High)|frames rendered" ;;
  tap)     "$ADB" -s "$SERIAL" shell input tap "$2" "$3" ;;
  text)    "$ADB" -s "$SERIAL" shell input text "$2" ;;
  size)    "$ADB" -s "$SERIAL" shell wm size ;;
  down)    "$ADB" -s "$SERIAL" emu kill 2>/dev/null || true; echo "killed" ;;
  *) echo "usage: $0 {boot|wait|install|launch|shot NAME|jank|gfxinfo|tap X Y|text STR|size|down}"; exit 1 ;;
esac
