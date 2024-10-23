package com.mcldev.comprainteligente.data

import android.location.Location
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supermarket")
data class Supermarket (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val localization: Location  // Can represent the location of the supermarket
)