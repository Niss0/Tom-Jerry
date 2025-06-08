package com.shahar.tomjerry.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class Score(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val score: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double
)
