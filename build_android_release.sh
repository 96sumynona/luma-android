#!/bin/bash
#
# Builds a release APK and app bundle for Luma.
#
# The release build type is signed with the debug key so anyone who clones the repository can
# install it; replace the signing config in app/build.gradle.kts before publishing anywhere.
#
# Usage: ./build_android_release.sh
set -o errexit
set -o nounset
set -o pipefail

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
DIST_DIR="$DIR/dist"
APP_OUT="$DIR/app/build/outputs"

GRADLE_PARAMS=${GRADLE_PARAMS:-}

echo "Luma - building release artifacts"
echo ""

echo "Removing old artifacts..."
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

echo "Building the release variant..."
"$DIR/gradlew" :app:clean :app:assembleRelease :app:bundleRelease ${GRADLE_PARAMS}

echo ""
echo "Copying artifacts..."
cp "$APP_OUT/apk/release/app-release.apk" "$DIST_DIR/luma-release.apk"
cp "$APP_OUT/bundle/release/app-release.aab" "$DIST_DIR/luma-release.aab"
cp "$APP_OUT/mapping/release/mapping.txt" "$DIST_DIR/luma-release-mapping.txt"

echo ""
echo "Done. Artifacts are in $DIST_DIR"
