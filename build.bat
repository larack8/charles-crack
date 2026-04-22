@echo off
REM ============================================================================
REM Charles Keygen - cross-platform build script (Windows).
REM ----------------------------------------------------------------------------
REM Produces release artifacts named:
REM   release\CharlesKeygen-<version>.jar                        (universal JAR)
REM   release\CharlesKeygen-<version>-windows-x64.exe            (app-image zip)
REM   release\CharlesKeygen-<version>-windows-x64.msi            (MSI installer)
REM
REM Requirements:
REM   - JDK 17+ on PATH (javac, jar, jpackage).
REM   - Optional: WiX Toolset 3.x for .msi installer (jpackage requirement).
REM ============================================================================
setlocal enabledelayedexpansion

set "APP_NAME=CharlesKeygen"
set "APP_VERSION=1.0.0"
set "MAIN_CLASS=Main"
set "VENDOR=charles-crack"
set "DESCRIPTION=Charles Proxy license key generator"
set "JAR_NAME=charles-keygen.jar"

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"
set "SRC_DIR=%PROJECT_ROOT%\src"
set "ASSETS_DIR=%PROJECT_ROOT%\assets"
set "BUILD_DIR=%PROJECT_ROOT%\build"
set "CLASSES_DIR=%BUILD_DIR%\classes"
set "JAR_PATH=%BUILD_DIR%\%JAR_NAME%"
set "DIST_DIR=%PROJECT_ROOT%\dist\windows"
set "RELEASE_DIR=%PROJECT_ROOT%\release"

REM ---------- Detect arch -----------------------------------------------------
set "ARCH_KEY=x64"
if /I "%PROCESSOR_ARCHITECTURE%"=="ARM64" set "ARCH_KEY=arm64"
if /I "%PROCESSOR_ARCHITECTURE%"=="x86"   set "ARCH_KEY=x86"
echo -^> Detected arch: %ARCH_KEY%

echo -^> Cleaning build output...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
if exist "%DIST_DIR%"  rmdir /s /q "%DIST_DIR%"
mkdir "%CLASSES_DIR%"  2>nul
mkdir "%DIST_DIR%"     2>nul
mkdir "%RELEASE_DIR%"  2>nul

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

REM Ship universal JAR to release\
set "RELEASE_JAR=%RELEASE_DIR%\%APP_NAME%-%APP_VERSION%.jar"
copy /y "%JAR_PATH%" "%RELEASE_JAR%" >nul
echo [OK] Release artifact: %RELEASE_JAR%

REM ---------- Icon ------------------------------------------------------------
set "ICON_ARG="
if exist "%ASSETS_DIR%\logo.ico" (
    set "ICON_ARG=--icon "%ASSETS_DIR%\logo.ico""
    echo -^> Using icon: %ASSETS_DIR%\logo.ico
) else (
    echo WARNING: No logo.ico found in %ASSETS_DIR% - building without custom icon
)

REM ---------- jpackage --------------------------------------------------------
where jpackage >nul 2>&1
if errorlevel 1 (
    echo WARNING: jpackage not found - skipping native build. JAR is at %RELEASE_JAR%.
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
    --main-jar %JAR_NAME% ^
    --main-class %MAIN_CLASS% ^
    --dest "%DIST_DIR%" ^
    --vendor "%VENDOR%" ^
    --description "%DESCRIPTION%" ^
    %ICON_ARG%
if errorlevel 1 (echo X jpackage app-image failed & exit /b 1)

echo -^> Running jpackage (msi installer, requires WiX)...
jpackage ^
    --type msi ^
    --name "%APP_NAME%" ^
    --app-version %APP_VERSION% ^
    --input "%INPUT_DIR%" ^
    --main-jar %JAR_NAME% ^
    --main-class %MAIN_CLASS% ^
    --dest "%DIST_DIR%" ^
    --vendor "%VENDOR%" ^
    --description "%DESCRIPTION%" ^
    --win-shortcut ^
    --win-menu ^
    %ICON_ARG%
if errorlevel 1 (
    echo NOTE: .msi build failed. This usually means WiX 3.x is missing from PATH.
    echo       The app-image in %DIST_DIR%\%APP_NAME% is still usable.
)

REM ---------- Move/rename artifacts to release\ ------------------------------
echo -^> Collecting release artifacts into %RELEASE_DIR%...

REM Zip the app-image directory to CharlesKeygen-1.0.0-windows-x64.exe.zip equivalent
REM Convention requested: <name>-<version>-windows-<arch>.exe → we deliver the
REM standalone .exe from the app-image directly (self-contained exe + runtime folder
REM zipped).
set "APPIMG_DIR=%DIST_DIR%\%APP_NAME%"
if exist "%APPIMG_DIR%" (
    set "DST_ZIP=%RELEASE_DIR%\%APP_NAME%-%APP_VERSION%-windows-%ARCH_KEY%.zip"
    if exist "!DST_ZIP!" del /q "!DST_ZIP!"
    powershell -NoProfile -Command "Compress-Archive -Path '%APPIMG_DIR%\*' -DestinationPath '!DST_ZIP!'" >nul
    echo [OK] !DST_ZIP!

    REM Also copy the raw .exe launcher so users who trust the zip contents can grab it directly
    if exist "%APPIMG_DIR%\%APP_NAME%.exe" (
        copy /y "%APPIMG_DIR%\%APP_NAME%.exe" "%RELEASE_DIR%\%APP_NAME%-%APP_VERSION%-windows-%ARCH_KEY%.exe" >nul
        echo [OK] %RELEASE_DIR%\%APP_NAME%-%APP_VERSION%-windows-%ARCH_KEY%.exe  (launcher only; needs runtime from the .zip)
    )
)

REM Rename .msi if jpackage produced one
for %%M in ("%DIST_DIR%\*.msi") do (
    copy /y "%%M" "%RELEASE_DIR%\%APP_NAME%-%APP_VERSION%-windows-%ARCH_KEY%.msi" >nul
    echo [OK] %RELEASE_DIR%\%APP_NAME%-%APP_VERSION%-windows-%ARCH_KEY%.msi
)

echo.
echo [OK] Release artifacts in %RELEASE_DIR%:
dir /b "%RELEASE_DIR%"
echo All done.
endlocal
