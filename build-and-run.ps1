# WuKongIM Android EasySDK - Build and Run Script (PowerShell)
# This script automates building the SDK, example app, and running it on a device/emulator

param(
    [switch]$Help,
    [switch]$Clean,
    [switch]$Logs,
    [switch]$SdkOnly,
    [switch]$NoRun
)

# Script configuration
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = $ScriptDir
$SdkModule = "."
$ExampleModule = "example"
$PackageName = "com.githubim.easysdk.example"
$ActivityName = "com.githubim.easysdk.example.MainActivity"

# Colors for output
$Colors = @{
    Info = "Cyan"
    Success = "Green"
    Warning = "Yellow"
    Error = "Red"
    Header = "Blue"
}

# Function to print colored output
function Write-Info($Message) {
    Write-Host "[INFO] $Message" -ForegroundColor $Colors.Info
}

function Write-Success($Message) {
    Write-Host "[SUCCESS] $Message" -ForegroundColor $Colors.Success
}

function Write-Warning($Message) {
    Write-Host "[WARNING] $Message" -ForegroundColor $Colors.Warning
}

function Write-Error($Message) {
    Write-Host "[ERROR] $Message" -ForegroundColor $Colors.Error
}

function Write-Header($Message) {
    Write-Host ""
    Write-Host "================================" -ForegroundColor $Colors.Header
    Write-Host " $Message" -ForegroundColor $Colors.Header
    Write-Host "================================" -ForegroundColor $Colors.Header
    Write-Host ""
}

# Function to check if command exists
function Test-Command($Command) {
    try {
        Get-Command $Command -ErrorAction Stop | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

# Function to check prerequisites
function Test-Prerequisites {
    Write-Header "Checking Prerequisites"
    
    $MissingTools = @()
    
    # Check for Java
    if (-not (Test-Command "java")) {
        $MissingTools += "Java JDK 8 or higher"
    } else {
        $JavaVersion = (java -version 2>&1 | Select-String "version" | Select-Object -First 1).ToString()
        Write-Info "Java: $JavaVersion"
    }
    
    # Check for Android SDK
    if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
        $MissingTools += "Android SDK (set ANDROID_HOME or ANDROID_SDK_ROOT)"
    } else {
        $AndroidHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }
        Write-Info "Android SDK: $AndroidHome"
    }
    
    # Check for ADB
    if (-not (Test-Command "adb")) {
        $MissingTools += "Android Debug Bridge (adb)"
    } else {
        $AdbVersion = (adb version | Select-Object -First 1).ToString()
        Write-Info "ADB: $AdbVersion"
    }
    
    # Check for Gradle wrapper or Gradle
    $GradleWrapper = Join-Path $ProjectRoot "gradlew.bat"
    if (Test-Path $GradleWrapper) {
        Write-Info "Using Gradle wrapper: $GradleWrapper"
        $script:GradleCmd = $GradleWrapper
    } elseif (Test-Command "gradle") {
        $GradleVersion = (gradle --version | Select-String "Gradle" | Select-Object -First 1).ToString()
        Write-Info "Using system Gradle: $GradleVersion"
        $script:GradleCmd = "gradle"
    } else {
        $MissingTools += "Gradle or Gradle wrapper"
    }
    
    # Report missing tools
    if ($MissingTools.Count -gt 0) {
        Write-Error "Missing required tools:"
        foreach ($Tool in $MissingTools) {
            Write-Host "  ✗ $Tool" -ForegroundColor $Colors.Error
        }
        Write-Host ""
        Write-Info "Please install the missing tools and try again."
        Write-Info "Installation guide: https://developer.android.com/studio/install"
        exit 1
    }
    
    Write-Success "All prerequisites are satisfied!"
}

# Function to check connected devices
function Test-Devices {
    Write-Header "Checking Connected Devices"
    
    # Start ADB server if not running
    adb start-server | Out-Null
    
    # Get list of devices
    $DeviceOutput = adb devices 2>$null | Select-Object -Skip 1 | Where-Object { $_ -match "device$|emulator$" }
    $Devices = @()
    foreach ($Line in $DeviceOutput) {
        if ($Line -match "^(\S+)\s+(device|emulator)$") {
            $Devices += $Matches[1]
        }
    }
    
    if ($Devices.Count -eq 0) {
        Write-Error "No connected devices or emulators found!"
        Write-Host ""
        Write-Info "To run the example app, you need either:"
        Write-Info "1. A physical Android device connected via USB with USB debugging enabled"
        Write-Info "2. An Android emulator running"
        Write-Host ""
        Write-Info "To start an emulator:"
        Write-Info "  emulator -avd <avd_name>"
        Write-Info "  Or use Android Studio: Tools > AVD Manager"
        Write-Host ""
        Write-Info "To connect a physical device:"
        Write-Info "  1. Enable Developer Options: Settings > About > Tap Build Number 7 times"
        Write-Info "  2. Enable USB Debugging: Settings > Developer Options > USB Debugging"
        Write-Info "  3. Connect device via USB and authorize the computer"
        exit 1
    } elseif ($Devices.Count -eq 1) {
        $script:SelectedDevice = $Devices[0]
        $DeviceInfo = (adb -s $script:SelectedDevice shell getprop ro.product.model 2>$null) -replace "`r|`n", ""
        Write-Success "Found 1 device: $($script:SelectedDevice) ($DeviceInfo)"
    } else {
        Write-Info "Found $($Devices.Count) devices:"
        for ($i = 0; $i -lt $Devices.Count; $i++) {
            $DeviceInfo = (adb -s $Devices[$i] shell getprop ro.product.model 2>$null) -replace "`r|`n", ""
            Write-Host "  $($i + 1)) $($Devices[$i]) ($DeviceInfo)" -ForegroundColor $Colors.Header
        }
        
        Write-Host ""
        $Selection = Read-Host "Select device (1-$($Devices.Count))"
        
        if ($Selection -match "^\d+$" -and [int]$Selection -ge 1 -and [int]$Selection -le $Devices.Count) {
            $script:SelectedDevice = $Devices[[int]$Selection - 1]
            Write-Success "Selected device: $($script:SelectedDevice)"
        } else {
            Write-Error "Invalid selection. Exiting."
            exit 1
        }
    }
}

# Function to clean project
function Invoke-CleanProject {
    Write-Header "Cleaning Project"
    
    Write-Info "Cleaning previous builds..."
    $Result = & $script:GradleCmd clean
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to clean project"
        exit 1
    }
    Write-Success "Project cleaned successfully"
}

# Function to build SDK
function Invoke-BuildSdk {
    Write-Header "Building WuKongIM Android EasySDK"
    
    Write-Info "Building SDK library module..."
    $Result = & $script:GradleCmd ":assembleDebug"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to build SDK"
        Write-Info "Check the error messages above for details"
        exit 1
    }
    Write-Success "SDK built successfully"
}

# Function to build example app
function Invoke-BuildExample {
    Write-Header "Building Example Application"
    
    Write-Info "Building example application..."
    $Result = & $script:GradleCmd ":example:assembleDebug"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to build example app"
        Write-Info "Check the error messages above for details"
        exit 1
    }
    Write-Success "Example app built successfully"
    
    # Find the APK file
    $ApkPath = Get-ChildItem -Path "$ProjectRoot\example\build\outputs\apk\debug" -Filter "*.apk" -Recurse | Select-Object -First 1
    if ($ApkPath) {
        $script:ApkPath = $ApkPath.FullName
        Write-Info "APK location: $($script:ApkPath)"
    } else {
        Write-Warning "Could not locate APK file"
    }
}

# Function to install and run app
function Invoke-InstallAndRun {
    Write-Header "Installing and Running Example App"
    
    if (-not $script:ApkPath -or -not (Test-Path $script:ApkPath)) {
        Write-Error "APK file not found. Build may have failed."
        exit 1
    }
    
    Write-Info "Installing app on device: $($script:SelectedDevice)"
    $Result = adb -s $script:SelectedDevice install -r $script:ApkPath
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to install app"
        exit 1
    }
    Write-Success "App installed successfully"
    
    Write-Info "Launching app..."
    $Result = adb -s $script:SelectedDevice shell am start -n "$PackageName/$ActivityName"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Failed to launch app"
        exit 1
    }
    Write-Success "App launched successfully"
    Write-Info "The WuKongIM EasySDK Example app should now be running on your device"
}

# Function to show logs
function Show-Logs {
    Write-Header "Application Logs"
    
    Write-Info "Showing application logs (press Ctrl+C to stop)..."
    Write-Info "Filter: $PackageName"
    Write-Host ""
    
    # Clear existing logs and show new ones
    adb -s $script:SelectedDevice logcat -c
    adb -s $script:SelectedDevice logcat | Select-String "$PackageName|WuKongExample|WuKongEasySDK"
}

# Function to display usage
function Show-Usage {
    Write-Host "WuKongIM Android EasySDK - Build and Run Script (PowerShell)"
    Write-Host ""
    Write-Host "Usage: .\build-and-run.ps1 [OPTIONS]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Help      Show this help message"
    Write-Host "  -Clean     Clean project before building"
    Write-Host "  -Logs      Show application logs after launching"
    Write-Host "  -SdkOnly   Build only the SDK (skip example app)"
    Write-Host "  -NoRun     Build but don't install/run the app"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\build-and-run.ps1                # Build and run with default options"
    Write-Host "  .\build-and-run.ps1 -Clean         # Clean, build, and run"
    Write-Host "  .\build-and-run.ps1 -Clean -Logs   # Clean, build, run, and show logs"
    Write-Host "  .\build-and-run.ps1 -SdkOnly       # Build only the SDK library"
    Write-Host "  .\build-and-run.ps1 -NoRun         # Build but don't install/run"
    Write-Host ""
}

# Main execution
if ($Help) {
    Show-Usage
    exit 0
}

# Change to project directory
Set-Location $ProjectRoot

Write-Header "WuKongIM Android EasySDK - Build and Run"
Write-Info "Project directory: $ProjectRoot"

# Execute build steps
Test-Prerequisites

if ($Clean) {
    Invoke-CleanProject
}

Invoke-BuildSdk

if (-not $SdkOnly) {
    Invoke-BuildExample
    
    if (-not $NoRun) {
        Test-Devices
        Invoke-InstallAndRun
        
        if ($Logs) {
            Write-Host ""
            Read-Host "Press Enter to start showing logs (or Ctrl+C to exit)"
            Show-Logs
        }
    }
}

Write-Success "Script completed successfully!"
