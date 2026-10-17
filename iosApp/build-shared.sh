#!/bin/sh
set -eu

# Xcode integration; use JDK 21, never Android Studio's Java 25 runtime.
if [ "${OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED:-NO}" = "YES" ]; then
    exit 0
fi
if [ -z "${JAVA_HOME:-}" ]; then
    JAVA_HOME=$(/usr/libexec/java_home -v 21)
fi
export JAVA_HOME
pgp_java_version=$("$JAVA_HOME/bin/java" -XshowSettings:properties -version 2>&1 | awk '/java.specification.version =/ { print $3 }')
if [ "$pgp_java_version" != "21" ]; then
    echo "error: PGP requires JDK 21. Set JAVA_HOME to your JDK 21 installation." >&2
    exit 1
fi
cd "$SRCROOT/.."
# Static information is copied from this checkout; no runtime/network fetch.
pgp_information_dir="$TARGET_BUILD_DIR/$UNLOCALIZED_RESOURCES_FOLDER_PATH/PlayerInformation"
mkdir -p "$pgp_information_dir/licenses"
for pgp_document in PLAYER_HELP_IOS.md PLAYER_PRIVACY_POLICY_IOS.md PLAYER_STUDIO_GUIDE.md; do
    cp "docs/$pgp_document" "$pgp_information_dir/$pgp_document"
done
cp LICENSE THIRD_PARTY_NOTICES.md "$pgp_information_dir/"
cp docs/licenses/*.txt "$pgp_information_dir/licenses/"
exec ./gradlew --gradle-user-home .gradle-local :playerShared:embedAndSignAppleFrameworkForXcode
