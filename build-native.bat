@echo off
REM Builds the native Windows EXE installer via GraalVM Native Image.
REM Requires GraalVM 25+ on PATH (set JAVA_HOME to your GraalVM installation)
REM and MSVC build tools (run from a VS Developer Command Prompt, or
REM install Visual Studio Build Tools and run vcvars64.bat first).
REM
REM Output: target\<brand.name>-<brand.version>-double-click-me.exe
REM         target\toolscreen-downloader.exe

cd /d "%~dp0"

set BRAND_NAME=EasyInjectBundled
set BRAND_VERSION=1.0
for /f "tokens=1,* delims==" %%a in ('findstr /b "brand.name=" branding.properties 2^>nul') do set BRAND_NAME=%%b
for /f "tokens=1,* delims==" %%a in ('findstr /b "brand.version=" branding.properties 2^>nul') do set BRAND_VERSION=%%b

echo ============================================
echo   Project:  %BRAND_NAME%
echo   Version:  %BRAND_VERSION%
echo   Mode:     Native Image (GraalVM)
echo ============================================
echo.

call mvn -Pnative clean package --batch-mode --no-transfer-progress
if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful!
    echo Output: target\%BRAND_NAME%-%BRAND_VERSION%-double-click-me.exe
    echo Output: target\toolscreen-downloader.exe
    echo.
) else (
    echo.
    echo Build failed!
    exit /b 1
)
