package com.mcldev.comprainteligente.data.database

import android.location.Location
import androidx.room.TypeConverter

/**
 * Covnerts the data coming from and to the database
 */
class Converter {
    @TypeConverter
    fun fromLocation(location: Location?): String? {
        return if (location != null) {
            "${location.latitude},${location.longitude}"
        } else {
            null
        }
    }

    /**
     * @param locationString: location stored as a String (usually from the DB)
     * This methods converts the String into the correct Location type
     */
    @TypeConverter
    fun toLocation(locationString: String?): Location? {
        return if (locationString != null) {
            val latLong = locationString.split(",")
            val location = Location("provider")
            location.latitude = latLong[0].toDouble()
            location.longitude = latLong[1].toDouble()
            location
        } else {
            null
        }
    }
}