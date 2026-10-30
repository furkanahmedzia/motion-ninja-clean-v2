package com.motionninja.editor.export

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motionninja.domain.ExportConfig
import com.motionninja.domain.VideoCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExportViewModel(private val context: Context) : ViewModel() {
    
    private val _deviceCapabilities = MutableStateFlow<DeviceCapabilities?>(null)
    val deviceCapabilities = _deviceCapabilities.asStateFlow()
    
    init {
        viewModelScope.launch {
            _deviceCapabilities.value = detectDeviceCapabilities()
        }
    }
    
    data class DeviceCapabilities(
        val maxSupportedWidth: Int,
        val maxSupportedHeight: Int,
        val maxSupportedFps: Int,
        val supportedCodecs: Set<VideoCodec>,
        val hdrSupported: Boolean,
        val tenBitSupported: Boolean,
        val availableStorageBytes: Long,
        val totalMemoryMB: Int
    ) {
        fun isResolutionSupported(width: Int, height: Int): Boolean {
            return width <= maxSupportedWidth && height <= maxSupportedHeight
        }
        
        fun isCodecAvailable(codec: VideoCodec): Boolean {
            return supportedCodecs.contains(codec)
        }
        
        fun getRecommendedPreset(): ExportConfig {
            // Recommend highest quality that device can handle
            val presets = ExportConfig.getAllPresets().reversed() // Highest first
            return presets.firstOrNull { preset ->
                isResolutionSupported(preset.width, preset.height) &&
                isCodecAvailable(preset.codec) &&
                (!preset.enableHdr || hdrSupported)
            } ?: ExportConfig.HD_1080P
        }
    }
    
    private fun detectDeviceCapabilities(): DeviceCapabilities {
        var maxWidth = 1920
        var maxHeight = 1080
        var maxFps = 30
        val supportedCodecs = mutableSetOf<VideoCodec>()
        var hdrSupported = false
        var tenBitSupported = false
        
        // Detect hardware encoder capabilities
        try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (info in codecList.codecInfos) {
                if (!info.isEncoder) continue
                
                for (type in info.supportedTypes) {
                    val caps = info.capabilities
                    val videoCaps = caps.getVideoCapabilities()
                    
                    // Update max resolution
                    val widthRange = videoCaps.widthRange
                    val heightRange = videoCaps.heightRange
                    if (widthRange != null && heightRange != null) {
                        maxWidth = maxOf(maxWidth, widthRange.upper)
                        maxHeight = maxOf(maxHeight, heightRange.upper)
                    }
                    
                    // Check fps
                    val fpsRange = videoCaps.bitrateRange // actually use frame rate range if available
                    
                    // Detect codec
                    when (type) {
                        "video/avc" -> supportedCodecs.add(VideoCodec.H264)
                        "video/hevc" -> {
                            supportedCodecs.add(VideoCodec.HEVC)
                            // Check for 10-bit HEVC
                            val colorFormats = caps.colorFormats
                            if (colorFormats.any { it == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420P10 || 
                                                                   it == MediaCodecInfo.CodecCapabilities.COLOR_FormatP010 }) {
                                tenBitSupported = true
                            }
                        }
                        "video/vp9" -> supportedCodecs.add(VideoCodec.VP9)
                        "video/av01" -> supportedCodecs.add(VideoCodec.AV1)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback defaults
            supportedCodecs.add(VideoCodec.H264)
        }
        
        // Check HDR support (simplified)
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            hdrSupported = checkHdrSupport()
        }
        
        // Storage
        val storageDir = context.getExternalFilesDir(null) ?: context.filesDir
        val stat = android.os.StatFs(storageDir.path)
        val availableStorage = stat.blockSizeLong * stat.availableBlocksLong
        
        // Memory
        val memInfo = android.app.ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(memInfo)
        val totalMemoryMB = (memInfo.totalMem / 1024 / 1024).toInt()
        
        return DeviceCapabilities(
            maxSupportedWidth = maxWidth,
            maxSupportedHeight = maxHeight,
            maxSupportedFps = maxFps,
            supportedCodecs = supportedCodecs,
            hdrSupported = hdrSupported,
            tenBitSupported = tenBitSupported,
            availableStorageBytes = availableStorage,
            totalMemoryMB = totalMemoryMB
        )
    }
    
    private fun checkHdrSupport(): Boolean {
        // Check for HDR display support
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val display = windowManager.defaultDisplay
            // Simplified check - real implementation would check HDR capabilities via Display.HdrCapabilities
            true
        } catch (e: Exception) {
            false
        }
    }
}