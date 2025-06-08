package com.shahar.tomjerry.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shahar.tomjerry.R
import com.shahar.tomjerry.ui.fragments.MapFragment
import com.shahar.tomjerry.ui.fragments.ScoresFragment

class HighScoresActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_scores)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.scores_fragment_container, ScoresFragment())
                .replace(R.id.map_fragment_container, MapFragment())
                .commit()
        }
    }
}
