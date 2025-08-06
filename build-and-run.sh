#!/bin/bash

# WuKongIM Android EasySDK - Build and Run Script
# This script automates building the SDK, example app, and running it on a device/emulator
# Compatible with: Linux, macOS, Unix-like systems

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
SDK_MODULE="."
EXAMPLE_MODULE="example"
PACKAGE_NAME="com.wukongim.easysdk.example"
ACTIVITY_NAME="com.wukongim.easysdk.example.MainActivity"

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "\n${BLUE}================================${NC}"
    echo -e "${BLUE} $1${NC}"
    echo -e "${BLUE}================================${NC}\n"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    local missing_tools=()
    
    # Check for Java
    if ! command_exists java; then
        missing_tools+=("Java JDK 8 or higher")
    else
        java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        print_info "Java version: $java_version"
    fi
    
    # Check for Android SDK
    if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
        missing_tools+=("Android SDK (set ANDROID_HOME or ANDROID_SDK_ROOT)")
    else
        android_home="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
        print_info "Android SDK: $android_home"
    fi
    
    # Check for ADB
    if ! command_exists adb; then
        missing_tools+=("Android Debug Bridge (adb)")
    else
        adb_version=$(adb version | head -n 1)
        print_info "ADB: $adb_version"
    fi
    
    # Check for Gradle wrapper or Gradle
    if [ -f "$PROJECT_ROOT/gradlew" ]; then
        print_info "Using Gradle wrapper: $PROJECT_ROOT/gradlew"
        GRADLE_CMD="$PROJECT_ROOT/gradlew"
    elif command_exists gradle; then
        gradle_version=$(gradle --version | grep "Gradle" | head -n 1)
        print_info "Using system Gradle: $gradle_version"
        GRADLE_CMD="gradle"
    else
        missing_tools+=("Gradle or Gradle wrapper")
    fi
    
    # Report missing tools
    if [ ${#missing_tools[@]} -ne 0 ]; then
        print_error "Missing required tools:"
        for tool in "${missing_tools[@]}"; do
            echo -e "  ${RED}✗${NC} $tool"
        done
        echo
        print_info "Please install the missing tools and try again."
        print_info "Installation guide: https://developer.android.com/studio/install"
        exit 1
    fi
    
    print_success "All prerequisites are satisfied!"
}

# Function to check connected devices
check_devices() {
    print_header "Checking Connected Devices"
    
    # Start ADB server if not running
    adb start-server >/dev/null 2>&1
    
    # Get list of devices
    devices=$(adb devices | grep -v "List of devices" | grep -E "device$|emulator$" | awk '{print $1}')
    if [ -z "$devices" ]; then
        device_count=0
    else
        device_count=$(echo "$devices" | wc -l | tr -d ' ')
    fi
    
    if [ "$device_count" -eq 0 ]; then
        print_error "No connected devices or emulators found!"
        echo
        print_info "To run the example app, you need either:"
        print_info "1. A physical Android device connected via USB with USB debugging enabled"
        print_info "2. An Android emulator running"
        echo
        print_info "To start an emulator:"
        print_info "  \$ emulator -avd <avd_name>"
        print_info "  Or use Android Studio: Tools > AVD Manager"
        echo
        print_info "To connect a physical device:"
        print_info "  1. Enable Developer Options: Settings > About > Tap Build Number 7 times"
        print_info "  2. Enable USB Debugging: Settings > Developer Options > USB Debugging"
        print_info "  3. Connect device via USB and authorize the computer"
        exit 1
    elif [ "$device_count" -eq 1 ]; then
        SELECTED_DEVICE="$devices"
        device_info=$(adb -s "$SELECTED_DEVICE" shell getprop ro.product.model 2>/dev/null || echo "Unknown Device")
        print_success "Found 1 device: $SELECTED_DEVICE ($device_info)"
    else
        print_info "Found $device_count devices:"
        i=1
        device_array=()
        while IFS= read -r device; do
            if [ -n "$device" ]; then
                device_info=$(adb -s "$device" shell getprop ro.product.model 2>/dev/null || echo "Unknown Device")
                echo -e "  ${BLUE}$i)${NC} $device ($device_info)"
                device_array+=("$device")
                ((i++))
            fi
        done <<< "$devices"
        
        echo
        read -p "Select device (1-$device_count): " selection
        
        if [[ "$selection" =~ ^[0-9]+$ ]] && [ "$selection" -ge 1 ] && [ "$selection" -le "$device_count" ]; then
            SELECTED_DEVICE="${device_array[$((selection-1))]}"
            print_success "Selected device: $SELECTED_DEVICE"
        else
            print_error "Invalid selection. Exiting."
            exit 1
        fi
    fi
}

# Function to clean project
clean_project() {
    print_header "Cleaning Project"
    
    print_info "Cleaning previous builds..."
    if $GRADLE_CMD clean; then
        print_success "Project cleaned successfully"
    else
        print_error "Failed to clean project"
        exit 1
    fi
}

# Function to build SDK
build_sdk() {
    print_header "Building WuKongIM Android EasySDK"
    
    print_info "Building SDK library module..."
    if $GRADLE_CMD :assembleDebug; then
        print_success "SDK built successfully"
    else
        print_error "Failed to build SDK"
        print_info "Check the error messages above for details"
        exit 1
    fi
}

# Function to build example app
build_example() {
    print_header "Building Example Application"
    
    print_info "Building example application..."
    if $GRADLE_CMD :example:assembleDebug; then
        print_success "Example app built successfully"
        
        # Find the APK file
        APK_PATH=$(find "$PROJECT_ROOT/example/build/outputs/apk/debug" -name "*.apk" | head -n 1)
        if [ -n "$APK_PATH" ]; then
            print_info "APK location: $APK_PATH"
        else
            print_warning "Could not locate APK file"
        fi
    else
        print_error "Failed to build example app"
        print_info "Check the error messages above for details"
        exit 1
    fi
}

# Function to install and run app
install_and_run() {
    print_header "Installing and Running Example App"
    
    if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
        print_error "APK file not found. Build may have failed."
        exit 1
    fi
    
    print_info "Installing app on device: $SELECTED_DEVICE"
    if adb -s "$SELECTED_DEVICE" install -r "$APK_PATH"; then
        print_success "App installed successfully"
    else
        print_error "Failed to install app"
        exit 1
    fi
    
    print_info "Launching app..."
    if adb -s "$SELECTED_DEVICE" shell am start -n "$PACKAGE_NAME/$ACTIVITY_NAME"; then
        print_success "App launched successfully"
        print_info "The WuKongIM EasySDK Example app should now be running on your device"
    else
        print_error "Failed to launch app"
        exit 1
    fi
}

# Function to show logs
show_logs() {
    print_header "Application Logs"
    
    print_info "Showing application logs (press Ctrl+C to stop)..."
    print_info "Filter: $PACKAGE_NAME"
    echo
    
    # Clear existing logs and show new ones
    adb -s "$SELECTED_DEVICE" logcat -c
    adb -s "$SELECTED_DEVICE" logcat | grep "$PACKAGE_NAME\|WuKongExample\|WuKongEasySDK"
}

# Function to display usage
show_usage() {
    echo "WuKongIM Android EasySDK - Build and Run Script"
    echo
    echo "Usage: $0 [OPTIONS]"
    echo
    echo "Options:"
    echo "  -h, --help     Show this help message"
    echo "  -c, --clean    Clean project before building"
    echo "  -l, --logs     Show application logs after launching"
    echo "  --sdk-only     Build only the SDK (skip example app)"
    echo "  --no-run       Build but don't install/run the app"
    echo
    echo "Examples:"
    echo "  $0                    # Build and run with default options"
    echo "  $0 --clean           # Clean, build, and run"
    echo "  $0 --clean --logs    # Clean, build, run, and show logs"
    echo "  $0 --sdk-only        # Build only the SDK library"
    echo "  $0 --no-run          # Build but don't install/run"
    echo
}

# Main execution function
main() {
    local clean_build=false
    local show_logs_after=false
    local sdk_only=false
    local no_run=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_usage
                exit 0
                ;;
            -c|--clean)
                clean_build=true
                shift
                ;;
            -l|--logs)
                show_logs_after=true
                shift
                ;;
            --sdk-only)
                sdk_only=true
                shift
                ;;
            --no-run)
                no_run=true
                shift
                ;;
            *)
                print_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # Change to project directory
    cd "$PROJECT_ROOT"
    
    print_header "WuKongIM Android EasySDK - Build and Run"
    print_info "Project directory: $PROJECT_ROOT"
    
    # Execute build steps
    check_prerequisites
    
    if [ "$clean_build" = true ]; then
        clean_project
    fi
    
    build_sdk
    
    if [ "$sdk_only" = false ]; then
        build_example
        
        if [ "$no_run" = false ]; then
            check_devices
            install_and_run
            
            if [ "$show_logs_after" = true ]; then
                echo
                read -p "Press Enter to start showing logs (or Ctrl+C to exit)..."
                show_logs
            fi
        fi
    fi
    
    print_success "Script completed successfully!"
}

# Run main function with all arguments
main "$@"
