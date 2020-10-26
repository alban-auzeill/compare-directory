#!/usr/bin/env bash
set -euo pipefail

do_release() {
  export CURRENT_VERSION="$(sed -rn "s/^project\.version '([^']*)'$/\1/p" build.gradle)"
  echo "current version: ${CURRENT_VERSION}"
  export RELEASE_VERSION="${CURRENT_VERSION/-SNAPSHOT/}"
  echo "release version: ${RELEASE_VERSION}"
  (
    graalvm
    ./gradlew --no-daemon "-PreleaseVersion=${RELEASE_VERSION}" clean build
    native-image -jar "build/libs/compare-directory-${RELEASE_VERSION}.jar" build/compare-directory
    echo "Binary file: build/compare-directory is ready to be downloaded on"
    echo "https://github.com/alban-auzeill/compare-directory/releases"
    echo "to create the release: v${RELEASE_VERSION}"
    echo
    echo "Then build.gradle need to be prepared for next release."
  )
}

do_release
