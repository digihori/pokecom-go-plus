#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
# Default remains the complete local UI suite. CI explicitly selects --unit.
pgp_test_mode=full
pgp_scheme=PokecomGOPlayer
if [ "${1:-}" = "--unit" ]; then
    pgp_test_mode=unit
    pgp_scheme=PokecomGOPlayerUnitTests
    shift
    # Device I/O can abort inside CoreAudio initialization on hosted runners,
    # before an XCTest timeout can recover. Keep actual offline engine coverage
    # in this suite; exercise live output in the full local suite instead.
    set -- -skip-testing:PokecomGOPlayerTests/PlayerPlaybackHostTests/testLiveSimulatorAudioNodeConsumesSyntheticPCM "$@"
fi
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
    for device in sorted(data["devices"][runtime], key=lambda d: d.get("state") != "Booted"):
        if device.get("isAvailable") and device["name"].startswith("iPhone"):
            print(device["deviceTypeIdentifier"] + "|" + runtime + "|" + device["udid"])
            sys.exit(0)
if requested:
    sys.exit("No available iPhone for requested Simulator runtime: " + requested)
sys.exit("No available iPhone Simulator; install an iOS runtime in Xcode.")
')
pgp_device_type=${pgp_simulator_spec%%|*}
pgp_runtime_and_id=${pgp_simulator_spec#*|}
pgp_runtime=${pgp_runtime_and_id%%|*}
pgp_owns_simulator=1
if [ "$pgp_test_mode" = "unit" ] && [ "${GITHUB_ACTIONS:-false}" = "true" ]; then
    # Hosted CI has no user app/ROM Library. Reuse its preconfigured device only there.
    pgp_simulator_id=${pgp_runtime_and_id#*|}
    pgp_owns_simulator=0
else
    pgp_simulator_id=$(xcrun simctl create "PGP CI isolated" "$pgp_device_type" "$pgp_runtime")
fi
pgp_boot_pid=
pgp_cleanup_simulator() {
    pgp_exit_status=$?
    if [ -n "$pgp_boot_pid" ]; then
        kill "$pgp_boot_pid" >/dev/null 2>&1 || true
        wait "$pgp_boot_pid" 2>/dev/null || true
    fi
    if [ "$pgp_exit_status" -ne 0 ] && [ "$pgp_test_mode" = "full" ]; then
        mkdir -p iosApp/build
        xcrun simctl spawn "$pgp_simulator_id" log show --style compact --last 20m \
            --predicate 'process CONTAINS[c] "fileprovider" OR process == "DocumentsApp" OR subsystem CONTAINS[c] "FileProvider"' \
            > iosApp/build/CI-FileProvider.log 2>&1 || true
    fi
    if [ "$pgp_owns_simulator" = "1" ]; then
        xcrun simctl shutdown "$pgp_simulator_id" >/dev/null 2>&1 || true
        xcrun simctl delete "$pgp_simulator_id" >/dev/null 2>&1 || true
    fi
}
trap pgp_cleanup_simulator EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
pgp_result_bundle=${PGP_IOS_RESULT_BUNDLE:-iosApp/build/CI-TestResults-$(date +%Y%m%d-%H%M%S).xcresult}
mkdir -p iosApp/build
pgp_started=$(date +%s)
echo "Simulator runtime: $pgp_runtime"
echo "Simulator device type: $pgp_device_type"
echo "Test mode: $pgp_test_mode; scheme: $pgp_scheme"
if [ "$pgp_test_mode" = "unit" ]; then
    # A generic build destination does not wait for a booted device. Overlap
    # first-boot migration with compilation, then require both to succeed.
    xcrun simctl bootstatus "$pgp_simulator_id" -b > iosApp/build/CI-SimulatorBoot.log 2>&1 &
    pgp_boot_pid=$!
    xcodebuild -project iosApp/PokecomGOPlayer.xcodeproj -scheme "$pgp_scheme" \
        -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' \
        -derivedDataPath iosApp/build/CI-DerivedData \
        CODE_SIGNING_ALLOWED=NO build-for-testing -showBuildTimingSummary "$@"
    echo "Build elapsed: $(($(date +%s) - pgp_started)) seconds"
    if wait "$pgp_boot_pid"; then
        pgp_boot_pid=
    else
        pgp_boot_pid=
        cat iosApp/build/CI-SimulatorBoot.log
        exit 1
    fi
    cat iosApp/build/CI-SimulatorBoot.log
    echo "Build and Simulator ready: $(($(date +%s) - pgp_started)) seconds"
    pgp_test_action=test-without-building
else
    xcrun simctl bootstatus "$pgp_simulator_id" -b
    xcrun simctl launch "$pgp_simulator_id" com.apple.DocumentsApp
    xcrun simctl list runtimes
    pgp_test_action=test
fi
pgp_tests_started=$(date +%s)
xcodebuild -version
xcodebuild -project iosApp/PokecomGOPlayer.xcodeproj -scheme "$pgp_scheme" \
    -sdk iphonesimulator -destination "platform=iOS Simulator,id=$pgp_simulator_id" \
    -derivedDataPath iosApp/build/CI-DerivedData \
    -resultBundlePath "$pgp_result_bundle" \
    CODE_SIGNING_ALLOWED=NO "$pgp_test_action" -parallel-testing-enabled NO "$@"
echo "Test launch and execution: $(($(date +%s) - pgp_tests_started)) seconds"
echo "Total build and test: $(($(date +%s) - pgp_started)) seconds"
