#!/usr/bin/env bash
################################################################################
# PreCI macOS PKG Installer Builder
#
# Builds a .pkg installer for macOS using pkgbuild + productbuild.
#
# Usage:
#   ./build-pkg.sh <VERSION> [SIGNING_IDENTITY] [APPLE_ID] [APP_PASSWORD] [TEAM_ID]
#
# Arguments:
#   VERSION           - Version string (e.g. 1.0.0), required
#   SIGNING_IDENTITY  - Optional Developer ID Installer certificate name
#                       for productsign (e.g. "Developer ID Installer: ...")
#   APPLE_ID          - Optional Apple ID for notarization (e.g. "user@example.com")
#   APP_PASSWORD      - Optional app-specific password for notarization
#   TEAM_ID           - Optional Team ID for notarization (e.g. "XXXXXXXXXX")
#
# Prerequisites:
#   - macOS with Xcode command-line tools installed
#   - All distributable files in PreCI/ directory:
#       preci  preci-server  preci-mcp  preci-updater
#       config/  checkerset/
#       install.sh  uninstall.sh  uninstall_old_preci.sh
#
# Notarization:
#   When SIGNING_IDENTITY, APPLE_ID, APP_PASSWORD and TEAM_ID are all provided,
#   the script will submit the signed package to Apple for notarization and
#   staple the notarization ticket to the package.
#
# Output:
#   PreCI-Setup-<VERSION>.pkg
#
################################################################################

set -euo pipefail

################################################################################
# Configuration
################################################################################

VERSION="${1:-}"
SIGNING_IDENTITY="${2:-}"
APPLE_ID="${3:-}"
APP_PASSWORD="${4:-}"
TEAM_ID="${5:-}"

if [[ -z "${VERSION}" ]]; then
    echo "Usage: $0 <VERSION> [SIGNING_IDENTITY] [APPLE_ID] [APP_PASSWORD] [TEAM_ID]"
    echo "  Example: $0 1.0.0"
    echo "  Example: $0 1.0.0 \"Developer ID Installer: PreCI Team (XXXXXXXXXX)\""
    echo "  Example: $0 1.0.0 \"Developer ID Installer: PreCI Team (XXXXXXXXXX)\" user@example.com app-password XXXXXXXXXX"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="${SCRIPT_DIR}/PreCI"
PKG_SCRIPTS_DIR="${SCRIPT_DIR}/pkg-scripts"
BUILD_DIR="${SCRIPT_DIR}/.build-pkg-tmp"
OUTPUT_FILE="${SCRIPT_DIR}/PreCI-Setup-${VERSION}.pkg"

PKG_IDENTIFIER="com.preci.pkg"
STAGING_LOCATION="/tmp/preci-pkg-staging"

REQUIRED_FILES=(
    "preci"
    "preci-server"
    "preci-mcp"
    "preci-updater"
    "config"
    "checkerset"
    "install.sh"
    "uninstall.sh"
    "uninstall_old_preci.sh"
)

################################################################################
# Helpers
################################################################################

info()    { echo "[INFO] $*"; }
success() { echo "[SUCCESS] $*"; }
error()   { echo "[ERROR] $*" >&2; }

cleanup() {
    rm -rf "${BUILD_DIR}"
}
trap cleanup EXIT

################################################################################
# Validate prerequisites
################################################################################

info "Validating prerequisites..."

if ! command -v pkgbuild &>/dev/null; then
    error "pkgbuild not found. Install Xcode command-line tools: xcode-select --install"
    exit 1
fi

if ! command -v productbuild &>/dev/null; then
    error "productbuild not found. Install Xcode command-line tools: xcode-select --install"
    exit 1
fi

if [[ ! -d "${DIST_DIR}" ]]; then
    error "PreCI/ directory not found at: ${DIST_DIR}"
    error "Please place all distributable files in PreCI/ before building."
    exit 1
fi

missing=()
for f in "${REQUIRED_FILES[@]}"; do
    if [[ ! -e "${DIST_DIR}/${f}" ]]; then
        missing+=("${f}")
    fi
done

if [[ ${#missing[@]} -gt 0 ]]; then
    error "Missing required files in PreCI/:"
    for f in "${missing[@]}"; do
        echo "  - ${f}"
    done
    exit 1
fi

info "All prerequisites validated"

################################################################################
# Prepare build directory
################################################################################

info "Preparing build directory..."

rm -rf "${BUILD_DIR}"
mkdir -p "${BUILD_DIR}/payload" "${BUILD_DIR}/scripts" "${BUILD_DIR}/resources"

# Copy payload
cp -R "${DIST_DIR}/"* "${BUILD_DIR}/payload/"

# Copy and make scripts executable
cp "${PKG_SCRIPTS_DIR}/preinstall"  "${BUILD_DIR}/scripts/"
cp "${PKG_SCRIPTS_DIR}/postinstall" "${BUILD_DIR}/scripts/"
chmod +x "${BUILD_DIR}/scripts/preinstall" "${BUILD_DIR}/scripts/postinstall"

################################################################################
# Generate resource files
################################################################################

info "Generating installer resources..."

cat > "${BUILD_DIR}/resources/welcome.html" << WELCOMEEOF
<!DOCTYPE html>
<html>
<head><meta charset="utf-8"></head>
<body style="font-family: -apple-system, Helvetica Neue, sans-serif; font-size: 13px;">
<h2>PreCI ${VERSION}</h2>
<p>此向导将引导您完成 PreCI ${VERSION} 的安装。</p>
<p>安装目录：<code>~/PreCI</code></p>
<p>建议在安装前关闭所有正在使用 PreCI 的程序。</p>
</body>
</html>
WELCOMEEOF

cat > "${BUILD_DIR}/resources/conclusion.html" << CONCLUSIONEOF
<!DOCTYPE html>
<html>
<head><meta charset="utf-8"></head>
<body style="font-family: -apple-system, Helvetica Neue, sans-serif; font-size: 13px;">
<h2>安装完成</h2>
<p>PreCI ${VERSION} 已成功安装到 <code>~/PreCI</code>。</p>
<p><strong>请重新打开终端窗口</strong>使环境变量生效，然后运行：</p>
<pre>  preci version</pre>
<p>卸载方式：运行 <code>~/PreCI/uninstall.sh</code></p>
</body>
</html>
CONCLUSIONEOF

################################################################################
# Generate distribution.xml
################################################################################

info "Generating distribution.xml..."

cat > "${BUILD_DIR}/distribution.xml" << DISTEOF
<?xml version="1.0" encoding="utf-8"?>
<installer-gui-script minSpecVersion="2">
    <title>PreCI ${VERSION}</title>
    <welcome  file="welcome.html" />
    <conclusion file="conclusion.html" />

    <options customize="never" require-scripts="true" hostArchitectures="x86_64,arm64" />
    <domains enable_localSystem="true" />

    <choices-outline>
        <line choice="default">
            <line choice="${PKG_IDENTIFIER}" />
        </line>
    </choices-outline>

    <choice id="default" />
    <choice id="${PKG_IDENTIFIER}" visible="false">
        <pkg-ref id="${PKG_IDENTIFIER}" />
    </choice>

    <pkg-ref id="${PKG_IDENTIFIER}" version="${VERSION}" onConclusion="none">PreCI-component.pkg</pkg-ref>
</installer-gui-script>
DISTEOF

################################################################################
# Build component package
################################################################################

info "Building component package..."

pkgbuild \
    --root "${BUILD_DIR}/payload" \
    --install-location "${STAGING_LOCATION}" \
    --identifier "${PKG_IDENTIFIER}" \
    --version "${VERSION}" \
    --scripts "${BUILD_DIR}/scripts" \
    "${BUILD_DIR}/PreCI-component.pkg"

################################################################################
# Build distribution package
################################################################################

info "Building distribution package..."

productbuild \
    --distribution "${BUILD_DIR}/distribution.xml" \
    --resources "${BUILD_DIR}/resources" \
    --package-path "${BUILD_DIR}" \
    "${BUILD_DIR}/PreCI-unsigned.pkg"

################################################################################
# Sign (optional)
################################################################################

if [[ -n "${SIGNING_IDENTITY}" ]]; then
    info "Signing package with: ${SIGNING_IDENTITY}"
    productsign \
        --sign "${SIGNING_IDENTITY}" \
        "${BUILD_DIR}/PreCI-unsigned.pkg" \
        "${OUTPUT_FILE}"
else
    mv "${BUILD_DIR}/PreCI-unsigned.pkg" "${OUTPUT_FILE}"
fi

################################################################################
# Notarize & Staple (可选，需签名+公证参数齐全)
################################################################################

if [[ -n "${SIGNING_IDENTITY}" && -n "${APPLE_ID}" && -n "${APP_PASSWORD}" && -n "${TEAM_ID}" ]]; then
    info "Submitting package for notarization..."
    NOTARY_LOG="${BUILD_DIR}/notary-output.log"
    xcrun notarytool submit "${OUTPUT_FILE}" \
        --apple-id "${APPLE_ID}" \
        --password "${APP_PASSWORD}" \
        --team-id "${TEAM_ID}" \
        --wait 2>&1 | tee "${NOTARY_LOG}" || true

    # 提取 submission id
    SUBMISSION_ID=$(grep -m1 'id:' "${NOTARY_LOG}" | awk '{print $2}')

    # 检查公证结果是否成功
    if grep -q "status: Accepted" "${NOTARY_LOG}"; then
        info "Notarization accepted."
    else
        error "Notarization failed!"
        if [[ -n "${SUBMISSION_ID}" ]]; then
            info "Fetching notarization log for details..."
            xcrun notarytool log "${SUBMISSION_ID}" \
                --apple-id "${APPLE_ID}" \
                --password "${APP_PASSWORD}" \
                --team-id "${TEAM_ID}" 2>&1 || true
        fi
        error "Please fix the issues above and retry."
        error "HINT: Make sure you are using a 'Developer ID Installer' certificate, NOT '3rd Party Mac Developer Installer'."
        exit 1
    fi

    info "Stapling notarization ticket..."
    xcrun stapler staple "${OUTPUT_FILE}"

    info "Verifying notarization..."
    xcrun stapler validate "${OUTPUT_FILE}"
    spctl --assess -v --type install "${OUTPUT_FILE}"

    success "Package signed, notarized and stapled successfully."
elif [[ -n "${SIGNING_IDENTITY}" ]]; then
    info "Skipping notarization: APPLE_ID, APP_PASSWORD or TEAM_ID not provided."
    info "Package is signed but NOT notarized. Users may see Gatekeeper warnings."
else
    info "Skipping signing and notarization: no SIGNING_IDENTITY provided."
fi

################################################################################
# Done
################################################################################

success "Package created: ${OUTPUT_FILE}"
echo ""
echo "Install via GUI:  open ${OUTPUT_FILE}"
echo "Install via CLI:  sudo installer -pkg ${OUTPUT_FILE} -target /"
echo ""
