package com.shahar.tomjerry.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.shahar.tomjerry.R
import com.shahar.tomjerry.utilities.GameConstants // Import your constants

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val buttonSlow: MaterialButton = findViewById(R.id.menu_button_slow)
        val buttonFast: MaterialButton = findViewById(R.id.menu_button_fast)
        val sensorModeButton: MaterialButton = findViewById(R.id.menu_sensor_mode)

        buttonSlow.setOnClickListener {
            startGameWithMode(GameConstants.MODE_BUTTON_SLOW)
        }

        buttonFast.setOnClickListener {
            startGameWithMode(GameConstants.MODE_BUTTON_FAST)
        }

        sensorModeButton.setOnClickListener {
            startGameWithMode(GameConstants.MODE_SENSOR)
        }
    }

    private fun startGameWithMode(gameMode: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra(GameConstants.EXTRA_GAME_MODE, gameMode)
        startActivity(intent)
    }
}