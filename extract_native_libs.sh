#!/bin/bash
# extract_native_libs.sh
# Extract native libraries and assets from original Motion Ninja APK

set -e

ORIGINAL_APK="${1:-/storage/emulated/0/backups/apps/Motion Ninja 4.1.8.apk}"
TARGET_DIR="${2:-$(pwd)/app/src/main}"

if [ ! -f "$ORIGINAL_APK" ]; then
    echo "Error: Original APK not found at $ORIGINAL_APK"
    echo "Usage: $0 <path-to-original-apk> [target-dir]"
    exit 1
fi

echo "Extracting from: $ORIGINAL_APK"
echo "Target: $TARGET_DIR"

# Create directories
mkdir -p "$TARGET_DIR/jniLibs/arm64-v8a"
mkdir -p "$TARGET_DIR/assets/cutout/model"
mkdir -p "$TARGET_DIR/assets/rife/model"
mkdir -p "$TARGET_DIR/assets/object3d"
mkdir -p "$TARGET_DIR/assets/mlkit-google-ocr-models"

# Extract native libraries (arm64-v8a only for modern devices)
echo "Extracting native libraries..."
unzip -j "$ORIGINAL_APK" "lib/arm64-v8a/*.so" -d "$TARGET_DIR/jniLibs/arm64-v8a/"

# Extract AI models
echo "Extracting AI models..."
unzip -j "$ORIGINAL_APK" "assets/cutout/model/*" -d "$TARGET_DIR/assets/cutout/model/" 2>/dev/null || true
unzip -j "$ORIGINAL_APK" "assets/rife/model/*" -d "$TARGET_DIR/assets/rife/model/" 2>/dev/null || true
unzip -j "$ORIGINAL_APK" "assets/object3d/*" -d "$TARGET_DIR/assets/object3d/" 2>/dev/null || true
unzip -j "$ORIGINAL_APK" "assets/mlkit-google-ocr-models/**/*" -d "$TARGET_DIR/assets/mlkit-google-ocr-models/" 2>/dev/null || true

# List extracted files
echo ""
echo "=== Extracted native libraries ==="
ls -lh "$TARGET_DIR/jniLibs/arm64-v8a/"

echo ""
echo "=== Extracted assets ==="
find "$TARGET_DIR/assets" -type f | head -30
echo "..."

echo ""
echo "Done! Native libraries and assets extracted to $TARGET_DIR"
echo ""
echo "Next steps:"
echo "1. Open project in Android Studio"
echo "2. Sync Gradle"
echo "3. Build: ./gradlew assembleDebug"
echo "4. Install: ./gradlew installDebug"