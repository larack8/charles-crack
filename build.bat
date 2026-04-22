@echo off
REM ============================================================================
REM Charles Keygen - cross-platform build script (Windows).
REM ----------------------------------------------------------------------------
REM Builds:
REM   1. build\charles-keygen.jar          (portable fat/runnable JAR)
REM   2. dist\windows\CharlesKeygen*       (native app-image / .msi installer)
REM
REM Requirements:
REM   - JDK 17+ on PATH (javac, jar, jpackage).
REM   - Optional: WiX Toolset 3.x for .msi installer (jpackage requirement).
REM ============================================================================
setlocal enabledelayedexpansion

set "APP_NAME=CharlesKeygen"
set "APP_VERSION=1.0.0"
set "MAIN_CLASS=Main"

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"
set "SRC_DIR=%PROJECT_ROOT%\src"
set "BUILD_DIR=%PROJECT_ROOT%\build"
set "CLASSES_DIR=%BUILD_DIR%\classes"
set "JAR_PATH=%BUILD_DIR%\charles-keygen.jar"
set "DIST_DIR=%PROJECT_ROOT%\dist\windows"

echo -^> Cleaning build output...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%CLASSES_DIR%" 2>nul
mkdir "%DIST_DIR%"    2>nul

REM ---------- Verify tools ----------------------------------------------------
where javac >nul 2>&1 || (echo X Missing javac on PATH. & exit /b 1)
where jar   >nul 2>&1 || (echo X Missing jar on PATH.   & exit /b 1)

REM ---------- Compile ---------------------------------------------------------
echo -^> Compiling Java sources...
set "SOURCES="
for /R "%SRC_DIR%" %%f in (*.java) do set "SOURCES=!SOURCES! "%%f""
javac -encoding UTF-8 -source 11 -target 11 -d "%CLASSES_DIR%" %SOURCES%
if errorlevel 1 (echo X javac failed & exit /b 1)

REM ---------- Manifest + JAR --------------------------------------------------
echo -^> Writing MANIFEST.MF...
set "MANIFEST=%BUILD_DIR%\MANIFEST.MF"
> "%MANIFEST%" echo Manifest-Version: 1.0
>>"%MANIFEST%" echo Main-Class: %MAIN_CLASS%
>>"%MANIFEST%" echo Implementation-Title: %APP_NAME%
>>"%MANIFEST%" echo Implementation-Version: %APP_VERSION%

echo -^> Packaging %JAR_PATH%...
jar cfm "%JAR_PATH%" "%MANIFEST%" -C "%CLASSES_DIR%" .
if errorlevel 1 (echo X jar failed & exit /b 1)
echo [OK] JAR built: %JAR_PATH%

REM ---------- jpackage --------------------------------------------------------
where jpackage >nul 2>&1
if errorlevel 1 (
    echo WARNING: jpackage not found - skipping native build. JAR is at %JAR_PATH%.
    exit /b 0
)

set "INPUT_DIR=%BUILD_DIR%\jpackage-input"
mkdir "%INPUT_DIR%" 2>nul
copy /y "%JAR_PATH%" "%INPUT_DIR%\" >nul

echo -^> Running jpackage (app-image)...
jpackage ^
    --type app-image ^
    --name "%APP_NAME%" ^
    --app-version %APP_VERSION% ^
    --input "%INPUT_DIR%" ^
    --main-jar charles-keygen.jar ^
    --main-class %MAIN_CLASS% ^
    --dest "%DIST_DIR%" ^
    --vendor "charles-crack" ^
    --description "Charles Proxy license key generator"
if errorlevel 1 (echo X jpackage app-image failed & exit /b 1)

echo -^> Running jpackage (msi installer, requires WiX)...
jpackage ^
    --type msi ^
    --name "%APP_NAME%" ^
    --app-version %APP_VERSION% ^
    --input "%INPUT_DIR%" ^
    --main-jar charles-keygen.jar ^
    --main-class %MAIN_CLASS% ^
    --dest "%DIST_DIR%" ^
    --vendor "charles-crack" ^
    --description "Charles Proxy license key generator" ^
    --win-shortcut ^
    --win-menu
if errorlevel 1 (
    echo NOTE: .msi build failed. This usually means WiX 3.x is missing from PATH.
    echo       The app-image in %DIST_DIR%\%APP_NAME% is still usable.
)

echo.
echo [OK] Native artifacts in %DIST_DIR%:
dir /b "%DIST_DIR%"
echo All done.
endlocal
