package com.shahar.tomjerry.repository

import androidx.lifecycle.LiveData
import com.shahar.tomjerry.data.Score
import com.shahar.tomjerry.data.ScoreDao

class ScoreRepository(private val scoreDao: ScoreDao) {

    val topScores: LiveData<List<Score>> = scoreDao.getTopScores()

    suspend fun insert(score: Score) {
        scoreDao.insertScore(score)
    }
}
