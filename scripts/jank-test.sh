#!/usr/bin/env bash
# Reset app data, then drive a fixed, repeatable workload (sustained typing in
# the Label field = the auto-save path) and print the gfxinfo jank summary.
# Run with the APK under test already installed:
#   nix-shell emulator.nix --run './scripts/jank-test.sh "label"'
set -euo pipefail
cd "$(dirname "$0")/.."
ADB="$ANDROID_HOME/platform-tools/adb"
SERIAL="${SERIAL:-emulator-5554}"
PKG="com.theob.gymlogger"
TAG="${1:-run}"

a(){ "$ADB" -s "$SERIAL" "$@"; }

a shell pm clear "$PKG" >/dev/null            # identical starting state every run
a shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 3
a shell dumpsys gfxinfo "$PKG" reset >/dev/null
a shell input tap 540 580                     # focus the Label field
sleep 0.6
for i in 1 2 3 4 5 6 7 8; do a shell input text "Heavy-pull-day-stress-typing-$i"; done
sleep 1.5
echo "================ JANK: $TAG ================"
a shell dumpsys gfxinfo "$PKG" | grep -iE "Total frames|Janky frames|percentile|Missed Vsync|High input" | grep -vi gpu
