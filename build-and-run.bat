@echo off
setlocal enabledelayedexpansion

REM WuKongIM Android EasySDK - Build and Run Script (Windows)
REM This script automates building the SDK, example app, and running it on a device/emulator

REM Script configuration
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%"
set "SDK_MODULE=."
set "EXAMPLE_MODULE=example"
set "PACKAGE_NAME=com.githubim.easysdk.example"
set "ACTIVITY_NAME=com.githubim.easysdk.example.MainActivity"

REM Command line options
set "CLEAN_BUILD=false"
set "SHOW_LOGS_AFTER=false"
set "SDK_ONLY=false"
set "NO_RUN=false"

REM Parse command line arguments
:parse_args
if "%~1"=="" goto :args_done
if /i "%~1"=="-h" goto :show_usage
if /i "%~1"=="--help" goto :show_usage
if /i "%~1"=="-c" set "CLEAN_BUILD=true" & shift & goto :parse_args
if /i "%~1"=="--clean" set "CLEAN_BUILD=true" & shift & goto :parse_args
if /i "%~1"=="-l" set "SHOW_LOGS_AFTER=true" & shift & goto :parse_args
if /i "%~1"=="--logs" set "SHOW_LOGS_AFTER=true" & shift & goto :parse_args
if /i "%~1"=="--sdk-only" set "SDK_ONLY=true" & shift & goto :parse_args
if /i "%~1"=="--no-run" set "NO_RUN=true" & shift & goto :parse_args
echo [ERROR] Unknown option: %~1
goto :show_usage

:args_done

REM Change to project directory
cd /d "%PROJECT_ROOT%"

call :print_header "WuKongIM Android EasySDK - Build and Run"
echo [INFO] Project directory: %PROJECT_ROOT%

REM Execute build steps
call :check_prerequisites
if errorlevel 1 exit /b 1

if "%CLEAN_BUILD%"=="true" (
    call :clean_project
    if errorlevel 1 exit /b 1
)

call :build_sdk
if errorlevel 1 exit /b 1

if "%SDK_ONLY%"=="false" (
    call :build_example
    if errorlevel 1 exit /b 1
    
    if "%NO_RUN%"=="false" (
        call :check_devices
        if errorlevel 1 exit /b 1
        
        call :install_and_run
        if errorlevel 1 exit /b 1
        
        if "%SHOW_LOGS_AFTER%"=="true" (
            echo.
            pause
            call :show_logs
        )
    )
)

echo [SUCCESS] Script completed successfully!
exit /b 0

REM Function to print colored headers
:print_header
echo.
echo ================================
echo  %~1
echo ================================
echo.
goto :eof

REM Function to check prerequisites
:check_prerequisites
call :print_header "Checking Prerequisites"

set "MISSING_TOOLS="

REM Check for Java
java -version >nul 2>&1
if errorlevel 1 (
    set "MISSING_TOOLS=!MISSING_TOOLS! Java-JDK-8-or-higher"
) else (
    for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr "version"') do (
        echo [INFO] Java version: %%i
        goto :java_found
    )
    :java_found
)

REM Check for Android SDK
if "%ANDROID_HOME%"=="" if "%ANDROID_SDK_ROOT%"=="" (
    set "MISSING_TOOLS=!MISSING_TOOLS! Android-SDK"
) else (
    if not "%ANDROID_HOME%"=="" (
        echo [INFO] Android SDK: %ANDROID_HOME%
    ) else (
        echo [INFO] Android SDK: %ANDROID_SDK_ROOT%
    )
)

REM Check for ADB
adb version >nul 2>&1
if errorlevel 1 (
    set "MISSING_TOOLS=!MISSING_TOOLS! Android-Debug-Bridge"
) else (
    for /f "tokens=*" %%i in ('adb version 2^>^&1 ^| findstr "Android Debug Bridge"') do (
        echo [INFO] ADB: %%i
        goto :adb_found
    )
    :adb_found
)

REM Check for Gradle wrapper or Gradle
if exist "%PROJECT_ROOT%\gradlew.bat" (
    echo [INFO] Using Gradle wrapper: %PROJECT_ROOT%\gradlew.bat
    set "GRADLE_CMD=%PROJECT_ROOT%\gradlew.bat"
) else (
    gradle --version >nul 2>&1
    if errorlevel 1 (
        set "MISSING_TOOLS=!MISSING_TOOLS! Gradle-or-Gradle-wrapper"
    ) else (
        for /f "tokens=*" %%i in ('gradle --version 2^>^&1 ^| findstr "Gradle"') do (
            echo [INFO] Using system Gradle: %%i
            set "GRADLE_CMD=gradle"
            goto :gradle_found
        )
        :gradle_found
    )
)

REM Report missing tools
if not "!MISSING_TOOLS!"=="" (
    echo [ERROR] Missing required tools:
    for %%i in (!MISSING_TOOLS!) do (
        echo   X %%i
    )
    echo.
    echo [INFO] Please install the missing tools and try again.
    echo [INFO] Installation guide: https://developer.android.com/studio/install
    exit /b 1
)

echo [SUCCESS] All prerequisites are satisfied!
goto :eof

REM Function to check connected devices
:check_devices
call :print_header "Checking Connected Devices"

REM Start ADB server if not running
adb start-server >nul 2>&1

REM Get list of devices
for /f "skip=1 tokens=1,2" %%i in ('adb devices 2^>nul') do (
    if "%%j"=="device" set "DEVICES=!DEVICES! %%i"
    if "%%j"=="emulator" set "DEVICES=!DEVICES! %%i"
)

REM Count devices
set "DEVICE_COUNT=0"
for %%i in (%DEVICES%) do set /a DEVICE_COUNT+=1

if %DEVICE_COUNT%==0 (
    echo [ERROR] No connected devices or emulators found!
    echo.
    echo [INFO] To run the example app, you need either:
    echo [INFO] 1. A physical Android device connected via USB with USB debugging enabled
    echo [INFO] 2. An Android emulator running
    echo.
    echo [INFO] To start an emulator:
    echo [INFO]   emulator -avd ^<avd_name^>
    echo [INFO]   Or use Android Studio: Tools ^> AVD Manager
    echo.
    echo [INFO] To connect a physical device:
    echo [INFO]   1. Enable Developer Options: Settings ^> About ^> Tap Build Number 7 times
    echo [INFO]   2. Enable USB Debugging: Settings ^> Developer Options ^> USB Debugging
    echo [INFO]   3. Connect device via USB and authorize the computer
    exit /b 1
) else if %DEVICE_COUNT%==1 (
    for %%i in (%DEVICES%) do (
        set "SELECTED_DEVICE=%%i"
        for /f "tokens=*" %%j in ('adb -s %%i shell getprop ro.product.model 2^>nul') do (
            echo [SUCCESS] Found 1 device: %%i ^(%%j^)
        )
    )
) else (
    echo [INFO] Found %DEVICE_COUNT% devices:
    set "INDEX=1"
    for %%i in (%DEVICES%) do (
        for /f "tokens=*" %%j in ('adb -s %%i shell getprop ro.product.model 2^>nul') do (
            echo   !INDEX!^) %%i ^(%%j^)
        )
        set /a INDEX+=1
    )
    echo.
    set /p "SELECTION=Select device (1-%DEVICE_COUNT%): "
    
    REM Validate selection and set device
    if !SELECTION! geq 1 if !SELECTION! leq %DEVICE_COUNT% (
        set "INDEX=1"
        for %%i in (%DEVICES%) do (
            if !INDEX!==!SELECTION! (
                set "SELECTED_DEVICE=%%i"
                echo [SUCCESS] Selected device: %%i
                goto :device_selected
            )
            set /a INDEX+=1
        )
        :device_selected
    ) else (
        echo [ERROR] Invalid selection. Exiting.
        exit /b 1
    )
)
goto :eof

REM Function to clean project
:clean_project
call :print_header "Cleaning Project"

echo [INFO] Cleaning previous builds...
%GRADLE_CMD% clean
if errorlevel 1 (
    echo [ERROR] Failed to clean project
    exit /b 1
)
echo [SUCCESS] Project cleaned successfully
goto :eof

REM Function to build SDK
:build_sdk
call :print_header "Building WuKongIM Android EasySDK"

echo [INFO] Building SDK library module...
%GRADLE_CMD% :assembleDebug
if errorlevel 1 (
    echo [ERROR] Failed to build SDK
    echo [INFO] Check the error messages above for details
    exit /b 1
)
echo [SUCCESS] SDK built successfully
goto :eof

REM Function to build example app
:build_example
call :print_header "Building Example Application"

echo [INFO] Building example application...
%GRADLE_CMD% :example:assembleDebug
if errorlevel 1 (
    echo [ERROR] Failed to build example app
    echo [INFO] Check the error messages above for details
    exit /b 1
)
echo [SUCCESS] Example app built successfully

REM Find the APK file
for /r "%PROJECT_ROOT%\example\build\outputs\apk\debug" %%i in (*.apk) do (
    set "APK_PATH=%%i"
    echo [INFO] APK location: %%i
    goto :apk_found
)
echo [WARNING] Could not locate APK file
:apk_found
goto :eof

REM Function to install and run app
:install_and_run
call :print_header "Installing and Running Example App"

if "%APK_PATH%"=="" (
    echo [ERROR] APK file not found. Build may have failed.
    exit /b 1
)

if not exist "%APK_PATH%" (
    echo [ERROR] APK file not found: %APK_PATH%
    exit /b 1
)

echo [INFO] Installing app on device: %SELECTED_DEVICE%
adb -s %SELECTED_DEVICE% install -r "%APK_PATH%"
if errorlevel 1 (
    echo [ERROR] Failed to install app
    exit /b 1
)
echo [SUCCESS] App installed successfully

echo [INFO] Launching app...
adb -s %SELECTED_DEVICE% shell am start -n %PACKAGE_NAME%/%ACTIVITY_NAME%
if errorlevel 1 (
    echo [ERROR] Failed to launch app
    exit /b 1
)
echo [SUCCESS] App launched successfully
echo [INFO] The WuKongIM EasySDK Example app should now be running on your device
goto :eof

REM Function to show logs
:show_logs
call :print_header "Application Logs"

echo [INFO] Showing application logs (press Ctrl+C to stop)...
echo [INFO] Filter: %PACKAGE_NAME%
echo.

REM Clear existing logs and show new ones
adb -s %SELECTED_DEVICE% logcat -c
adb -s %SELECTED_DEVICE% logcat | findstr "%PACKAGE_NAME% WuKongExample WuKongEasySDK"
goto :eof

REM Function to display usage
:show_usage
echo WuKongIM Android EasySDK - Build and Run Script (Windows)
echo.
echo Usage: %~nx0 [OPTIONS]
echo.
echo Options:
echo   -h, --help     Show this help message
echo   -c, --clean    Clean project before building
echo   -l, --logs     Show application logs after launching
echo   --sdk-only     Build only the SDK (skip example app)
echo   --no-run       Build but don't install/run the app
echo.
echo Examples:
echo   %~nx0                    # Build and run with default options
echo   %~nx0 --clean           # Clean, build, and run
echo   %~nx0 --clean --logs    # Clean, build, run, and show logs
echo   %~nx0 --sdk-only        # Build only the SDK library
echo   %~nx0 --no-run          # Build but don't install/run
echo.
exit /b 0
