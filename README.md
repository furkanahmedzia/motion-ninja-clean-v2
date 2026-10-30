# Motion Ninja Clean Rebuild

Professional video editor rebuilt from scratch — **no ads, no bloat, full 2K/4K/HDR export**.

## Features

| Category | Status |
|----------|--------|
| **Core Editor** | ✅ Multi-track timeline, clips, transitions |
| **AI Effects** | ✅ Cutout, RIFE slow-mo, Inpainting, Enhancement |
| **3D/Tracking** | ✅ SLAM, 3D text, Camera tracking |
| **Export** | ✅ **720p → 4K DCI**, 24/30/60fps, H.264/HEVC/VP9/AV1, **HDR (Rec.2020/P3)** |
| **Device Detection** | ✅ Auto-detects max resolution, codec, HDR support |
| **Ads/Analytics** | ❌ **Completely removed** |
| **Billing/Pro** | ❌ **Removed — all features free** |
| **Templates/Community** | ❌ **Removed** |
| **Bottom Bar** | 🔄 2-tab (Projects / Editor) |

## Export Presets

| Preset | Resolution | FPS | Bitrate | Codec | HDR |
|--------|------------|-----|---------|-------|-----|
| Preview | 720×1280 | 30 | 6 Mbps | H.264 | No |
| **1080p** | 1080×1920 | 30 | 16 Mbps | H.264 | No |
| **1080p 60fps** | 1080×1920 | 60 | 24 Mbps | H.264 | No |
| **2K (QHD)** | 1440×2560 | 30 | 30 Mbps | HEVC | No |
| **2K 60fps** | 1440×2560 | 60 | 45 Mbps | HEVC | No |
| **4K UHD** | 2160×3840 | 30 | 60 Mbps | HEVC | No |
| **4K UHD 60fps HDR** | 2160×3840 | 60 | 100 Mbps | HEVC | **Rec.2020** |
| **4K DCI Cinema** | 2160×4096 | 24 | 80 Mbps | HEVC | **Rec.2020** |
| TikTok/Reels | 1080×1920 | 30 | 12 Mbps | H.264 | No |
| YouTube 4K | 2160×3840 | 60 | 50 Mbps | HEVC | No |

## Requirements

- **Android Studio** Ladybug+ (2024.2+)
- **JDK 17**
- **Android SDK 34**, **NDK 26+**
- **Original Motion Ninja APK** (for native libs & AI models)

## Quick Start

```bash
# 1. Clone/navigate to rebuild directory
cd /workspace/rebuild

# 2. Extract native libraries & AI models from original APK
./extract_native_libs.sh "/storage/emulated/0/backups/apps/Motion Ninja 4.1.8.apk"

# 3. Open in Android Studio
#    File → Open → select /workspace/rebuild

# 4. Build & Install
./gradlew assembleDebug
./gradlew installDebug
```

## Project Structure

```
rebuild/
├── settings.gradle.kts
├── build.gradle.kts
├── extract_native_libs.sh
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/motionninja/
│       │   ├── MotionApp.kt
│       │   ├── MainActivity.kt
│       │   ├── domain/
│       │   │   ├── Project.kt
│       │   │   ├── TimelineTrack.kt
│       │   │   └── ExportConfig.kt          # 2K/4K/HDR presets
│       │   ├── data/
│       │   │   ├── ProjectRepository.kt
│       │   │   ├── MotionDatabase.kt
│       │   │   └── converters/
│       │   │       ├── ExportConfigConverter.kt
│       │   │       └── TrackListConverter.kt
│       │   ├── editor/
│       │   │   ├── EditorActivity.kt
│       │   │   └── export/
│       │   │       ├── ExportManager.kt       # Hardware validation, progress
│       │   │       ├── ExportSettingsScreen.kt # Quality/Codec/HDR UI
│       │   │       └── ExportViewModel.kt     # Device capability detection
│       │   ├── home/
│       │   │   └── HomeScreen.kt
│       │   ├── ui/theme/Theme.kt
│       │   └── util/NativeLibs.kt
│       ├── jniLibs/arm64-v8a/                 # ← Extracted .so files
│       ├── assets/                            # ← Extracted AI models
│       │   ├── cutout/model/
│       │   ├── rife/model/
│       │   ├── object3d/
│       │   └── mlkit-google-ocr-models/
│       └── res/
│           ├── values/strings.xml
│           └── xml/{file_paths,backup_rules,data_extraction_rules}.xml
```

## Native Libraries (from original APK)

These are **required** and must be extracted via `extract_native_libs.sh`:

| Library | Purpose |
|---------|---------|
| `libVAVComposition.so` | Core video composition/export engine |
| `libncnn.so` | Neural network inference (AI effects) |
| `libopencv_java4.so` | Computer vision |
| `libslam.so` | 3D camera tracking / SLAM |
| `librife.so` | RIFE frame interpolation |
| `libinpaint.so` | Video inpainting |
| `libremini.so` | Quality enhancement |
| `libtensorflowlite_*_jni.so` | TFLite inference |
| `libtext3D.so` | 3D text rendering |
| `libMNN.so` | MegEngine neural net runtime |
| `libgzysegment.so` | AI segmentation |
| `libavcodec/format/filter/util/swresample/swscale.so` | FFmpeg codecs |

## Architecture Decisions

1. **Compose-only UI** — No XML layouts, modern declarative UI
2. **Room + Flow** — Reactive database with coroutines
3. **NativeLibs singleton** — Loads all .so files at startup
4. **ExportViewModel** — Detects hardware capabilities once at launch
5. **No DI framework** — Manual `by viewModels()` + constructor injection
6. **ProGuard/R8** — Minifies release build (~8 MB DEX vs 42 MB original)

## Customizing Export

Edit `ExportConfig.kt` to add/remove presets:

```kotlin
val CUSTOM_5K = ExportConfig(
    width = 2880, height = 5120, fps = 30, bitrateMbps = 120,
    codec = VideoCodec.HEVC, enableHdr = true, colorSpace = ColorSpace.REC2020
)
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `UnsatisfiedLinkError` | Run `extract_native_libs.sh` again, verify .so files in `jniLibs/arm64-v8a/` |
| Export fails at 4K | Device lacks HEVC 4K encoder — falls back to 1080p with warning |
| Out of memory on 4K | Add `android:largeHeap="true"` to `<application>` |
| Gradle sync fails | Ensure JDK 17, Android Gradle Plugin 8.4+, KSP 1.9.20 |

## License

Rebuilt from reverse-engineered Motion Ninja 4.1.8. Original app copyright belongs to its developers. This clean-room rebuild is for educational/personal use only.

## Credits

- Original native engine: **VAVComposition** + FFmpeg + ncnn + OpenCV
- AI models: Extracted from original APK assets
- UI: Jetpack Compose Material3