# WuKongIM Android EasySDK - Build and Run Scripts

This directory contains comprehensive build and run scripts that automate the entire process of building the WuKongIM Android EasySDK and running the example application on Android devices or emulators.

## Available Scripts

### 🐧 Unix/Linux/macOS: `build-and-run.sh`
Bash script for Unix-like systems (Linux, macOS, WSL)

### 🪟 Windows: `build-and-run.bat`
Batch script for Windows Command Prompt

### 💙 Windows PowerShell: `build-and-run.ps1`
PowerShell script for Windows PowerShell/PowerShell Core

## Prerequisites

Before running any script, ensure you have the following installed:

### Required Tools
- **Java JDK 8 or higher**
- **Android SDK** (via Android Studio or standalone)
- **Android Debug Bridge (ADB)** (included with Android SDK)
- **Gradle** (or use the included Gradle wrapper)

### Environment Variables
- `ANDROID_HOME` or `ANDROID_SDK_ROOT` pointing to your Android SDK installation

### Connected Device/Emulator
- Physical Android device with USB debugging enabled, OR
- Running Android emulator

## Quick Start

### Linux/macOS/Unix
```bash
# Make script executable
chmod +x build-and-run.sh

# Run with default options
./build-and-run.sh

# Clean build and show logs
./build-and-run.sh --clean --logs
```

### Windows Command Prompt
```cmd
# Run with default options
build-and-run.bat

# Clean build and show logs
build-and-run.bat --clean --logs
```

### Windows PowerShell
```powershell
# Set execution policy (if needed)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# Run with default options
.\build-and-run.ps1

# Clean build and show logs
.\build-and-run.ps1 -Clean -Logs
```

## Script Options

All scripts support the same command-line options:

| Option | Description |
|--------|-------------|
| `-h, --help` | Show help message and usage examples |
| `-c, --clean` | Clean project before building |
| `-l, --logs` | Show application logs after launching |
| `--sdk-only` | Build only the SDK library (skip example app) |
| `--no-run` | Build but don't install/run the app |

### PowerShell Syntax
PowerShell uses different syntax for parameters:
- `-Help` instead of `--help`
- `-Clean` instead of `--clean`
- `-Logs` instead of `--logs`
- `-SdkOnly` instead of `--sdk-only`
- `-NoRun` instead of `--no-run`

## Usage Examples

### Basic Usage
```bash
# Build SDK and example app, then install and run
./build-and-run.sh
```

### Clean Build
```bash
# Clean previous builds, then build and run
./build-and-run.sh --clean
```

### Build and Monitor Logs
```bash
# Build, run, and show real-time application logs
./build-and-run.sh --logs
```

### SDK Development
```bash
# Build only the SDK library (for SDK development)
./build-and-run.sh --sdk-only
```

### CI/CD Pipeline
```bash
# Build but don't install/run (useful for CI/CD)
./build-and-run.sh --no-run
```

### Complete Development Workflow
```bash
# Clean build with logs for development
./build-and-run.sh --clean --logs
```

## What the Scripts Do

### 1. Prerequisites Check ✅
- Verifies Java JDK installation
- Checks Android SDK configuration
- Validates ADB availability
- Confirms Gradle/Gradle wrapper presence
- Reports missing tools with installation guidance

### 2. Device Detection 📱
- Scans for connected Android devices and emulators
- Handles multiple devices with interactive selection
- Provides clear instructions for device setup
- Validates device connectivity

### 3. Build Process 🔨
- **Clean** (optional): Removes previous build artifacts
- **SDK Build**: Compiles the WuKongIM Android EasySDK library
- **Example Build**: Compiles the example application
- **Error Handling**: Clear error messages and build failure guidance

### 4. Installation and Launch 🚀
- Installs the example APK on the selected device
- Launches the application automatically
- Provides success confirmation and next steps

### 5. Log Monitoring 📊
- **Real-time Logs**: Shows filtered application logs
- **Log Filtering**: Focuses on WuKongIM-related log entries
- **Interactive**: Easy to start/stop log monitoring

## Error Handling

The scripts include comprehensive error handling for common scenarios:

### Missing Prerequisites
```
[ERROR] Missing required tools:
  ✗ Java JDK 8 or higher
  ✗ Android SDK (set ANDROID_HOME or ANDROID_SDK_ROOT)

[INFO] Please install the missing tools and try again.
[INFO] Installation guide: https://developer.android.com/studio/install
```

### No Connected Devices
```
[ERROR] No connected devices or emulators found!

[INFO] To run the example app, you need either:
[INFO] 1. A physical Android device connected via USB with USB debugging enabled
[INFO] 2. An Android emulator running

[INFO] To start an emulator:
[INFO]   emulator -avd <avd_name>
[INFO]   Or use Android Studio: Tools > AVD Manager
```

### Build Failures
```
[ERROR] Failed to build SDK
[INFO] Check the error messages above for details
```

## Device Setup Instructions

### Physical Device Setup
1. **Enable Developer Options**:
   - Go to Settings > About Phone
   - Tap "Build Number" 7 times
   - Developer Options will appear in Settings

2. **Enable USB Debugging**:
   - Go to Settings > Developer Options
   - Enable "USB Debugging"

3. **Connect and Authorize**:
   - Connect device via USB
   - Authorize the computer when prompted

### Emulator Setup
1. **Using Android Studio**:
   - Open Android Studio
   - Go to Tools > AVD Manager
   - Create and start an emulator

2. **Using Command Line**:
   ```bash
   # List available AVDs
   emulator -list-avds
   
   # Start an emulator
   emulator -avd <avd_name>
   ```

## Troubleshooting

### Common Issues

**"Command not found" errors**:
- Ensure Android SDK is properly installed
- Verify PATH includes Android SDK tools
- Check ANDROID_HOME/ANDROID_SDK_ROOT environment variables

**"No devices found"**:
- Check USB connection for physical devices
- Verify USB debugging is enabled
- Restart ADB: `adb kill-server && adb start-server`
- For emulators, ensure they're fully booted

**Build failures**:
- Check internet connection (for dependency downloads)
- Verify Gradle wrapper permissions: `chmod +x gradlew`
- Clean project: use `--clean` option

**Permission denied (Linux/macOS)**:
```bash
chmod +x build-and-run.sh
```

**PowerShell execution policy (Windows)**:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Getting Help

1. **Run with help option**:
   ```bash
   ./build-and-run.sh --help
   ```

2. **Check prerequisites**:
   ```bash
   ./build-and-run.sh --sdk-only
   ```

3. **Verbose output**: Scripts provide detailed logging for troubleshooting

## Integration with Development Workflow

### IDE Integration
You can run these scripts from your IDE's terminal or integrate them into build configurations.

### CI/CD Integration
Use the `--no-run` option for continuous integration:
```bash
./build-and-run.sh --clean --no-run
```

### Development Cycle
```bash
# During development
./build-and-run.sh --clean --logs

# For testing
./build-and-run.sh --logs

# For SDK-only changes
./build-and-run.sh --sdk-only
```

## Script Maintenance

The scripts are designed to be:
- **Self-contained**: No external dependencies beyond standard tools
- **Cross-platform**: Consistent behavior across different operating systems
- **Maintainable**: Clear structure and comprehensive error handling
- **Extensible**: Easy to modify for additional functionality

For issues or improvements, please refer to the project's issue tracker or contribute directly to the script files.
