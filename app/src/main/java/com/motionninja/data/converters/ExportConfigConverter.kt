package com.motionninja.data.converters

import androidx.room.TypeConverter
import com.motionninja.domain.ExportConfig
import com.motionninja.domain.VideoCodec
import com.motionninja.domain.ExportPreset
import com.motionninja.domain.ColorSpace
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ExportConfigConverter {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    @TypeConverter
    fun exportConfigToString(config: ExportConfig): String {
        return json.encodeToString(config)
    }
    
    @TypeConverter
    fun stringToExportConfig(data: String): ExportConfig {
        return try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            ExportConfig.HD_1080P
        }
    }
    
    @TypeConverter
    fun videoCodecToString(codec: VideoCodec): String = codec.name
    
    @TypeConverter
    fun stringToVideoCodec(data: String): VideoCodec = VideoCodec.valueOf(data)
    
    @TypeConverter
    fun exportPresetToString(preset: ExportPreset): String = preset.name
    
    @TypeConverter
    fun stringToExportPreset(data: String): ExportPreset = ExportPreset.valueOf(data)
    
    @TypeConverter
    fun colorSpaceToString(space: ColorSpace): String = space.name
    
    @TypeConverter
    fun stringToColorSpace(data: String): ColorSpace = ColorSpace.valueOf(data)
}