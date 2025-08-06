# WuKongIM Android EasySDK - Makefile
# Provides convenient shortcuts for common build tasks

.PHONY: help build clean run logs sdk install check

# Default target
help:
	@echo "WuKongIM Android EasySDK - Build Targets"
	@echo ""
	@echo "Available targets:"
	@echo "  help     - Show this help message"
	@echo "  check    - Check prerequisites"
	@echo "  clean    - Clean project"
	@echo "  sdk      - Build SDK only"
	@echo "  build    - Build SDK and example app"
	@echo "  install  - Build and install on device"
	@echo "  run      - Build, install, and run (default workflow)"
	@echo "  logs     - Build, run, and show logs"
	@echo ""
	@echo "Examples:"
	@echo "  make run     # Build and run example app"
	@echo "  make logs    # Build, run, and monitor logs"
	@echo "  make sdk     # Build only the SDK library"
	@echo ""

# Check prerequisites
check:
	@echo "Checking prerequisites..."
	@./build-and-run.sh --sdk-only

# Clean project
clean:
	@echo "Cleaning project..."
	@./build-and-run.sh --clean --no-run

# Build SDK only
sdk:
	@echo "Building SDK..."
	@./build-and-run.sh --sdk-only

# Build SDK and example app
build:
	@echo "Building SDK and example app..."
	@./build-and-run.sh --no-run

# Build and install on device
install:
	@echo "Building and installing..."
	@./build-and-run.sh

# Build, install, and run (default workflow)
run: install

# Build, run, and show logs
logs:
	@echo "Building, running, and showing logs..."
	@./build-and-run.sh --logs

# Clean build and run
clean-run:
	@echo "Clean build and run..."
	@./build-and-run.sh --clean

# Clean build with logs
clean-logs:
	@echo "Clean build with logs..."
	@./build-and-run.sh --clean --logs
