#!/bin/bash
set -eu

app=$1
version=$2
output=$3
shift 3
stage=$(mktemp -d)
trap 'rm -rf "$stage"' EXIT
mkdir "$stage/payload"
ditto "$app" "$stage/payload/PokecomGOStudio.app"
python3 - "$stage/component.plist" <<'PY'
import plistlib
import sys

with open(sys.argv[1], "wb") as file:
    plistlib.dump([{
        "RootRelativeBundlePath": "PokecomGOStudio.app",
        "BundleIsRelocatable": False,
        "BundleHasStrictIdentifier": True,
        "BundleIsVersionChecked": True,
        "BundleOverwriteAction": "upgrade",
    }], file)
PY
pkgbuild --root "$stage/payload" --component-plist "$stage/component.plist" \
    --install-location /Applications --identifier com.digihori.pgp.desktop.installer \
    --version "$version" "$stage/PokecomGOStudio.pkg"
productbuild --package "$stage/PokecomGOStudio.pkg" "$@" "$output"
