#!/bin/sh

set -eu

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

GRADLE_VERSION=8.10.2
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME/gradle-$GRADLE_VERSION/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_HOME"
  archive="$GRADLE_HOME/gradle.zip"
  curl -fsSL --retry 3 -o "$archive" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  unzip -q -o "$archive" -d "$GRADLE_HOME"
  rm -f "$archive"
fi

exec "$GRADLE_BIN" "$@"