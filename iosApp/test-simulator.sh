#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ "$(uname -m)" != "arm64" ]; then
    echo "error: iOS CI requires an Apple Silicon runner." >&2
    exit 1
fi
# Create a disposable device instead of installing test probes over an existing app/ROM Library.
pgp_simulator_spec=$(xcrun simctl list devices available -j | python3 -c '
import json, os, sys
data = json.load(sys.stdin)
requested = os.environ.get("PGP_IOS_SIMULATOR_RUNTIME")
def version(runtime):
    return tuple(int(part) for part in runtime.rsplit(".iOS-", 1)[-1].split("-"))
runtimes = [runtime for runtime in data["devices"] if ".iOS-" in runtime]
for runtime in sorted(runtimes, key=version, reverse=True):
    if requested and runtime != requested:
        continue
    for device in data["devices"][runtime]:
        if device.get("isAvailable") and device["name"].startswith("iPhone"):
            print(device["deviceTypeIdentifier"] + "|" + runtime)
            sys.exit(0)
if requested:
    sys.exit("No available iPhone for requested Simulator runtime: " + requested)
sys.exit("No available iPhone Simulator; install an iOS runtime in Xcode.")
')
pgp_device_type=${pgp_simulator_spec%%|*}
pgp_runtime=${pgp_simulator_spec#*|}
pgp_simulator_id=$(xcrun simctl create "PGP CI isolated" "$pgp_device_type" "$pgp_runtime")
pgp_cleanup_simulator() {
    xcrun simctl shutdown "$pgp_simulator_id" >/dev/null 2>&1 || true
    xcrun simctl delete "$pgp_simulator_id" >/dev/null 2>&1 || true
}
trap pgp_cleanup_simulator EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
pgp_result_bundle=${PGP_IOS_RESULT_BUNDLE:-iosApp/build/CI-TestResults-$(date +%Y%m%d-%H%M%S).xcresult}
xcodebuild -version
echo "Simulator runtime: $pgp_runtime"
echo "Simulator device type: $pgp_device_type"
xcodebuild -project iosApp/PokecomGOPlayer.xcodeproj -scheme PokecomGOPlayer \
    -sdk iphonesimulator -destination "platform=iOS Simulator,id=$pgp_simulator_id" \
    -derivedDataPath iosApp/build/CI-DerivedData \
    -resultBundlePath "$pgp_result_bundle" \
    CODE_SIGNING_ALLOWED=NO build test -parallel-testing-enabled NO "$@"
