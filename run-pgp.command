#!/bin/zsh
set -eu

SCRIPT_DIR=${0:A:h}
ANDROID_STUDIO_JDK="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

if [[ -z ${JAVA_HOME:-} ]]; then
    if [[ -d "$ANDROID_STUDIO_JDK" ]]; then
        export JAVA_HOME="$ANDROID_STUDIO_JDK"
    elif /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 21)
    else
        print -u2 "JDK 21が見つかりません。Android StudioまたはJDK 21をインストールしてください。"
        exit 1
    fi
fi

cd "$SCRIPT_DIR"
exec ./gradlew --gradle-user-home .gradle-local :desktopApp:run
