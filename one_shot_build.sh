#!/bin/bash
# one_shot_build.sh - Fully automated: upload APK to filebin, create repo, build, download
# Usage: ./one_shot_build.sh "https://filebin.net/xxx/Motion_Ninja.apk"

set -e

APK_URL="${1}"
REPO_NAME="motion-ninja-clean"
WORKDIR="/workspace/rebuild"

if [ -z "$APK_URL" ]; then
    echo "Usage: $0 \"<direct-download-url-to-apk>\""
    echo "Example: $0 \"https://filebin.net/abc123/Motion%20Ninja%204.1.8.apk\""
    exit 1
fi

# Check prerequisites
command -v gh >/dev/null 2>&1 || { echo "❌ gh CLI not found"; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "❌ Not logged in. Run: gh auth login"; exit 1; }

echo "=========================================="
echo "  Motion Ninja Clean - One-Shot Build"
echo "=========================================="
echo "APK URL: $APK_URL"
echo ""

# 1. Create/update GitHub repo
echo "📦 Setting up GitHub repo: $REPO_NAME"
cd "$WORKDIR"

if gh repo view "$REPO_NAME" >/dev/null 2>&1; then
    echo "   Repo exists, pushing updates..."
    git add -A
    git commit -m "Update $(date '+%Y-%m-%d %H:%M')" 2>/dev/null || true
    git push origin main
else
    echo "   Creating new repo..."
    gh repo create "$REPO_NAME" --public --source=. --push
fi

REPO_FULL=$(gh repo view --json nameWithOwner -q .nameWithOwner)
echo "   Repo: $REPO_FULL"
echo ""

# 2. Trigger workflow with APK URL
echo "🚀 Triggering build with APK URL..."
RUN_ID=$(gh workflow run build.yml -f apk_url="$APK_URL" --repo "$REPO_FULL" --json runId -q .runId 2>/dev/null || {
    # Fallback: trigger and then get run ID
    gh workflow run build.yml -f apk_url="$APK_URL" --repo "$REPO_FULL"
    sleep 3
    gh run list --workflow=build.yml --repo "$REPO_FULL" --limit=1 --json databaseId -q '.[0].databaseId'
})

echo "   Run ID: $RUN_ID"
echo ""

# 3. Watch build
echo "👀 Watching build (3-5 minutes)..."
gh run watch "$RUN_ID" --repo "$REPO_FULL"

# 4. Download APK
echo ""
echo "⬇️  Downloading APK..."
gh run download "$RUN_ID" -n motion-ninja-clean-debug --repo "$REPO_FULL"

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
    echo "Install: adb install $FINAL_NAME"
else
    echo "❌ APK not found"
    exit 1
fi