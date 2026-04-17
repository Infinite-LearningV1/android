#!/bin/bash
# ================================================================================================
# INFINITE TRACK - RELEASE BUILD SCRIPT
# ================================================================================================
# Purpose: Build production-ready APK with R8 optimization and verification
# Usage: ./build-release.sh
# Requirements: JDK 8+, Android SDK, Gradle
# ================================================================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
APP_MODULE="app"
BUILD_TYPE="Release"
ARTIFACTS_DIR="artifacts"
VERSION_NAME="1.0.0"
VERSION_CODE="1"

echo -e "${BLUE}================================================================================================${NC}"
echo -e "${BLUE}INFINITE TRACK - RELEASE BUILD${NC}"
echo -e "${BLUE}================================================================================================${NC}"
echo ""

# Step 1: Clean previous builds
echo -e "${YELLOW}[1/7] Cleaning previous builds...${NC}"
./gradlew clean
echo -e "${GREEN}✓ Clean completed${NC}"
echo ""

# Step 2: Verify Gradle configuration
echo -e "${YELLOW}[2/7] Verifying Gradle configuration...${NC}"
./gradlew :${APP_MODULE}:tasks --group="build" | grep "assembleRelease"
echo -e "${GREEN}✓ Gradle configuration verified${NC}"
echo ""

# Step 3: Build release APK with R8 enabled
echo -e "${YELLOW}[3/7] Building release APK with R8 optimization...${NC}"
./gradlew :${APP_MODULE}:assembleRelease -Pandroid.enableR8=true --stacktrace
echo -e "${GREEN}✓ Release APK built successfully${NC}"
echo ""

# Step 4: Verify BuildConfig.DEBUG is false
echo -e "${YELLOW}[4/7] Verifying BuildConfig.DEBUG flag...${NC}"
if unzip -p ${APP_MODULE}/build/outputs/apk/release/${APP_MODULE}-release-unsigned.apk classes.dex | grep -q "DEBUG.*false"; then
    echo -e "${GREEN}✓ BuildConfig.DEBUG is FALSE (correct)${NC}"
else
    echo -e "${RED}✗ WARNING: BuildConfig.DEBUG verification failed${NC}"
fi
echo ""

# Step 5: Check R8 mapping file
echo -e "${YELLOW}[5/7] Checking R8 mapping file...${NC}"
MAPPING_FILE="${APP_MODULE}/build/outputs/mapping/release/mapping.txt"
if [ -f "$MAPPING_FILE" ]; then
    echo -e "${GREEN}✓ Mapping file generated: $MAPPING_FILE${NC}"
    echo -e "${BLUE}  Mapping file size: $(wc -l < $MAPPING_FILE) lines${NC}"
else
    echo -e "${RED}✗ WARNING: Mapping file not found${NC}"
fi
echo ""

# Step 6: Archive artifacts
echo -e "${YELLOW}[6/7] Archiving build artifacts...${NC}"
ARTIFACT_PATH="${ARTIFACTS_DIR}/${VERSION_NAME}-${VERSION_CODE}"
mkdir -p "$ARTIFACT_PATH"

# Copy APK
if [ -f "${APP_MODULE}/build/outputs/apk/release/${APP_MODULE}-release-unsigned.apk" ]; then
    cp "${APP_MODULE}/build/outputs/apk/release/${APP_MODULE}-release-unsigned.apk" \
       "$ARTIFACT_PATH/infinite-track-${VERSION_NAME}-release-unsigned.apk"
    echo -e "${GREEN}✓ APK copied to: $ARTIFACT_PATH${NC}"
fi

# Copy mapping file
if [ -f "$MAPPING_FILE" ]; then
    cp "$MAPPING_FILE" "$ARTIFACT_PATH/mapping.txt"
    echo -e "${GREEN}✓ Mapping file archived${NC}"
fi

# Copy ProGuard configuration
cp "${APP_MODULE}/proguard-rules.pro" "$ARTIFACT_PATH/proguard-rules.pro"
echo -e "${GREEN}✓ ProGuard rules archived${NC}"
echo ""

# Step 7: Display build summary
echo -e "${YELLOW}[7/7] Build Summary${NC}"
echo -e "${BLUE}================================================================================================${NC}"

APK_PATH="${APP_MODULE}/build/outputs/apk/release/${APP_MODULE}-release-unsigned.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo -e "${GREEN}✓ Release APK: $APK_PATH${NC}"
    echo -e "${GREEN}  APK Size: $APK_SIZE${NC}"
    
    # Display APK info using aapt if available
    if command -v aapt &> /dev/null; then
        echo ""
        echo -e "${BLUE}APK Information:${NC}"
        aapt dump badging "$APK_PATH" | grep -E "package:|sdkVersion:|targetSdkVersion:|versionCode:|versionName:"
    fi
fi

echo ""
echo -e "${BLUE}================================================================================================${NC}"
echo -e "${GREEN}BUILD COMPLETED SUCCESSFULLY!${NC}"
echo -e "${BLUE}================================================================================================${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "1. Sign the APK with your release keystore"
echo -e "2. Test the APK on a real device"
echo -e "3. Archive the mapping.txt file for crash deobfuscation"
echo ""
echo -e "${YELLOW}Artifacts location: ${ARTIFACT_PATH}${NC}"

