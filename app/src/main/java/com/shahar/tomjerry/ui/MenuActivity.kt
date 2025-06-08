package com.shahar.tomjerry.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shahar.tomjerry.databinding.ActivityMenuBinding // Add this import
import com.shahar.tomjerry.utilities.GameConstants

class MenuActivity : AppCompatActivity() {

    // Add a binding variable
    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize ViewBinding
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Use binding to access views
        binding.menuButtonSlow.setOnClickListener {
            startGameWithMode(GameConstants.MODE_BUTTON_SLOW)
        }

        binding.menuButtonFast.setOnClickListener {
            startGameWithMode(GameConstants.MODE_BUTTON_FAST)
        }

        binding.menuSensorMode.setOnClickListener {
            startGameWithMode(GameConstants.MODE_SENSOR)
        }

        binding.menuButtonHighScores.setOnClickListener {
            val intent = Intent(this, HighScoresActivity::class.java)
            startActivity(intent)
        }
        // --------------------------
    }

    private fun startGameWithMode(gameMode: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra(GameConstants.EXTRA_GAME_MODE, gameMode)
        startActivity(intent)
    }
}
