package com.motionninja.data.converters

import androidx.room.TypeConverter
import com.motionninja.domain.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TrackListConverter {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        useArrayPolymorphism = true
    }
    
    @TypeConverter
    fun trackListToString(tracks: List<TimelineTrack>): String {
        return json.encodeToString(tracks)
    }
    
    @TypeConverter
    fun stringToTrackList(data: String): List<TimelineTrack> {
        return try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            emptyList()
        }
    }
}