#!/usr/bin/env bash
# The build wrapper this machine needs.
#
# Three environment facts, all local to this host and none of them in
# `gradle.properties`, because each would break a build anywhere else:
#
#   1. The default `java` on PATH is a JRE (java-25-openjdk) with no `javac`, and
#      Gradle's toolchain auto-detection picks it and then fails to create
#      `compileDebugJavaWithJavac` with "does not provide the required
#      capabilities: [JAVA_COMPILER]". JDK 21 (Temurin) is installed alongside
#      and is a full JDK, so the build is pointed at it explicitly.
#   2. The machine has 4 cores and ~3.8 GB of RAM with a desktop session on it,
#      so the build runs without parallelism and with a bounded worker count. A
#      parallel Compose + KSP build peaks high enough to get OOM-killed here.
#   3. The test suite renders 30 full-screen images through Robolectric's *native*
#      graphics runtime, and the Gradle daemon cannot be resident while it does.
#      See [run_separated] - that is the one genuinely surprising thing here.
#
# Usage:
#   tools/build.sh                       # assembleDebug + the JVM test suite
#   tools/build.sh test                  # JVM tests only
#   tools/build.sh assembleDebug         # debug APK only
#   tools/build.sh lint
#   tools/build.sh record                # re-record the Roborazzi baselines
set -euo pipefail

cd "$(dirname "$0")/.."

export JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$JAVA_HOME/bin:$PATH"

# Keep the Gradle daemon's heap where gradle.properties puts it and stop a second
# daemon being started behind the first one's back.
export GRADLE_OPTS="${GRADLE_OPTS:--Dorg.gradle.workers.max=2}"

# Robolectric's temp goes on /home, not /tmp.
#
# `/tmp` here is a 1.9 GB tmpfs, and Robolectric extracts a ~205 MB native runtime into
# it on every run and does not clean up afterwards. Three runs later there is 600 MB
# of stale copies on a 1.9 GB volume, and the next extraction fails - which surfaces
# as "Unable to load Robolectric native runtime library" in whichever test class
# started first, on an otherwise identical tree. It is a disk-space failure wearing a
# library-load error's name.
#
# `/home` is a real volume with ~96 GB free, so the same extraction cannot run it out.
mkdir -p "$HOME/.cache/salah-test-tmp"
export JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$HOME/.cache/salah-test-tmp"

TASK="${1:-check}"
shift || true

echo "JAVA_HOME=$JAVA_HOME"
java -version 2>&1 | head -1

gradle() { ./gradlew --no-parallel --console=plain "$@"; }

# [run_separated] Runs a compile task and a test task as two invocations with the
# daemon stopped in between.
#
# `assembleDebug` drives the Compose compiler inside the Gradle daemon, which takes
# most of its 1280m and keeps it. The test phase then wants a worker large enough to
# hold the screenshot suite's native bitmaps, and on this 3.8 GB host it does not
# get one. The symptom was "Unable to load Robolectric native runtime library", raised
# in whichever test class happened to start first and moving between runs on an
# identical tree - which is what gave it away as the host rather than the code.
#
# Stopping the daemon hands the machine back. It costs one daemon restart, and it is
# the only reason `check` here is not a single command.
run_separated() {
  echo "--- $1 ---"
  gradle "$1" "$@"
  echo "--- stopping the Gradle daemon before the test phase ---"
  ./gradlew --stop || true
  sleep 3
  echo "--- $2 ---"
  gradle "$2" "$@"
}

case "$TASK" in
  check)    run_separated assembleDebug testDebugUnitTest ;;
  record)   run_separated assembleDebug testDebugUnitTest recordRoborazziDebug ;;
  test)     gradle testDebugUnitTest "$@" ;;
  *)        gradle "$TASK" "$@" ;;
esac
