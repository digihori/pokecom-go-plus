#!/bin/sh
# Compose regenerates jpackage's private resource directory, so apply the volume
# icon to a writable copy and atomically replace only the generated DMG.
set -eu

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 image.dmg icon.icns" >&2
    exit 2
fi
pgp_dmg=$1
pgp_icon=$2
test -f "$pgp_dmg"
test -f "$pgp_icon"
xcrun --find SetFile >/dev/null
pgp_work=$(mktemp -d "$(dirname "$pgp_dmg")/.dmg-icon.XXXXXX")
pgp_mount="$pgp_work/mounted"
pgp_attached=false
cleanup() {
    if [ "$pgp_attached" = true ]; then
        hdiutil detach "$pgp_mount" >/dev/null || return
    fi
    rm -rf "$pgp_work"
}
trap cleanup EXIT
trap 'exit 1' HUP INT TERM

hdiutil convert "$pgp_dmg" -format UDRW -o "$pgp_work/writable.dmg" >/dev/null
mkdir "$pgp_mount"
hdiutil attach "$pgp_work/writable.dmg" -readwrite -owners off -nobrowse -noautoopen \
    -mountpoint "$pgp_mount" >/dev/null
pgp_attached=true
chmod u+w "$pgp_mount"
if [ -e "$pgp_mount/.VolumeIcon.icns" ]; then
    chmod u+w "$pgp_mount/.VolumeIcon.icns"
fi
cp "$pgp_icon" "$pgp_mount/.VolumeIcon.icns"
xcrun SetFile -a C "$pgp_mount"
cmp "$pgp_icon" "$pgp_mount/.VolumeIcon.icns"
hdiutil detach "$pgp_mount" >/dev/null
pgp_attached=false
hdiutil convert "$pgp_work/writable.dmg" -format UDZO \
    -o "$pgp_work/finished.dmg" >/dev/null
hdiutil verify "$pgp_work/finished.dmg" >/dev/null
mv -f "$pgp_work/finished.dmg" "$pgp_dmg"
echo "Studio DMG volume icon applied."
