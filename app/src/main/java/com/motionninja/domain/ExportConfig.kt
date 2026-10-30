package com.motionninja.domain

import androidx.room.TypeConverters
import com.motionninja.data.converters.ExportConfigConverter
import kotlinx.serialization.Serializable

@Serializable
@TypeConverters(ExportConfigConverter::class)
data class ExportConfig(
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrateMbps: Int,
    val codec: VideoCodec = VideoCodec.H264,
    val audioBitrateKbps: Int = 256,
    val audioSampleRate: Int = 48000,
    val audioChannels: Int = 2,
    val preset: ExportPreset = ExportPreset.HIGH_QUALITY,
    val enableHdr: Boolean = false,
    val colorSpace: ColorSpace = ColorSpace.REC709
) {
    companion object {
        // Standard presets
        val PREVIEW_720P = ExportConfig(
            width = 720, height = 1280, fps = 30, bitrateMbps = 6,
            preset = ExportPreset.FAST
        )
        
        val HD_1080P = ExportConfig(
            width = 1080, height = 1920, fps = 30, bitrateMbps = 16,
            preset = ExportPreset.HIGH_QUALITY
        )
        
        val HD_1080P_60FPS = ExportConfig(
            width = 1080, height = 1920, fps = 60, bitrateMbps = 24,
            preset = ExportPreset.HIGH_QUALITY
        )
        
        // 2K presets
        val QHD_2K = ExportConfig(
            width = 1440, height = 2560, fps = 30, bitrateMbps = 30,
            preset = ExportPreset.HIGH_QUALITY
        )
        
        val QHD_2K_60FPS = ExportConfig(
            width = 1440, height = 2560, fps = 60, bitrateMbps = 45,
            preset = ExportPreset.HIGH_QUALITY
        )
        
        // 4K presets
        val UHD_4K = ExportConfig(
            width = 2160, height = 3840, fps = 30, bitrateMbps = 60,
            preset = ExportPreset.HIGH_QUALITY
        )
        
        val UHD_4K_60FPS = ExportConfig(
            width = 2160, height = 3840, fps = 60, bitrateMbps = 100,
            preset = ExportPreset.HIGH_QUALITY,
            enableHdr = true,
            colorSpace = ColorSpace.REC2020
        )
        
        val DCI_4K = ExportConfig(
            width = 2160, height = 4096, fps = 24, bitrateMbps = 80,
            preset = ExportPreset.HIGH_QUALITY,
            enableHdr = true,
            colorSpace = ColorSpace.REC2020
        )
        
        // Social media optimized
        val TIKTOK_REELS_1080P = ExportConfig(
            width = 1080, height = 1920, fps = 30, bitrateMbps = 12,
            preset = ExportPreset.BALANCED
        )
        
        val YOUTUBE_4K = ExportConfig(
            width = 2160, height = 3840, fps = 60, bitrateMbps = 50,
            preset = ExportPreset.HIGH_QUALITY
        )
        
        fun getAllPresets(): List<ExportConfig> = listOf(
            PREVIEW_720P,
            HD_1080P,
            HD_1080P_60FPS,
            QHD_2K,
            QHD_2K_60FPS,
            UHD_4K,
            UHD_4K_60FPS,
            DCI_4K,
            TIKTOK_REELS_1080P,
            YOUTUBE_4K
        )
        
        fun getPresetByName(name: String): ExportConfig? = getAllPresets().find { it.name == name }
    }
    
    val name: String
        get() = when {
            this == PREVIEW_720P -> "720p Preview"
            this == HD_1080P -> "1080p 30fps"
            this == HD_1080P_60FPS -> "1080p 60fps"
            this == QHD_2K -> "2K (1440p) 30fps"
            this == QHD_2K_60FPS -> "2K (1440p) 60fps"
            this == UHD_4K -> "4K UHD 30fps"
            this == UHD_4K_60FPS -> "4K UHD 60fps (HDR)"
            this == DCI_4K -> "4K DCI 24fps (Cinema)"
            this == TIKTOK_REELS_1080P -> "TikTok/Reels 1080p"
            this == YOUTUBE_4K -> "YouTube 4K 60fps"
            else -> "${width}x${height} ${fps}fps"
        }
    
    val estimatedFileSizePerMinuteMB: Double
        get() = (bitrateMbps * 1_000_000 / 8.0 * 60) / (1024 * 1024)
    
    val isHighResolution: Boolean
        get() = width >= 1440 || height >= 2560
    
    val requiresHdrSupport: Boolean
        get() = enableHdr
}

enum class VideoCodec { H264, HEVC, VP9, AV1 }
enum class ExportPreset { FAST, BALANCED, HIGH_QUALITY, LOSSLESS }
enum class ColorSpace { REC709, REC2020, P3_D65 }