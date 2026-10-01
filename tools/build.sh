#!/usr/bin/env bash
# The build wrapper this machine needs.
#
# Two environment facts, both local to this host and neither committed to
# `gradle.properties`, because both would break a build anywhere else:
#
#   1. The default `java` on PATH is a JRE (java-25-openjdk) with no `javac`, and
#      Gradle's toolchain auto-detection picks it and then fails to create
#      `compileDebugJavaWithJavac` with "does not provide the required
#      capabilities: [JAVA_COMPILER]". JDK 21 (Temurin) is installed alongside
#      and is a full JDK, so the build is pointed at it explicitly.
#   2. The machine has 4 cores and ~3.8 GB of RAM with a desktop session on it,
#      so the build runs without parallelism and with a bounded worker count. A
#      parallel Compose + KSP build peaks high enough to get OOM-killed here.
#
# Usage:
#   tools/build.sh                       # assembleDebug + the JVM test suite
#   tools/build.sh test                  # JVM tests only
#   tools/build.sh assembleDebug         # debug APK only
#   tools/build.sh lint
#   tools/build.sh record                # re-record the Roborazzi baselines (x86_64 only)
set -euo pipefail

cd "$(dirname "$0")/.."

export JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$JAVA_HOME/bin:$PATH"

# Keep the Gradle daemon's heap where gradle.properties puts it and stop a second
# daemon being started behind the first one's back.
export GRADLE_OPTS="${GRADLE_OPTS:--Dorg.gradle.workers.max=2}"

TASK="${1:-check}"
shift || true

case "$TASK" in
  check)    TASKS=(assembleDebug testDebugUnitTest) ;;
  test)     TASKS=(testDebugUnitTest) ;;
  record)   TASKS=(assembleDebug testDebugUnitTest recordRoborazziDebug) ;;
  *)        TASKS=("$TASK") ;;
esac

echo "JAVA_HOME=$JAVA_HOME"
java -version 2>&1 | head -1

./gradlew --no-parallel --console=plain "${TASKS[@]}" "$@"
