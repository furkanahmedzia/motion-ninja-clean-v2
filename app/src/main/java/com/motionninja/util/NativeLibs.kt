package com.motionninja.util

class NativeLibs {
    companion object {
        private val LIBS = listOf(
            "ncnn", "opencv_java4", "VAVComposition", "avcodec", "avformat", "avfilter",
            "avutil", "swresample", "swscale", "slam", "rife", "inpaint", "remini",
            "tensorflowlite_jni", "tensorflowlite_gpu_jni", "text3D", "MNN", "m3d",
            "fx3d", "nanovg", "mmkv", "gzysegment", "ArmArchNewEncrypt", "dcv", "mnnrvm",
            "pl_droidsonroids_gif"
        )
        
        @JvmStatic
        fun loadAll() {
            LIBS.forEach { System.loadLibrary(it) }
        }
    }
    
    // Core video export with full quality options
    external fun exportVideo(
        projectId: Long,
        tracksJson: String,
        outputPath: String,
        width: Int, height: Int, fps: Int,
        bitrate: Long, codec: Int,
        audioBitrate: Int, audioSampleRate: Int, audioChannels: Int,
        enableHdr: Boolean, colorSpace: Int,
        progressCallback: (Float, Int) -> Unit
    ): Int
    
    // AI inference
    external fun segmentCutout(inputPath: String, outputPath: String): Int
    external fun rifeInterpolate(inputPath: String, outputPath: String, times: Int): Int
    external fun inpaintRemove(inputPath: String, maskPath: String, outputPath: String): Int
    external fun enhanceQuality(inputPath: String, outputPath: String, level: Int): Int
    external fun slamTrack(inputPath: String, configJson: String): String // returns JSON
    
    // Hardware capability queries
    external fun getMaxSupportedResolution(): IntArray // [width, height]
    external fun getSupportedCodecs(): IntArray // bitmask of VideoCodec
    external fun isHdrEncodingSupported(): Boolean
    external fun isTenBitEncodingSupported(): Boolean
}