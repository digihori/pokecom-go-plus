#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ "$(uname -m)" != "arm64" ]; then
    echo "error: iOS CI requires an Apple Silicon runner." >&2
    exit 1
fi
pgp_simulator_id=$(xcrun simctl list devices available -j | python3 -c '
import json, sys
data = json.load(sys.stdin)
devices = [device for runtime, entries in data["devices"].items()
           if ".iOS-" in runtime for device in entries
           if device.get("isAvailable") and device["name"].startswith("iPhone")]
if not devices:
    sys.exit("No available iPhone Simulator; install an iOS runtime in Xcode.")
print(devices[0]["udid"])
')
pgp_result_bundle=${PGP_IOS_RESULT_BUNDLE:-iosApp/build/CI-TestResults-$(date +%Y%m%d-%H%M%S).xcresult}
xcodebuild -version
xcodebuild -project iosApp/PokecomGOPlayer.xcodeproj -scheme PokecomGOPlayer \
    -sdk iphonesimulator -destination "platform=iOS Simulator,id=$pgp_simulator_id" \
    -derivedDataPath iosApp/build/CI-DerivedData \
    -resultBundlePath "$pgp_result_bundle" \
    CODE_SIGNING_ALLOWED=NO build test -parallel-testing-enabled NO
