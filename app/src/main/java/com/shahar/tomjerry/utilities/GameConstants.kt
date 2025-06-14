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
    const val GAME_SPEED_BOOST_MS = 100L // Making the boost significantly faster
    const val BONUS_DURATION_MS = 5000L // 5 seconds
    const val BONUS_COOLDOWN_MS = 10000L // 10 seconds
}