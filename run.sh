#!/usr/bin/env bash
# Build the app, install it on every connected phone/tablet (USB debugging on),
# and open it. Starts the emulator only if no device is connected.
# Usage: ./run.sh
set -euo pipefail
cd "$(dirname "$0")"

export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
ADB="$ANDROID_HOME/platform-tools/adb"
AVD="coc_pixel"

if ! "$ADB" devices | grep -qE 'device$'; then
  echo "Starting emulator '$AVD'..."
  nohup "$ANDROID_HOME/emulator/emulator" -avd "$AVD" -no-snapshot-save >/dev/null 2>&1 &
  until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
fi

echo "Building and installing..."
./gradlew :app:installDebug --console=plain -q

# installDebug installs on every connected device; open the app on each of them.
for serial in $("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'); do
  "$ADB" -s "$serial" shell am start -n com.example.cocwidget/.ui.MainActivity >/dev/null
  echo "Opened on $serial"
done
