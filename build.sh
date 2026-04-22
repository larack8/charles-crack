#!/usr/bin/env bash
# ============================================================================
# Charles Keygen — cross-platform build script (macOS / Linux).
# ----------------------------------------------------------------------------
# Builds:
#   1. build/charles-keygen.jar                     (portable fat/runnable JAR)
#   2. dist/<os>/CharlesKeygen[.app|.exe|...]       (native installer/bundle
#                                                     via jpackage — current OS)
#
# Requirements:
#   - JDK 17+ (any LTS with `javac`, `jar`, and `jpackage`).
#   - Windows users: run `build.bat` instead from cmd / PowerShell.
#
# Note: `jpackage` can only build an installer for the OS it is running on.
# Run this script on each target OS (or in CI) to get all three binaries.
# ============================================================================
set -euo pipefail

APP_NAME="CharlesKeygen"
APP_VERSION="1.0.0"
MAIN_CLASS="Main"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="${PROJECT_ROOT}/src"
BUILD_DIR="${PROJECT_ROOT}/build"
CLASSES_DIR="${BUILD_DIR}/classes"
JAR_PATH="${BUILD_DIR}/charles-keygen.jar"
DIST_DIR="${PROJECT_ROOT}/dist"

# ---------- Detect OS --------------------------------------------------------
UNAME="$(uname -s)"
case "${UNAME}" in
    Darwin*)  OS_KEY="macos"  ; JPACKAGE_TYPES=("app-image" "dmg") ;;
    Linux*)   OS_KEY="linux"  ; JPACKAGE_TYPES=("app-image")       ;;
    MINGW*|MSYS*|CYGWIN*) OS_KEY="windows"; JPACKAGE_TYPES=("app-image") ;;
    *)        OS_KEY="unknown"; JPACKAGE_TYPES=("app-image")       ;;
esac
echo "→ Detected OS: ${OS_KEY}"

# ---------- Locate JDK tools -------------------------------------------------
require() {
    command -v "$1" >/dev/null 2>&1 || { echo "✗ Missing required tool: $1"; exit 1; }
}
require javac
require jar

JAVAC_VERSION="$(javac -version 2>&1 | awk '{print $2}')"
echo "→ Using javac ${JAVAC_VERSION}"

# ---------- Clean ------------------------------------------------------------
echo "→ Cleaning build output…"
rm -rf "${BUILD_DIR}" "${DIST_DIR}/${OS_KEY}"
mkdir -p "${CLASSES_DIR}" "${DIST_DIR}/${OS_KEY}"

# ---------- Compile ----------------------------------------------------------
echo "→ Compiling Java sources…"
# Build list of sources (portable across bash versions).
SOURCES=()
while IFS= read -r -d '' f; do SOURCES+=("$f"); done \
    < <(find "${SRC_DIR}" -name '*.java' -type f -print0)
javac -encoding UTF-8 -source 11 -target 11 -d "${CLASSES_DIR}" "${SOURCES[@]}"

# ---------- Manifest + JAR ---------------------------------------------------
echo "→ Writing MANIFEST.MF…"
MANIFEST="${BUILD_DIR}/MANIFEST.MF"
cat > "${MANIFEST}" <<EOF
Manifest-Version: 1.0
Main-Class: ${MAIN_CLASS}
Implementation-Title: ${APP_NAME}
Implementation-Version: ${APP_VERSION}
EOF

echo "→ Packaging ${JAR_PATH}…"
jar cfm "${JAR_PATH}" "${MANIFEST}" -C "${CLASSES_DIR}" .
echo "✓ JAR built: ${JAR_PATH}"

# ---------- Native packaging via jpackage ------------------------------------
if ! command -v jpackage >/dev/null 2>&1; then
    echo "⚠ jpackage not found — skipping native build. JAR is ready at ${JAR_PATH}."
    exit 0
fi

INPUT_DIR="${BUILD_DIR}/jpackage-input"
rm -rf "${INPUT_DIR}" && mkdir -p "${INPUT_DIR}"
cp "${JAR_PATH}" "${INPUT_DIR}/"

for TYPE in "${JPACKAGE_TYPES[@]}"; do
    echo "→ Running jpackage (--type ${TYPE})…"
    jpackage \
        --type "${TYPE}" \
        --name "${APP_NAME}" \
        --app-version "${APP_VERSION}" \
        --input "${INPUT_DIR}" \
        --main-jar "charles-keygen.jar" \
        --main-class "${MAIN_CLASS}" \
        --dest "${DIST_DIR}/${OS_KEY}" \
        --vendor "charles-crack" \
        --description "Charles Proxy license key generator" \
        || { echo "✗ jpackage failed for type=${TYPE}"; exit 1; }
done

echo "✓ Native artifacts in ${DIST_DIR}/${OS_KEY}:"
ls -la "${DIST_DIR}/${OS_KEY}"
echo ""
echo "All done. 🎉"
