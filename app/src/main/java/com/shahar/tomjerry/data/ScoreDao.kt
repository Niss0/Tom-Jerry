package com.shahar.tomjerry.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: Score)

    @Query("SELECT * FROM scores ORDER BY score DESC LIMIT 10")
    fun getTopScores(): LiveData<List<Score>>
}

