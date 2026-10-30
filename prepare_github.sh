#!/bin/bash
# prepare_github.sh - Prepare rebuild project for GitHub Actions build

set -e

REBUILD_DIR="/workspace/rebuild"
ORIGINAL_APK="/storage/emulated/0/backups/apps/Motion Ninja 4.1.8.apk"
GITHUB_REPO="${1:-yourusername/motion-ninja-clean}"

echo "=== Motion Ninja Clean - GitHub Build Preparation ==="
echo ""

# 1. Verify original APK exists
if [ ! -f "$ORIGINAL_APK" ]; then
    echo "❌ Original APK not found at: $ORIGINAL_APK"
    echo "   Please update ORIGINAL_APK variable in this script"
    exit 1
fi

echo "✅ Found original APK: $(ls -lh "$ORIGINAL_APK" | awk '{print $5}')"
echo ""

# 2. Copy APK to rebuild dir for workflow (optional - workflow can reference external)
echo "Copying APK to rebuild directory for reference..."
cp "$ORIGINAL_APK" "$REBUILD_DIR/original.apk"
echo "✅ Copied to rebuild/original.apk"
echo ""

# 3. Initialize git if needed
cd "$REBUILD_DIR"
if [ ! -d ".git" ]; then
    echo "Initializing git repo..."
    git init
    git config user.email "builder@local"
    git config user.name "Local Builder"
fi

# 4. Add all files
git add .
git commit -m "Motion Ninja Clean Rebuild - $(date '+%Y-%m-%d')"

echo ""
echo "=== Next Steps ==="
echo ""
echo "1. Create GitHub repo: $GITHUB_REPO"
echo "   gh repo create $GITHUB_REPO --public --source=. --push"
echo ""
echo "2. Or manually:"
echo "   git remote add origin https://github.com/$GITHUB_REPO.git"
echo "   git branch -M main"
echo "   git push -u origin main"
echo ""
echo "3. Enable GitHub Actions in repo settings"
echo ""
echo "4. Run workflow:"
echo "   gh workflow run build.yml"
echo "   # or push a tag: git tag v1.0.0 && git push origin v1.0.0"
echo ""
echo "5. Download APK from Actions artifacts:"
echo "   gh run list --workflow=build.yml"
echo "   gh run download <run-id> -n motion-ninja-clean-debug"
echo ""
echo "=== Alternative: Upload APK as Release Asset ==="
echo "If workflow can't access original APK, upload it as a release asset first:"
echo "  gh release create assets-v1 original.apk --title 'Build Assets' --repo $GITHUB_REPO"
echo "Then modify workflow to download from release."
echo ""
echo "Done! Rebuild project ready at: $REBUILD_DIR"