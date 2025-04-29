package com.shahar.tomjerry.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.shahar.tomjerry.viewmodel.GameManager
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.shahar.tomjerry.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameActivity : AppCompatActivity() {
    private lateinit var gameManager: GameManager
    private lateinit var jerryImageView: ImageView
    private lateinit var buttonLeft: Button
    private lateinit var buttonRight: Button
    private lateinit var gameRelativeLayout: RelativeLayout
    // obstacleViews maps position to view
    private val obstacleViews = mutableMapOf<Pair<Int, Int>, ImageView>()
    private lateinit var heartViews: List<ImageView>
    private lateinit var heartsContainer: RelativeLayout
    private lateinit var buttonsContainer: ConstraintLayout

    private var screenWidth = 0
    private var colWidth = 0
    private var playableAreaTop = 0
    private var playableAreaBottom = 0
    private var playableAreaHeight = 0
    private var isGameRunning = false
    private var gameJob: Job? = null

    // Define desired size for Jerry and Tom (in pixels)
    private val characterSize = 400


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameManager = GameManager()

        // Initialize UI elements
        jerryImageView = findViewById(R.id.jerry_image_view)
        buttonLeft = findViewById(R.id.btn_left_arrow_game)
        buttonRight = findViewById(R.id.btn_right_arrow_game)

        // Convert the parent layout to RelativeLayout
        // Note: You'll need to update your layout XML file as well
        gameRelativeLayout = findViewById(R.id.game_relative_layout)
        heartsContainer = findViewById(R.id.hearts_container)
        buttonsContainer = findViewById(R.id.buttons_container)

        gameRelativeLayout.post {
            screenWidth = gameRelativeLayout.width
            colWidth = screenWidth / gameManager.cols
            playableAreaBottom = buttonsContainer.top
            playableAreaHeight = playableAreaBottom - playableAreaTop
            initializeHearts()
            positionJerryBasedOnColumn()
            setupButtonListeners()
            startGame()
        }
    }

    private fun setupButtonListeners() {
        buttonLeft.setOnClickListener {
            if (isGameRunning && gameManager.moveJerryLeft()) {
                positionJerryBasedOnColumn()
                // Check for collision immediately after Jerry moves
                checkAndHandleCollision()
                // Update obstacle views after potential collision
                updateObstacleViews()
            }
        }

        buttonRight.setOnClickListener {
            if (isGameRunning && gameManager.moveJerryRight()) {
                positionJerryBasedOnColumn()
                // Check for collision immediately after Jerry moves
                checkAndHandleCollision()
                // Update obstacle views after potential collision
                updateObstacleViews()
            }
        }
    }

    private fun positionJerryBasedOnColumn() {
        // Calculate position based on column using RelativeLayout parameters
        val column = gameManager.getJerryColumn()

        // Create RelativeLayout params with fixed size
        val params = RelativeLayout.LayoutParams(
            characterSize, // Set width
            characterSize  // Set height
        )

        // Set vertical position (align bottom with buttons container)
        params.addRule(RelativeLayout.ABOVE, buttonsContainer.id)

        // Set horizontal position based on column
        val horizontalMargin = (colWidth - characterSize) / 2
        when (column) {
            0 -> { // Left column
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                params.leftMargin = horizontalMargin
            }
            1 -> { // Middle column
                params.addRule(RelativeLayout.CENTER_HORIZONTAL)
            }
            2 -> { // Right column
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                params.rightMargin = horizontalMargin
            }
        }

        // Apply the layout parameters
        jerryImageView.layoutParams = params
    }

    private fun gameOver() {
        stopGame()
        // Show game over message
        findViewById<TextView>(R.id.gameOverText).visibility = View.VISIBLE
    }

    private fun startGame() {
        isGameRunning = true
        var obstacleSpawnCounter = 0
        val obstacleSpawnFrequency = 10 // Adjust to control how often obstacles spawn

        gameJob = lifecycleScope.launch(Dispatchers.Default) {
            // Game loop
            while (isGameRunning) {
                gameManager.moveObstaclesDown()
                val collisionPosition = checkAndHandleCollision()

                val shouldSpawnObstacle = obstacleSpawnCounter >= obstacleSpawnFrequency
                if (shouldSpawnObstacle) {
                    obstacleSpawnCounter = 0
                    gameManager.addObstacle()
                }

                // 2. UI UPDATE PHASE - Switch to Main Thread
                withContext(Dispatchers.Main) {
                    // Update all obstacle views based on the current state of the game matrix
                    updateObstacleViews()

                    if (collisionPosition != null) {
                        updateHeartsDisplay()
                        makeText(this@GameActivity,
                            getString(R.string.toast_message_when_hit), LENGTH_SHORT
                        ).show()

                        vibrate()

                        if (gameManager.isGameOver()) {
                            gameOver()
                            return@withContext  // Exit the coroutine
                        }
                    }
                }
                obstacleSpawnCounter++
                delay(200)  // Game tick delay - Adjust for game speed
            }
        }
    }

    private fun updateHeartsDisplay() {
        val remainingHearts = gameManager.getRemainingHearts()
        for (i in heartViews.indices) {
            if (i < remainingHearts) {
                heartViews[i].visibility = View.VISIBLE
            } else {
                heartViews[i].visibility = View.INVISIBLE // Or View.GONE
            }
        }
    }
    private fun initializeHearts() {
        // Simply collect all existing heart views into a list
        heartViews = listOf(
            findViewById(R.id.heart1),
            findViewById(R.id.heart2),
            findViewById(R.id.heart3)
        )
    }

    // Helper function to position an obstacle view
    private fun positionObstacle(obstacleView: ImageView, row: Int, col: Int) {
        // Calculate position using RelativeLayout parameters with fixed size
        val params = RelativeLayout.LayoutParams(
            characterSize, // Set width
            characterSize  // Set height
        )

        val rowHeight = playableAreaHeight / gameManager.rows
        params.topMargin = playableAreaTop + (row * rowHeight)

        // Set horizontal position based on column
        val horizontalMargin = (colWidth - characterSize) / 2
        when (col) {
            0 -> { // Left column
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                params.leftMargin = horizontalMargin
            }
            1 -> { // Middle column
                params.addRule(RelativeLayout.CENTER_HORIZONTAL)
            }
            2 -> { // Right column
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                params.rightMargin = horizontalMargin
            }
        }

        // Apply the layout parameters
        obstacleView.layoutParams = params
    }

    private fun updateObstacleViews() {
        val currentObstaclePositions = gameManager.getAllObstaclePositions()

        // Identify views to remove (views that exist but are not in the current game matrix state)
        val viewsToRemove = obstacleViews.keys.filter { it !in currentObstaclePositions }
        for (position in viewsToRemove) {
            removeObstacleView(position) // Use the helper function to remove view and from map
        }

        // Identify obstacles in the matrix that don't have views yet
        val obstaclesToAddViewFor = currentObstaclePositions.filter { it !in obstacleViews.keys }
        for (position in obstaclesToAddViewFor) {
            addObstacleView(position) // Use the helper function to create and add view/map entry
        }

        // Ensure all existing obstacle views are in the correct position
        for ((position, view) in obstacleViews) {
            positionObstacle(view, position.first, position.second)
        }
    }

    // Helper function to add an obstacle view
    private fun addObstacleView(position: Pair<Int, Int>) {
        val obstacleView = ImageView(this).apply {
            id = View.generateViewId()
            setImageResource(R.drawable.ic_tom) // Assuming this is the drawable for Tom
        }
        gameRelativeLayout.addView(obstacleView)
        positionObstacle(obstacleView, position.first, position.second)
        obstacleViews[position] = obstacleView
    }

    // Helper function to remove an obstacle view
    private fun removeObstacleView(position: Pair<Int, Int>) {
        val obstacleView = obstacleViews[position] ?: return
        gameRelativeLayout.removeView(obstacleView)
        obstacleViews.remove(position)
    }


    // Helper function to check and handle collision
    private fun checkAndHandleCollision(): Pair<Int, Int>? {
        val collisionPosition = if (gameManager.checkCollision()) {
            gameManager.handleCollision()
        } else null

        return collisionPosition
    }


    private fun stopGame() {
        isGameRunning = false
        gameJob?.cancel() // Cancel the coroutine to avoid leaks
        gameJob = null
    }

    override fun onPause() {
        super.onPause()
        // Pause the game when activity goes to background
        stopGame()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure game is stopped when activity is destroyed
        stopGame()
    }

    private fun vibrate(): Boolean {
        try {
            // Get vibrator service
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            // Check if vibrator service is available
            if (vibrator == null) {
                Log.e("Vibration", "Vibrator service not available")
                return false
            }

            // Check if device has vibrator
            if (!vibrator.hasVibrator()) {
                Log.e("Vibration", "Device doesn't have vibrator capability")
                return false
            }

            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)

            return true
        } catch (e: Exception) {
            Log.e("Vibration", "Error triggering vibration", e)
            return false
        }
    }


}
