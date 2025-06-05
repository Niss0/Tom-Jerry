package com.shahar.tomjerry.utilities

object GameConstants {
    const val EXTRA_GAME_MODE = "EXTRA_GAME_MODE"

    const val MODE_BUTTON_SLOW = "MODE_BUTTON_SLOW"
    const val MODE_BUTTON_FAST = "MODE_BUTTON_FAST"
    const val MODE_SENSOR = "MODE_SENSOR"

    // --- Define Game Speeds (delay in milliseconds) ---
    const val GAME_SPEED_SLOW_MS: Long = 300L   // Slower game
    const val GAME_SPEED_NORMAL_MS: Long = 200L // Default/Sensor mode speed
    const val GAME_SPEED_FAST_MS: Long = 100L   // Faster game
}