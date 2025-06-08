package com.shahar.tomjerry.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.shahar.tomjerry.data.AppDatabase
import com.shahar.tomjerry.data.Score
import com.shahar.tomjerry.repository.ScoreRepository
import kotlinx.coroutines.launch

// AndroidViewModel is a ViewModel that is aware of the Application context.
// We need the context to instantiate the database.
class ScoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScoreRepository
    val topScores: LiveData<List<Score>>
    private val _selectedScore = MutableLiveData<Score?>()
    val selectedScore: LiveData<Score?> = _selectedScore


    init {
        // Get a reference to the DAO from the database singleton.
        val scoreDao = AppDatabase.getDatabase(application).scoreDao()
        // Initialize the repository, providing it with the DAO.
        repository = ScoreRepository(scoreDao)
        // Get the top scores LiveData from the repository.
        topScores = repository.topScores
    }

    // This function provides a way to insert a score.
    // It must be called from a coroutine, so we use viewModelScope.
    fun insert(score: Score) = viewModelScope.launch {
        repository.insert(score)
    }

    fun onScoreSelected(score: Score) {
        _selectedScore.value = score
    }
}
