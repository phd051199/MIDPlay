#!/bin/bash

# MIDPlay J2ME Build Script
# Professional build script for J2ME application

set -e

# Colors for professional logging
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${CYAN}[STEP]${NC} $1"
}

# Configuration
ENABLE_PROGUARD=${1:-true}
BUILD_TYPE=${2:-release}

echo -e "${BLUE}╔══════════════════════════════════════╗${NC}"
echo -e "${BLUE}║        MIDPlay J2ME Builder          ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════╝${NC}"
echo ""

log_info "Build configuration:"
log_info "  • Build type: $BUILD_TYPE"
log_info "  • ProGuard enabled: $ENABLE_PROGUARD"
echo ""

# Verify required libraries exist
log_step "Verifying build environment..."
if [ ! -f "lib/cldc_1.1.jar" ] || [ ! -f "lib/midp_2.0.jar" ]; then
    log_error "Required J2ME libraries not found in lib/ directory"
    log_error "Please ensure cldc_1.1.jar and midp_2.0.jar are present"
    exit 1
fi

if [ ! -f "lib/proguard-ant.jar" ]; then
    log_warning "ProGuard library not found, obfuscation will be disabled"
    ENABLE_PROGUARD=false
fi

log_success "Build environment verified"

# Clean previous builds
log_step "Cleaning previous builds..."
rm -rf build/compiled
rm -rf dist/MIDPlay_midlet.jar
rm -rf dist/MIDPlay.jar
rm -f out.map
log_success "Cleanup completed"

# Create build directories
log_step "Preparing build directories..."
mkdir -p build/compiled
mkdir -p dist
log_success "Build directories ready"

# Compile Java sources
log_step "Compiling Java sources..."
CLASSPATH="lib/cldc_1.1.jar:lib/midp_2.0.jar:res"

# Find all Java files
JAVA_FILES=$(find src -name "*.java" | tr '\n' ' ')
JAVA_COUNT=$(echo $JAVA_FILES | wc -w | tr -d ' ')

if [ -z "$JAVA_FILES" ]; then
    log_error "No Java source files found in src/ directory"
    exit 1
fi

log_info "Found $JAVA_COUNT Java source files"

# Compile with Java 8 compatibility
javac -cp "$CLASSPATH" -d build/compiled -source 8 -target 8 $JAVA_FILES 2>/dev/null

if [ $? -ne 0 ]; then
    log_error "Compilation failed"
    exit 1
fi

log_success "Compilation completed successfully"

# Copy application descriptor
log_step "Using Application Descriptor..."
if [ ! -f "Application Descriptor" ]; then
    log_error "Application Descriptor file not found"
    exit 1
fi

cp "Application Descriptor" build/manifest.mf

log_success "Manifest created"

# Create JAR file
log_step "Building JAR archive..."
cd build/compiled
jar cfm ../../dist/MIDPlay_midlet.jar ../manifest.mf .
JAR_RESULT=$?
cd ../..

if [ $JAR_RESULT -ne 0 ] || [ ! -f "dist/MIDPlay_midlet.jar" ]; then
    log_error "Failed to create JAR file"
    exit 1
fi

log_success "JAR archive created"

# Add resources to JAR
log_step "Adding application resources..."
cd res
jar uf ../dist/MIDPlay_midlet.jar * 2>/dev/null
cd ..
log_success "Resources added to JAR"

# Create JAD file
log_step "Generating JAD descriptor..."
JAR_SIZE=$(stat -f%z dist/MIDPlay_midlet.jar 2>/dev/null || stat -c%s dist/MIDPlay_midlet.jar)

# Copy Application Descriptor content and add JAR-specific properties
cp "Application Descriptor" dist/MIDPlay_midlet.jad

# Add JAR size and URL to JAD
sed -i '' '1i\
MIDlet-Jar-Size: '$JAR_SIZE'
' dist/MIDPlay_midlet.jad

sed -i '' '2i\
MIDlet-Jar-URL: MIDPlay_midlet.jar
' dist/MIDPlay_midlet.jad

log_success "JAD descriptor generated"

# Get original JAR size for reporting
ORIGINAL_SIZE=$(stat -f%z dist/MIDPlay_midlet.jar 2>/dev/null || stat -c%s dist/MIDPlay_midlet.jar)

# Run ProGuard if enabled
if [ "$ENABLE_PROGUARD" = "true" ]; then
    log_step "Running ProGuard obfuscation..."
    
    if [ -f "lib/proguard-ant.jar" ]; then
        java -cp lib/proguard-ant.jar proguard.ProGuard @midlets.pro 2>/dev/null
        
        if [ -f "dist/MIDPlay.jar" ]; then
            OBFUSCATED_SIZE=$(stat -f%z dist/MIDPlay.jar 2>/dev/null || stat -c%s dist/MIDPlay.jar)
            REDUCTION=$((100 - (OBFUSCATED_SIZE * 100 / ORIGINAL_SIZE)))
            
            log_success "ProGuard obfuscation completed"
            log_info "  • Original size: $(printf "%'d" $ORIGINAL_SIZE) bytes"
            log_info "  • Obfuscated size: $(printf "%'d" $OBFUSCATED_SIZE) bytes"
            log_info "  • Size reduction: ${REDUCTION}%"
        else
            log_warning "ProGuard did not create output JAR"
        fi
    else
        log_warning "ProGuard library not found, skipping obfuscation"
    fi
else
    log_info "ProGuard obfuscation skipped"
fi

# Build summary
echo ""
echo -e "${GREEN}╔══════════════════════════════════════╗${NC}"
echo -e "${GREEN}║           BUILD COMPLETED            ║${NC}"
echo -e "${GREEN}╚══════════════════════════════════════╝${NC}"
echo ""

log_success "Build artifacts:"
ls -la dist/ | grep -E '\.(jar|jad)$' | while read -r line; do
    echo -e "  ${GREEN}•${NC} $(echo $line | awk '{print $9, "(" $5 " bytes)"}')"
done

if [ -f "out.map" ]; then
    log_info "ProGuard mapping file: out.map"
fi

echo ""
log_success "Build completed successfully!"
echo ""
