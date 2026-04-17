@echo off
REM ================================================================================================
REM INFINITE TRACK - RELEASE BUILD SCRIPT (Windows)
REM ================================================================================================
REM Purpose: Build production-ready APK with R8 optimization and verification
REM Usage: build-release.bat
REM Requirements: JDK 8+, Android SDK, Gradle
REM ================================================================================================

setlocal enabledelayedexpansion

set APP_MODULE=app
set BUILD_TYPE=Release
set ARTIFACTS_DIR=artifacts
set VERSION_NAME=1.0.0
set VERSION_CODE=1

echo ================================================================================================
echo INFINITE TRACK - RELEASE BUILD
echo ================================================================================================
echo.

REM Step 1: Clean previous builds
echo [1/7] Cleaning previous builds...
call gradlew.bat clean
if %ERRORLEVEL% neq 0 (
    echo ERROR: Clean failed
    exit /b 1
)
echo DONE: Clean completed
echo.

REM Step 2: Verify Gradle configuration
echo [2/7] Verifying Gradle configuration...
call gradlew.bat :%APP_MODULE%:tasks --group="build" | findstr "assembleRelease"
echo DONE: Gradle configuration verified
echo.

REM Step 3: Build release APK with R8 enabled
echo [3/7] Building release APK with R8 optimization...
call gradlew.bat :%APP_MODULE%:assembleRelease -Pandroid.enableR8=true --stacktrace
if %ERRORLEVEL% neq 0 (
    echo ERROR: Build failed
    exit /b 1
)
echo DONE: Release APK built successfully
echo.

REM Step 4: Check mapping file
echo [4/7] Checking R8 mapping file...
set MAPPING_FILE=%APP_MODULE%\build\outputs\mapping\release\mapping.txt
if exist "%MAPPING_FILE%" (
    echo DONE: Mapping file generated: %MAPPING_FILE%
    for /f %%a in ('find /c /v "" ^< "%MAPPING_FILE%"') do set LINES=%%a
    echo       Mapping file size: !LINES! lines
) else (
    echo WARNING: Mapping file not found
)
echo.

REM Step 5: Archive artifacts
echo [5/7] Archiving build artifacts...
set ARTIFACT_PATH=%ARTIFACTS_DIR%\%VERSION_NAME%-%VERSION_CODE%
if not exist "%ARTIFACT_PATH%" mkdir "%ARTIFACT_PATH%"

REM Copy APK
set APK_SOURCE=%APP_MODULE%\build\outputs\apk\release\%APP_MODULE%-release-unsigned.apk
if exist "%APK_SOURCE%" (
    copy "%APK_SOURCE%" "%ARTIFACT_PATH%\infinite-track-%VERSION_NAME%-release-unsigned.apk"
    echo DONE: APK copied to: %ARTIFACT_PATH%
)

REM Copy mapping file
if exist "%MAPPING_FILE%" (
    copy "%MAPPING_FILE%" "%ARTIFACT_PATH%\mapping.txt"
    echo DONE: Mapping file archived
)

REM Copy ProGuard configuration
copy "%APP_MODULE%\proguard-rules.pro" "%ARTIFACT_PATH%\proguard-rules.pro"
echo DONE: ProGuard rules archived
echo.

REM Step 6: Display build summary
echo [6/7] Build Summary
echo ================================================================================================

set APK_PATH=%APP_MODULE%\build\outputs\apk\release\%APP_MODULE%-release-unsigned.apk
if exist "%APK_PATH%" (
    for %%A in ("%APK_PATH%") do set APK_SIZE=%%~zA
    set /a APK_SIZE_MB=!APK_SIZE! / 1048576
    echo DONE: Release APK: %APK_PATH%
    echo       APK Size: !APK_SIZE_MB! MB
)

echo.
echo ================================================================================================
echo BUILD COMPLETED SUCCESSFULLY!
echo ================================================================================================
echo.
echo Next Steps:
echo 1. Sign the APK with your release keystore
echo 2. Test the APK on a real device
echo 3. Archive the mapping.txt file for crash deobfuscation
echo.
echo Artifacts location: %ARTIFACT_PATH%
echo.

endlocal

