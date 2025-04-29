package com.shahar.tomjerry.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.shahar.tomjerry.R


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.start_game_button).setOnClickListener {
            Toast.makeText(this, "Button clicked", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "Start Game Button Clicked")
            startActivity(Intent(this, GameActivity::class.java))
        }



    }

    override fun onResume() {
        super.onResume()

        findViewById<Button>(R.id.start_game_button).setOnClickListener {
            Log.d("MainActivity", "Start Game Button Clicked")
            startActivity(Intent(this, GameActivity::class.java))
        }
    }
}