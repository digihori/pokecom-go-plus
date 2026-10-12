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
exec ./gradlew --gradle-user-home .gradle-local :playerShared:embedAndSignAppleFrameworkForXcode
