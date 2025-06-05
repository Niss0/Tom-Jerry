package com.shahar.tomjerry.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import com.shahar.tomjerry.viewmodel.GameManager
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.shahar.tomjerry.R
import com.shahar.tomjerry.utilities.SingleSoundPlayer
import com.shahar.tomjerry.interfaces.TiltCallback
import com.shahar.tomjerry.utilities.TiltDetector

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.shahar.tomjerry.utilities.GameConstants

class GameActivity : AppCompatActivity(), TiltCallback {
    private lateinit var gameManager: GameManager
    private lateinit var jerryImageView: ImageView
    private lateinit var buttonLeft: MaterialButton
    private lateinit var buttonRight: MaterialButton
    private lateinit var menuButton: MaterialButton

    private lateinit var gameRelativeLayout: RelativeLayout
    // obstacleViews maps position to view
    private val obstacleViews = mutableMapOf<Pair<Int, Int>, ImageView>()
    private lateinit var heartViews: List<ImageView>
    private lateinit var heartsContainer: RelativeLayout
    private lateinit var buttonsContainer: ConstraintLayout
    private lateinit var soundPlayer: SingleSoundPlayer
    private lateinit var tiltDetector: TiltDetector
    private lateinit var odometerTextView: TextView
    private var currentGameMode: String? = null
    private var currentGameDelayMs: Long = GameConstants.GAME_SPEED_NORMAL_MS // Default speed



    private val coinViews = mutableMapOf<Pair<Int, Int>, ImageView>()
    private var screenWidth = 0
    private var colWidth = 0
    private var playableAreaTop = 0
    private var playableAreaBottom = 0
    private var playableAreaHeight = 0
    private var isGameRunning = false
    private var gameJob: Job? = null

    // Define initial desired size for Jerry and Tom (in pixels)
    private var characterSize = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // --- Retrieve the game mode from the Intent ---
        currentGameMode = intent.getStringExtra(GameConstants.EXTRA_GAME_MODE)

        gameManager = GameManager()
        soundPlayer = SingleSoundPlayer(this)
        tiltDetector = TiltDetector(this, this)
        // Initialize UI elements
        jerryImageView = findViewById(R.id.jerry_image_view)
        buttonLeft = findViewById(R.id.btn_left_arrow_game)
        buttonRight = findViewById(R.id.btn_right_arrow_game)
        menuButton = findViewById(R.id.game_button_in_game_menu)

        gameRelativeLayout = findViewById(R.id.game_relative_layout)
        heartsContainer = findViewById(R.id.hearts_container)
        buttonsContainer = findViewById(R.id.buttons_container)
        odometerTextView = findViewById(R.id.odometer_text_view)
        updateOdometerDisplay()

        // --- Determine game speed based on mode ---
        when (currentGameMode) {
            GameConstants.MODE_BUTTON_SLOW -> {
                currentGameDelayMs = GameConstants.GAME_SPEED_SLOW_MS
            }
            GameConstants.MODE_BUTTON_FAST -> {
                currentGameDelayMs = GameConstants.GAME_SPEED_FAST_MS
            }
            GameConstants.MODE_SENSOR -> {
                currentGameDelayMs = GameConstants.GAME_SPEED_NORMAL_MS // Or a specific speed for sensor mode
            }
            else -> { // Default case if mode is null or unrecognized
                currentGameDelayMs = GameConstants.GAME_SPEED_NORMAL_MS
                Log.w("GameActivity", "Unknown game mode '$currentGameMode', defaulting to normal speed.")
            }
        }
        Log.d("GameActivity", "Set game delay to: $currentGameDelayMs ms for mode: $currentGameMode")

        gameRelativeLayout.post {
            screenWidth = gameRelativeLayout.width
            colWidth = screenWidth / gameManager.cols

            // Dynamically calculate characterSize based on colWidth
            characterSize = (colWidth * 0.8).toInt() // Make it 80% of the lane width, adjust as needed
            // Ensure characterSize is not zero if colWidth is very small, or set a minimum
            if (characterSize <= 0) {
                characterSize = 50 // A fallback minimum size in pixels
            }
            if (::heartsContainer.isInitialized)
                playableAreaTop = heartsContainer.bottom // Obstacles start below the hearts

            playableAreaBottom = buttonsContainer.top
            playableAreaHeight = playableAreaBottom - playableAreaTop

            if (playableAreaHeight <= 0) {
                Log.e(
                    "GameActivity",
                    "Error: playableAreaHeight is not positive. Check heart and button container layouts. Height: $playableAreaHeight, Top: $playableAreaTop, Bottom: $playableAreaBottom"
                )
                if (gameManager.rows > 0) { // Avoid division by zero if rows is 0
                    val fallbackRowHeight = 100 // A default row height in pixels
                    playableAreaHeight = fallbackRowHeight * gameManager.rows
                } else {
                    playableAreaHeight = 300 // Absolute fallback
                }
                Log.w("GameActivity", "Corrected playableAreaHeight to: $playableAreaHeight")
            }

            initializeHearts()
            positionJerryBasedOnColumn()
            setupButtonListenersAndVisibility()
            startGame()
        }
    }


    private fun updateOdometerDisplay() {
        val distanceValue = gameManager.getDistanceTraveled()
        odometerTextView.text = getString(R.string.odometer_format, distanceValue)
    }

    /**
     * Called by TiltDetector when the device is tilted significantly to the left.
     */
    override fun onTiltedLeft() {
        runOnUiThread {
            if (isGameRunning && currentGameMode == GameConstants.MODE_SENSOR) { // Only act if in SENSOR mode
                Log.d("GameActivity_Tilt", "onTiltedLeft callback invoked in SENSOR mode.")
                if (gameManager.moveJerryLeft()) {
                    positionJerryBasedOnColumn()
                    val interactionResult = gameManager.handleInteractionsAtJerryLocation()
                    processInteractionResult(interactionResult)
                }
            }
        }
    }

    /**
     * Called by TiltDetector when the device is tilted significantly to the right.
     */
    override fun onTiltedRight() {
        runOnUiThread {
            if (isGameRunning && currentGameMode == GameConstants.MODE_SENSOR) { // Only act if in SENSOR mode
                Log.d("GameActivity_Tilt", "onTiltedRight callback invoked in SENSOR mode.")
                if (gameManager.moveJerryRight()) {
                    positionJerryBasedOnColumn()
                    val interactionResult = gameManager.handleInteractionsAtJerryLocation()
                    processInteractionResult(interactionResult)
                }
            }
        }
    }

    /**
     * Called by TiltDetector when significant Y-axis tilt is detected.
     * @param yValue The raw Y-axis sensor value.
     */
    override fun onTiltY(yValue: Float) { // Retained for bonus feature
        runOnUiThread {
//            if (isGameRunning && currentGameMode == GameConstants.MODE_SENSOR) { // Example: Bonus only in sensor mode
//                // Log.d("GameActivity", "Y-axis tilt: $yValue (Sensor Mode)")
//                // Implement bonus speed logic here later
//            }
        }
    }

    private fun setupButtonListenersAndVisibility() {
        setupInGameMenuButtonListener()
        if (currentGameMode == GameConstants.MODE_BUTTON_SLOW || currentGameMode == GameConstants.MODE_BUTTON_FAST) {
            Log.d("GameActivity", "Setting up BUTTON controls for mode: $currentGameMode")
            buttonLeft.visibility = View.VISIBLE
            buttonRight.visibility = View.VISIBLE
            buttonLeft.isEnabled = true
            buttonRight.isEnabled = true
            buttonLeft.setOnClickListener {
                if (isGameRunning) {
                    if (gameManager.moveJerryLeft()) {
                        positionJerryBasedOnColumn()
                        val interactionResult = gameManager.handleInteractionsAtJerryLocation()
                        processInteractionResult(interactionResult)
                    }
                }
            }

            buttonRight.setOnClickListener {
                if (isGameRunning) {
                    if (gameManager.moveJerryRight()) {
                        positionJerryBasedOnColumn()
                        val interactionResult = gameManager.handleInteractionsAtJerryLocation()
                        processInteractionResult(interactionResult)
                    }
                }
            }
        } else {
            Log.d("GameActivity", "HIDING/DISABLING BUTTON controls for mode: $currentGameMode")
            // Hide buttons if not in a button mode
            buttonLeft.visibility = View.GONE
            buttonRight.visibility = View.GONE
            buttonLeft.setOnClickListener(null)
            buttonRight.setOnClickListener(null)
        }
    }

    private fun positionJerryBasedOnColumn() {
        val column = gameManager.getJerryColumn()
        val params = RelativeLayout.LayoutParams(
            characterSize,
            characterSize
        )
        params.addRule(RelativeLayout.ABOVE, buttonsContainer.id)

        // Calculate left margin based on column index
        // (screenWidth / gameManager.cols) is the width of one column
        val columnWidth = screenWidth / gameManager.cols
        val margin = (column * columnWidth) + (columnWidth - characterSize) / 2
        params.leftMargin = margin

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
        var obstacleSpawnFrequency = 5 // Adjust to control how often obstacles spawn
        var coinSpawnCounter = 0 // New local counter for coins
        val coinSpawnFrequency = 5

        gameJob = lifecycleScope.launch(Dispatchers.Default) {
            // Game loop
            while (isGameRunning) {
                gameManager.clearOffScreenItemsFromBottomRow()
                gameManager.moveObstaclesAndCoinsDown()

                // Check if a collision is about to occur based on the new positions.

                // Obstacle Spawning Logic (from your original code structure)
                var interactionFromItemMovement: GameManager.InteractionResult = GameManager.InteractionResult.NONE
                if (isGameRunning) {
                    interactionFromItemMovement = gameManager.handleInteractionsAtJerryLocation()
                    obstacleSpawnCounter++
                    if (obstacleSpawnCounter >= obstacleSpawnFrequency) {
                        obstacleSpawnCounter = 0
                        gameManager.addObstacle()
                    }
                    // --- Coin Spawning Logic ---
                    coinSpawnCounter++
                    if (coinSpawnCounter >= coinSpawnFrequency) {
                        coinSpawnCounter = 0
                        gameManager.addCoin()
                    }
                }

                // --- UI UPDATE PHASE - Switch to Main Thread ---
                if(isGameRunning) {
                    withContext(Dispatchers.Main) {
                        updateObstacleViews()
                        updateCoinViews()
                        updateOdometerDisplay()


                        if (interactionFromItemMovement != GameManager.InteractionResult.NONE) {
                            Log.d("GameActivity_Loop", "Interaction from item movement: $interactionFromItemMovement")
                            processInteractionResult(interactionFromItemMovement)
                            // processInteractionResult already handles UI updates like score, lives, toast, sound, game over.
                            // It also means the matrix was updated by handleInteractionsAtJerryLocation (e.g., coin removed).
                            // So, update the views again to reflect immediate removal if something was collected/hit.
                            if (interactionFromItemMovement == GameManager.InteractionResult.COIN_COLLECTED) {
                                updateCoinViews() // Ensure collected coin visually disappears
                            } else if (interactionFromItemMovement == GameManager.InteractionResult.OBSTACLE_COLLISION) {
                                updateObstacleViews() // Ensure hit obstacle visually disappears
                            }
                        }

                        if (gameManager.isGameOver()) {
                            gameOver() // This calls stopGame(), which should cancel the gameJob
                        }
                    }
                }

                        // Game tick delay (only if game is still running)
                        if (isGameRunning) {
                            delay(200)  // Adjust for game speed
                        }
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

    private fun positionObstacle(obstacleView: ImageView, row: Int, col: Int) {
        val params = RelativeLayout.LayoutParams(
            characterSize,
            characterSize
        )

        val rowHeight = playableAreaHeight / gameManager.rows
        params.topMargin = playableAreaTop + (row * rowHeight)

        // Calculate left margin based on column index
        val columnWidth = screenWidth / gameManager.cols
        val margin = (col * columnWidth) + (columnWidth - characterSize) / 2
        params.leftMargin = margin

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


    // --- Helper function to add a coin ImageView ---
    private fun addCoinView(position: Pair<Int, Int>) {
        val coinView = ImageView(this).apply {
            id = View.generateViewId() // Important for RelativeLayout if rules depend on it
            // Make sure you have a drawable named 'coin_icon' or similar in res/drawable
            setImageResource(R.drawable.coin_icon) // Replace 'coin_icon' with your actual coin drawable ID
            // Adjust layout params if coins should have a different size than obstacles/Jerry
        }
        gameRelativeLayout.addView(coinView) // Add to your main game layout
        positionCoin(coinView, position.first, position.second) // Position it
        coinViews[position] = coinView // Store it in the map
    }

    // --- Helper function to remove a coin ImageView ---
    private fun removeCoinView(position: Pair<Int, Int>) {
        coinViews[position]?.let { // Safely get the view from the map
            gameRelativeLayout.removeView(it) // Remove from layout
            coinViews.remove(position) // Remove from map
        }
    }

    // --- Helper function to position a coin ImageView ---
// This can be very similar to positionObstacle, or identical if they use the same size logic
    private fun positionCoin(coinView: ImageView, row: Int, col: Int) {
        // Using characterSize for coins as well, adjust if coins have a different size.
        // If coinSize is different, characterSize parameter should be passed or a new var used.
        val params = RelativeLayout.LayoutParams(
            characterSize, // Coin width (can be different if needed)
            characterSize  // Coin height (can be different if needed)
        )

        // Calculate vertical position based on row
        // Ensure playableAreaHeight and gameManager.rows are valid to avoid division by zero
        val rowHeight = if (gameManager.rows > 0) playableAreaHeight / gameManager.rows else 0
        params.topMargin = playableAreaTop + (row * rowHeight)

        // Calculate horizontal position based on column and center it
        // Ensure gameManager.cols is valid
        val columnWidth = if (gameManager.cols > 0) screenWidth / gameManager.cols else 0
        val margin = (col * columnWidth) + (columnWidth - characterSize) / 2
        params.leftMargin = margin

        coinView.layoutParams = params
    }

    // --- Main function to update all coin views ---
    private fun updateCoinViews() {
        val currentCoinPositions = gameManager.getAllCoinPositions() // Get current coin locations

        // Identify and remove coin views that are no longer in the gameManager's state
        val viewsToRemove = coinViews.keys.filter { it !in currentCoinPositions }
        for (position in viewsToRemove) {
            removeCoinView(position)
        }

        // Identify new coin positions in the gameManager's state that don't have views yet
        val coinsToAddViewFor = currentCoinPositions.filter { it !in coinViews.keys }
        for (position in coinsToAddViewFor) {
            addCoinView(position)
        }

        // Ensure all existing coin views are correctly positioned (e.g., if they moved)
        // Since coins are added at a position and removed if collected/off-screen,
        // they don't "move" in the UI in the same way obstacles might if you had more complex logic.
        // But calling positionCoin ensures they are drawn correctly based on matrix data.
        for ((position, view) in coinViews) {
            positionCoin(view, position.first, position.second)
        }
    }

    private fun setupInGameMenuButtonListener() {
        menuButton.setOnClickListener {
            if (isGameRunning) {
                stopGame() // PAUSE the game when menu is opened
                Log.d("GameActivity", "In-game menu button clicked, game PAUSED.")
            }
            showInGameModeSelectionDialog()
        }
    }

    private fun showInGameModeSelectionDialog() {
        val gameModeDisplayNames = arrayOf(
            "Buttons - Slow Speed",
            "Buttons - Fast Speed",
            "Sensor Controls"
        )
        val gameModeConstants = arrayOf(
            GameConstants.MODE_BUTTON_SLOW,
            GameConstants.MODE_BUTTON_FAST,
            GameConstants.MODE_SENSOR
        )

        AlertDialog.Builder(this)
            .setTitle("Change Game Mode")
            .setItems(gameModeDisplayNames) { dialog, which ->
                val selectedMode = gameModeConstants[which]
                Log.d("GameActivity", "New mode selected from in-game dialog: $selectedMode")

                if (selectedMode != currentGameMode) {
                    // If mode changed, re-launch GameActivity with the new mode
                    // This ensures a clean reset of game state, speed, controls.
                    val intent = Intent(this, GameActivity::class.java)
                    intent.putExtra(GameConstants.EXTRA_GAME_MODE, selectedMode)
                    // Flags to clear the previous GameActivity and start a new one
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish() // Close the current GameActivity
                } else {
                    // Mode didn't change, just resume if it was paused
                    if (!isGameRunning && gameManager.getRemainingHearts() > 0 && !gameManager.isGameOver()) {
                        Log.d("GameActivity", "Mode selection same as current, resuming game.")
                        startGame()
                    }
                }
                dialog.dismiss()
            }
            .setOnCancelListener {
                // If dialog is cancelled (e.g., back button), resume game if it was running and not game over
                if (!isGameRunning && gameManager.getRemainingHearts() > 0 && !gameManager.isGameOver()) {
                    Log.d("GameActivity", "In-game mode selection CANCELLED, resuming game.")
                    startGame()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // Same as OnCancelListener essentially
                if (!isGameRunning && gameManager.getRemainingHearts() > 0 && !gameManager.isGameOver()) {
                    Log.d("GameActivity", "In-game mode selection CANCELLED (button), resuming game.")
                    startGame()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun stopGame() {
        isGameRunning = false
        gameJob?.cancel() // Cancel the coroutine to avoid leaks
        gameJob = null
    }

    override fun onPause() {
        super.onPause()
        // Always stop the tilt detector if it might have been started,
        // TiltDetector.stop() handles if it wasn't running.
        tiltDetector.stop()
        Log.d("GameActivity", "onPause: TiltDetector stopped.")
        // stopGame() is also called here from your existing code
    }

    override fun onResume() {
        super.onResume()
        if (currentGameMode == GameConstants.MODE_SENSOR) {
            tiltDetector.start()
            Log.d("GameActivity", "onResume: TiltDetector STARTED for SENSOR mode.")
        } else {
            // Ensure tilt detector is stopped if not in sensor mode,
            // especially if mode could change while activity is paused.
            tiltDetector.stop()
            Log.d(
                "GameActivity",
                "onResume: TiltDetector STOPPED/NOT STARTED for mode: $currentGameMode."
            )
        }
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
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
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

    private fun processInteractionResult(result: GameManager.InteractionResult) {
        when (result) {
            GameManager.InteractionResult.COIN_COLLECTED -> {
               soundPlayer.playSound(R.raw.coin_collection_sound)
            }
            GameManager.InteractionResult.OBSTACLE_COLLISION -> {
                updateHeartsDisplay()
                makeText(
                    this@GameActivity,
                    getString(R.string.toast_message_when_hit),
                    LENGTH_SHORT
                ).show()
                vibrate()
                soundPlayer.playSound(R.raw.crash_sound)

                Log.d("GameActivity", "UI updated for OBSTACLE_COLLISION. Lives: ${gameManager.getRemainingHearts()}")
                if (gameManager.isGameOver()) {
                    gameOver()
                }
            }
            GameManager.InteractionResult.NONE -> {
                Log.d("GameActivity", "InteractionResult was NONE.")
            }
        }
    }

}
