#!/bin/bash
# build_apk.sh - Fully automated APK build via GitHub Actions
# Run this AFTER: gh auth login

set -e

REPO_NAME="motion-ninja-clean"
ORIGINAL_APK="/storage/emulated/0/backups/apps/Motion Ninja 4.1.8.apk"
WORKDIR="/workspace/rebuild"

echo "=========================================="
echo "  Motion Ninja Clean - Automated APK Build"
echo "=========================================="
echo ""

# Check prerequisites
command -v gh >/dev/null 2>&1 || { echo "❌ gh CLI not found"; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "❌ Not logged in. Run: gh auth login"; exit 1; }
[ -f "$ORIGINAL_APK" ] || { echo "❌ Original APK not found at: $ORIGINAL_APK"; exit 1; }

echo "✅ Prerequisites OK"
echo ""

# 1. Create GitHub repo (if not exists)
echo "📦 Creating/checking GitHub repo: $REPO_NAME"
if gh repo view "$REPO_NAME" >/dev/null 2>&1; then
    echo "   Repo exists, updating..."
    cd "$WORKDIR"
    git add -A
    git commit -m "Update $(date '+%Y-%m-%d %H:%M')" 2>/dev/null || true
    git push origin main
else
    echo "   Creating new repo..."
    cd "$WORKDIR"
    gh repo create "$REPO_NAME" --public --source=. --push
fi

REPO_FULL=$(gh repo view --json nameWithOwner -q .nameWithOwner)
echo "   Repo: $REPO_FULL"
echo ""

# 2. Upload original APK as release asset
echo "📤 Uploading original APK as release asset..."
gh release create build-assets \
  "$ORIGINAL_APK" \
  --title "Build Assets - $(date '+%Y-%m-%d')" \
  --notes "Original Motion Ninja 4.1.8 APK for native library extraction" \
  --repo "$REPO_FULL" \
  --clobber 2>/dev/null || \
gh release upload build-assets \
  "$ORIGINAL_APK" \
  --repo "$REPO_FULL" \
  --clobber

echo "   ✅ APK uploaded to release 'build-assets'"
echo ""

# 3. Trigger workflow
echo "🚀 Triggering GitHub Actions build..."
RUN_ID=$(gh workflow run build.yml --repo "$REPO_FULL" --json runId -q .runId 2>/dev/null || \
         gh workflow run build.yml --repo "$REPO_FULL" && \
         sleep 3 && \
         gh run list --workflow=build.yml --repo "$REPO_FULL" --limit=1 --json databaseId -q '.[0].databaseId')

echo "   Run ID: $RUN_ID"
echo ""

# 4. Watch build
echo "👀 Watching build (this takes 3-5 minutes)..."
gh run watch "$RUN_ID" --repo "$REPO_FULL"

# 5. Download APK
echo ""
echo "⬇️  Downloading APK..."
gh run download "$RUN_ID" -n motion-ninja-clean-debug --repo "$REPO_FULL"

# Find the APK
APK_FILE=$(find . -name "app-debug.apk" -o -name "*.apk" 2>/dev/null | head -1)
if [ -n "$APK_FILE" ]; then
    FINAL_NAME="MotionNinjaClean-$(date '+%Y%m%d-%H%M').apk"
    mv "$APK_FILE" "$FINAL_NAME"
    echo ""
    echo "=========================================="
    echo "✅ BUILD COMPLETE!"
    echo "=========================================="
    echo "APK: $FINAL_NAME"
    echo "Size: $(ls -lh "$FINAL_NAME" | awk '{print $5}')"
    echo "Location: $(pwd)/$FINAL_NAME"
    echo ""
    echo "Install to device:"
    echo "  adb install $FINAL_NAME"
else
    echo "❌ APK not found in download"
    exit 1
fi