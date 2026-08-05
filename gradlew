#!/bin/sh
set -e
APP="$(cd "$(dirname "$0")"; pwd)"
GUH="${GRADLE_USER_HOME:-$HOME/.gradle}"
PROPS="$APP/gradle/wrapper/gradle-wrapper.properties"
URL=$(grep "^distributionUrl=" "$PROPS" | cut -d= -f2- | sed 's/\\//g')
BASE=$(basename "$URL" .zip)
GDIR="$GUH/wrapper/dists/$BASE"
if [ ! -f "$GDIR/$BASE/bin/gradle" ]; then
    echo "Downloading Gradle $BASE..."
    mkdir -p "$GDIR"
    tmp=$(mktemp).zip
    curl -fL "$URL" -o "$tmp"
    unzip -q "$tmp" -d "$GDIR"
    rm "$tmp"
fi
exec "$GDIR/$BASE/bin/gradle" "$@"
