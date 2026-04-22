#!/usr/bin/env bash
# ============================================================================
# Charles Keygen — cross-platform build script (macOS / Linux).
# ----------------------------------------------------------------------------
# Produces release artifacts named:
#   release/CharlesKeygen-<version>.jar                      (universal JAR)
#   release/CharlesKeygen-<version>-macos-<arch>.dmg         (macOS installer)
#   release/CharlesKeygen-<version>-macos-<arch>-app.zip     (macOS .app bundle)
#   release/CharlesKeygen-<version>-linux-<arch>.tar.gz      (Linux app-image)
#
# Requirements:
#   - JDK 17+ (javac, jar, jpackage).
#   - Windows users: run `build.bat` instead from cmd / PowerShell.
#
# Note: `jpackage` can only build installers for the OS it runs on.
# Run this script on each target OS (or in CI) for full coverage.
# ============================================================================
set -euo pipefail

APP_NAME="CharlesKeygen"
APP_VERSION="1.0.0"
MAIN_CLASS="Main"
VENDOR="charles-crack"
DESCRIPTION="Charles Proxy license key generator"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="${PROJECT_ROOT}/src"
ASSETS_DIR="${PROJECT_ROOT}/assets"
BUILD_DIR="${PROJECT_ROOT}/build"
CLASSES_DIR="${BUILD_DIR}/classes"
JAR_NAME="charles-keygen.jar"
JAR_PATH="${BUILD_DIR}/${JAR_NAME}"
DIST_DIR="${PROJECT_ROOT}/dist"
RELEASE_DIR="${PROJECT_ROOT}/release"

# ---------- Detect OS + arch -------------------------------------------------
UNAME_S="$(uname -s)"
UNAME_M="$(uname -m)"
case "${UNAME_S}" in
    Darwin*)  OS_KEY="macos" ;;
    Linux*)   OS_KEY="linux" ;;
    MINGW*|MSYS*|CYGWIN*) OS_KEY="windows" ;;
    *)        OS_KEY="unknown" ;;
esac
case "${UNAME_M}" in
    x86_64|amd64) ARCH_KEY="x64" ;;
    arm64|aarch64) ARCH_KEY="arm64" ;;
    *) ARCH_KEY="${UNAME_M}" ;;
esac
echo "→ Detected OS: ${OS_KEY}, arch: ${ARCH_KEY}"

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
mkdir -p "${CLASSES_DIR}" "${DIST_DIR}/${OS_KEY}" "${RELEASE_DIR}"

# ---------- Compile ----------------------------------------------------------
echo "→ Compiling Java sources…"
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

# Ship universal JAR to release/
RELEASE_JAR="${RELEASE_DIR}/${APP_NAME}-${APP_VERSION}.jar"
cp -f "${JAR_PATH}" "${RELEASE_JAR}"
echo "✓ Release artifact: ${RELEASE_JAR}"

# ---------- Pick platform icon ----------------------------------------------
ICON_ARG=()
case "${OS_KEY}" in
    macos)   [[ -f "${ASSETS_DIR}/logo.icns" ]] && ICON_ARG=(--icon "${ASSETS_DIR}/logo.icns") ;;
    linux)   [[ -f "${ASSETS_DIR}/logo.png"  ]] && ICON_ARG=(--icon "${ASSETS_DIR}/logo.png")  ;;
    windows) [[ -f "${ASSETS_DIR}/logo.ico"  ]] && ICON_ARG=(--icon "${ASSETS_DIR}/logo.ico")  ;;
esac
if (( ${#ICON_ARG[@]} )); then
    echo "→ Using icon: ${ICON_ARG[1]}"
else
    echo "⚠ No platform icon found in ${ASSETS_DIR} — building without custom icon"
fi

# ---------- Native packaging via jpackage ------------------------------------
if ! command -v jpackage >/dev/null 2>&1; then
    echo "⚠ jpackage not found — skipping native build. JAR is ready at ${RELEASE_JAR}."
    exit 0
fi

INPUT_DIR="${BUILD_DIR}/jpackage-input"
rm -rf "${INPUT_DIR}" && mkdir -p "${INPUT_DIR}"
cp "${JAR_PATH}" "${INPUT_DIR}/"

# Which installer types to build for each OS
case "${OS_KEY}" in
    macos) JPACKAGE_TYPES=("app-image" "dmg") ;;
    linux) JPACKAGE_TYPES=("app-image") ;;
    *)     JPACKAGE_TYPES=("app-image") ;;
esac

run_jpackage() {
    local type="$1"
    echo "→ Running jpackage (--type ${type})…"
    jpackage \
        --type "${type}" \
        --name "${APP_NAME}" \
        --app-version "${APP_VERSION}" \
        --input "${INPUT_DIR}" \
        --main-jar "${JAR_NAME}" \
        --main-class "${MAIN_CLASS}" \
        --dest "${DIST_DIR}/${OS_KEY}" \
        --vendor "${VENDOR}" \
        --description "${DESCRIPTION}" \
        "${ICON_ARG[@]}" \
        || { echo "✗ jpackage failed for type=${type}"; return 1; }
}

for TYPE in "${JPACKAGE_TYPES[@]}"; do
    run_jpackage "${TYPE}"
done

# ---------- Move/rename artifacts to release/ -------------------------------
echo "→ Collecting release artifacts into ${RELEASE_DIR}…"

case "${OS_KEY}" in
    macos)
        # .dmg → CharlesKeygen-1.0.0-macos-<arch>.dmg
        SRC_DMG="$(find "${DIST_DIR}/${OS_KEY}" -maxdepth 1 -name '*.dmg' | head -n 1 || true)"
        if [[ -n "${SRC_DMG}" ]]; then
            DST_DMG="${RELEASE_DIR}/${APP_NAME}-${APP_VERSION}-macos-${ARCH_KEY}.dmg"
            cp -f "${SRC_DMG}" "${DST_DMG}"
            echo "✓ ${DST_DMG}"
        fi
        # .app bundle → zip to CharlesKeygen-1.0.0-macos-<arch>-app.zip
        SRC_APP="${DIST_DIR}/${OS_KEY}/${APP_NAME}.app"
        if [[ -d "${SRC_APP}" ]]; then
            DST_ZIP="${RELEASE_DIR}/${APP_NAME}-${APP_VERSION}-macos-${ARCH_KEY}-app.zip"
            rm -f "${DST_ZIP}"
            ( cd "${DIST_DIR}/${OS_KEY}" && zip -qry "${DST_ZIP}" "${APP_NAME}.app" )
            echo "✓ ${DST_ZIP}"
        fi
        ;;
    linux)
        # app-image dir → tar.gz as CharlesKeygen-1.0.0-linux-<arch>.tar.gz
        SRC_APPIMG="${DIST_DIR}/${OS_KEY}/${APP_NAME}"
        if [[ -d "${SRC_APPIMG}" ]]; then
            DST_TGZ="${RELEASE_DIR}/${APP_NAME}-${APP_VERSION}-linux-${ARCH_KEY}.tar.gz"
            rm -f "${DST_TGZ}"
            ( cd "${DIST_DIR}/${OS_KEY}" && tar -czf "${DST_TGZ}" "${APP_NAME}" )
            echo "✓ ${DST_TGZ}"
        fi
        ;;
esac

echo ""
echo "✓ Native artifacts staged in ${DIST_DIR}/${OS_KEY}"
echo "✓ Release artifacts in ${RELEASE_DIR}:"
ls -la "${RELEASE_DIR}"
echo ""
echo "All done. 🎉"
