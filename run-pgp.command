#!/bin/zsh
set -eu

SCRIPT_DIR=${0:A:h}
ANDROID_STUDIO_JDK="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

if JDK_21_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null); then
    export JAVA_HOME="$JDK_21_HOME"
elif [[ -x "$ANDROID_STUDIO_JDK/bin/java" ]] &&
    "$ANDROID_STUDIO_JDK/bin/java" -version 2>&1 | head -1 | grep -Eq 'version "21([."+])'; then
    export JAVA_HOME="$ANDROID_STUDIO_JDK"
else
    print -u2 "JDK 21が見つかりません。JDK 21をインストールしてください。"
    exit 1
fi

cd "$SCRIPT_DIR"
exec ./gradlew --gradle-user-home .gradle-local :desktopApp:run
