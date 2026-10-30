package com.motionninja.editor.export

import android.content.Context
import android.os.Build
import android.util.Log
import com.motionninja.domain.ExportConfig
import com.motionninja.domain.Project
import com.motionninja.util.NativeLibs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ExportManager(
    private val context: Context,
    private val nativeLibs: NativeLibs = NativeLibs
) {
    
    private val TAG = "ExportManager"
    
    data class ExportResult(
        val success: Boolean,
        val outputPath: String? = null,
        val error: String? = null,
        val durationMs: Long = 0,
        val fileSizeBytes: Long = 0
    )
    
    data class ExportProgress(
        val progress: Float, // 0.0 to 1.0
        val currentStage: ExportStage,
        val estimatedTimeRemainingMs: Long = 0
    )
    
    enum class ExportStage {
        PREPARING, ENCODING_VIDEO, ENCODING_AUDIO, MUXING, FINALIZING, COMPLETED, FAILED
    }
    
    /**
     * Export project with full quality options including 2K/4K
     */
    suspend fun exportProject(
        project: Project,
        outputPath: String,
        config: ExportConfig,
        progressListener: (ExportProgress) -> Unit = {}
    ): ExportResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            // Validate device capabilities for high-res export
            val validation = validateExportCapabilities(config)
            if (!validation.isValid) {
                return@withContext ExportResult(
                    success = false,
                    error = validation.errorMessage
                )
            }
            
            // Prepare tracks JSON for native engine
            val tracksJson = serializeTracks(project.tracks)
            
            // Call native VAVComposition engine
            progressListener(ExportProgress(0.05f, ExportStage.PREPARING))
            
            val resultCode = nativeLibs.exportVideo(
                projectId = project.id,
                tracksJson = tracksJson,
                outputPath = outputPath,
                width = config.width,
                height = config.height,
                fps = config.fps,
                bitrate = config.bitrateMbps * 1_000_000L,
                codec = config.codec.ordinal,
                audioBitrate = config.audioBitrateKbps * 1000,
                audioSampleRate = config.audioSampleRate,
                audioChannels = config.audioChannels,
                enableHdr = config.enableHdr,
                colorSpace = config.colorSpace.ordinal,
                progressCallback = { progress, stage ->
                    val exportStage = when (stage) {
                        0 -> ExportStage.ENCODING_VIDEO
                        1 -> ExportStage.ENCODING_AUDIO
                        2 -> ExportStage.MUXING
                        3 -> ExportStage.FINALIZING
                        else -> ExportStage.ENCODING_VIDEO
                    }
                    val elapsed = System.currentTimeMillis() - startTime
                    val estimatedTotal = if (progress > 0) (elapsed / progress).toLong() else 0
                    val remaining = if (estimatedTotal > elapsed) estimatedTotal - elapsed else 0
                    progressListener(ExportProgress(
                        progress = progress.coerceIn(0f, 1f),
                        currentStage = exportStage,
                        estimatedTimeRemainingMs = remaining
                    ))
                }
            )
            
            progressListener(ExportProgress(1f, ExportStage.FINALIZING))
            
            val duration = System.currentTimeMillis() - startTime
            
            if (resultCode == 0) {
                val file = File(outputPath)
                val fileSize = if (file.exists()) file.length() else 0L
                
                progressListener(ExportProgress(1f, ExportStage.COMPLETED))
                
                Log.i(TAG, "Export completed: ${outputPath} (${fileSize} bytes, ${duration}ms)")
                
                ExportResult(
                    success = true,
                    outputPath = outputPath,
                    durationMs = duration,
                    fileSizeBytes = fileSize
                )
            } else {
                val errorMsg = getExportErrorMessage(resultCode)
                Log.e(TAG, "Export failed with code: $resultCode - $errorMsg")
                progressListener(ExportProgress(1f, ExportStage.FAILED))
                ExportResult(success = false, error = errorMsg)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Export exception", e)
            progressListener(ExportProgress(1f, ExportStage.FAILED))
            ExportResult(success = false, error = e.message ?: "Unknown export error")
        }
    }
    
    /**
     * Validate device can handle the requested export config
     */
    private fun validateExportCapabilities(config: ExportConfig): ValidationResult {
        // Check available storage
        val outputDir = File(config.toString()).parentFile ?: File("/storage/emulated/0/Movies")
        val usableSpace = outputDir.usableSpace
        val estimatedSize = (config.bitrateMbps * 1_000_000 / 8.0 * 60 * 10).toLong() // 10 min estimate
        
        if (usableSpace < estimatedSize * 2) { // 2x buffer
            return ValidationResult(false, "Insufficient storage. Need ~${(estimatedSize / 1024 / 1024).toInt()}MB free")
        }
        
        // Check hardware encoder support for 4K
        if (config.isHighResolution) {
            val hasHardwareEncoder = checkHardwareEncoderSupport(config.codec, config.width, config.height)
            if (!hasHardwareEncoder) {
                return ValidationResult(false, "Device doesn't support hardware encoding for ${config.width}x${config.height}. Try lower resolution.")
            }
        }
        
        // Check HEVC support
        if (config.codec == VideoCodec.HEVC && !MediaCodecUtils.isHevcEncoderAvailable()) {
            return ValidationResult(false, "HEVC encoder not available. Use H.264 instead.")
        }
        
        // Check AV1 support (API 30+)
        if (config.codec == VideoCodec.AV1 && Build.VERSION.SDK_INT < 30) {
            return ValidationResult(false, "AV1 requires Android 11+. Use H.264 or HEVC.")
        }
        
        // Check HDR support
        if (config.enableHdr && !MediaCodecUtils.isHdrSupported(context)) {
            return ValidationResult(false, "HDR encoding not supported on this device.")
        }
        
        return ValidationResult(true, null)
    }
    
    private fun checkHardwareEncoderSupport(codec: VideoCodec, width: Int, height: Int): Boolean {
        return try {
            MediaCodecUtils.isEncoderAvailable(codec, width, height)
        } catch (e: Exception) {
            false
        }
    }
    
    private fun serializeTracks(tracks: List<Project.TimelineTrack>): String {
        // TODO: Implement proper JSON serialization using kotlinx.serialization
        return "[]"
    }
    
    private fun getExportErrorMessage(code: Int): String = when (code) {
        -1 -> "Invalid input parameters"
        -2 -> "Failed to initialize encoder"
        -3 -> "Failed to initialize muxer"
        -4 -> "Encoding failed"
        -5 -> "Muxing failed"
        -6 -> "Output file write error"
        -7 -> "Out of memory"
        -8 -> "Unsupported resolution/codec combination"
        -9 -> "HDR not supported"
        -10 -> "License validation failed"
        else -> "Export failed with code: $code"
    }
    
    data class ValidationResult(val isValid: Boolean, val errorMessage: String?)
}

object MediaCodecUtils {
    fun isHevcEncoderAvailable(): Boolean {
        // Check MediaCodecList for HEVC encoder
        return try {
            android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos.any {
                it.isEncoder && it.name.contains("hevc", ignoreCase = true)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun isEncoderAvailable(codec: VideoCodec, width: Int, height: Int): Boolean {
        val mime = when (codec) {
            VideoCodec.H264 -> "video/avc"
            VideoCodec.HEVC -> "video/hevc"
            VideoCodec.VP9 -> "video/vp9"
            VideoCodec.AV1 -> "video/av01"
        }
        return try {
            android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.contains(mime) && 
                info.capabilities.getVideoCapabilities().isSizeSupported(width, height)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    fun isHdrSupported(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            // Check for HDR capabilities
            val display = context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
            val metrics = android.util.DisplayMetrics()
            display.defaultDisplay.getRealMetrics(metrics)
            // Simplified check - real implementation would check HDR capabilities
            true
        } else {
            false
        }
    }
}