#!/usr/bin/env bash
set -euo pipefail

(
  graalvm &&
    ./gradlew --no-daemon clean build &&
    native-image -jar build/libs/compare-directory-1.1-SNAPSHOT.jar build/compare-directory
)
